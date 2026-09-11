package han1997.renovation.data.repo

import han1997.renovation.testing.PolishFixture
import han1997.renovation.data.db.*
import han1997.renovation.data.knowledge.DecoboxStateJson
import han1997.renovation.ui.quote.engine.*
import han1997.renovation.domain.quote.*
import han1997.renovation.ui.planner.engine.*
import han1997.renovation.domain.planner.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppPolishRepositoryTest {
    private fun quoteState(f: PolishFixture): QuotePlanState {
        val cat = f.cache.decoboxCatalog!!.floor.first()
        val variant = cat.variants.first()
        return QuotePlanState(house = HouseInfo(90.0, 20000000L, 2.4), rooms = listOf(QuoteRoomState("room", "客厅", 20.0,
            floor = SurfaceSelection(cat.id, variant.id, variant.specs.first().id, sourcing = Sourcing.SELF))))
    }
    private suspend fun saveQuote(f: PolishFixture, name: String = "方案") = f.container.quotePlanRepo.savePlan(name, "full", DecoboxStateJson.json.encodeToString(QuotePlanState.serializer(), quoteState(f)), f.today)

    @Test fun completeBackupPreservesMutableTemplatesStatusesQuotesAndPlanner() = runTest {
        PolishFixture().use { f ->
            f.initialize()
            val repo = f.container
            val templates = f.db.taskTemplateDao().listAll()
            val first = templates.first()
            repo.taskRepo.saveTask(first.id, "已修改任务", "保留备注", f.today, true)
            repo.taskRepo.setTemplateDone(first.id, true, f.today)
            repo.taskRepo.deleteTask(templates[1].id, true)
            repo.taskRepo.addTaskToStage(first.stageId, "用户新增任务")
            f.db.taskDao().upsert(TaskEntity("legacy-task", first.stageId, TaskEntity.TYPE_CUSTOM, "旧自定义", "备注", null, f.today, true, f.today, f.today))
            repo.budgetRepo.addExpenseCents("瓷砖", 12345L, "b-main", f.today, "含运费", f.today)
            repo.quickNoteRepo.upsert("买冰箱", "wish", "appliance", null, null, f.today)
            saveQuote(f)
            val pick = PlannerRules.search(f.cache.decoboxRequirements!!, "").first()
            val room = PlannerRoom("r", pick.spaceKey, "规划空间")
            val state = PlannerState(4, listOf(pick), listOf(room), listOf(Assignment(pick.itemKey, pick.itemName, room.id, room.name, Importance.MUST)))
            repo.quotePlanRepo.savePlannerState(DecoboxStateJson.json.encodeToString(PlannerState.serializer(), state), f.today)
            val exported = repo.importExportRepo.exportJson()
            assertEquals(2, Json.parseToJsonElement(exported).jsonObject["schema_version"]!!.jsonPrimitive.int)
            repo.clearAllData()
            assertEquals(98, f.db.taskTemplateDao().count())
            assertTrue(f.db.quotePlanDao().listAll().isEmpty())
            val imported = repo.importExportRepo.importJson(exported)
            assertTrue(imported.error, imported.success)
            assertEquals(exported, repo.importExportRepo.exportJson())
            assertTrue(f.db.taskDao().getById("legacy-task")!!.done)
            assertEquals(12345L, f.db.expenseDao().exportAll().single().amountCents)
            assertEquals("已修改任务", f.db.taskTemplateDao().getById(first.id)!!.text)
            assertNull(f.db.taskTemplateDao().getById(templates[1].id))
        }
    }
    @Test fun invalidAndFutureBackupsLeaveDatabaseUntouched() = runTest {
        PolishFixture().use { f ->
            f.initialize(); saveQuote(f)
            val before = f.container.importExportRepo.exportJson()
            listOf("{}", "not json", before.replace("\"schema_version\": 2", "\"schema_version\": 99")).forEach {
                assertFalse(f.container.importExportRepo.importJson(it).success)
                assertEquals(before, f.container.importExportRepo.exportJson())
            }
        }
    }
    @Test fun insertFailureRollsBackTheEntireRestore() = runTest {
        PolishFixture().use { f ->
            f.initialize()
            f.container.budgetRepo.addExpenseCents("已记账", 29L, "b-main", f.today, null, f.today)
            val before = f.container.importExportRepo.exportJson()
            f.db.openHelper.writableDatabase.execSQL("CREATE TRIGGER refuse_expense BEFORE INSERT ON expense BEGIN SELECT RAISE(ABORT, 'test failure'); END")
            assertFalse(f.container.importExportRepo.importJson(before).success)
            assertEquals(before, f.container.importExportRepo.exportJson())
        }
    }
    @Test fun legacyImportPreservesNewModulesIncludingChangesAfterPreview() = runTest {
        PolishFixture().use { f ->
            f.initialize(); saveQuote(f, "旧方案")
            val prepared = f.container.importExportRepo.prepareImport("""{"schema_version":1,"profile":null,"tasksDone":{},"budgetCategories":[],"expenses":[]}""")
            saveQuote(f, "预览后新增方案")
            assertTrue(f.container.importExportRepo.restore(prepared).success)
            assertEquals(2, f.db.quotePlanDao().listAll().size)
            assertEquals(98, f.db.taskTemplateDao().count())
        }
    }
    @Test fun applyingQuoteIsIdempotentAndPreservesCapExpensesAndUnselectedCategories() = runTest {
        PolishFixture().use { f ->
            f.initialize()
            f.container.budgetRepo.addExpenseCents("已有支出", 50L, "b-main", f.today, null, f.today)
            val cap = f.db.houseProfileDao().get()!!.totalBudgetCents
            val result = QuoteCalculator.calculate(f.cache.decoboxCatalog!!, quoteState(f))
            val cats = f.container.budgetRepo.listCategories()
            val buckets = QuoteBudgetMapping.buckets(result, cats)
            val preview = QuoteBudgetMapping.preview(result, cats, buckets.associate { it.key to "b-main" })
            f.container.quotePlanRepo.applyBudgetPreview(preview)
            f.container.quotePlanRepo.applyBudgetPreview(preview)
            assertEquals(result.estimatedTotalCents, f.db.budgetCategoryDao().getById("b-main")!!.plannedCents)
            assertEquals(cap, f.db.houseProfileDao().get()!!.totalBudgetCents)
            assertEquals(50L, f.db.expenseDao().exportAll().single().amountCents)
            cats.filter { it.id != "b-main" }.forEach { assertEquals(it, f.db.budgetCategoryDao().getById(it.id)) }
            f.db.budgetCategoryDao().upsert(f.db.budgetCategoryDao().getById("b-main")!!.copy(plannedCents = 123L))
            try { f.container.quotePlanRepo.applyBudgetPreview(preview); fail("应拒绝过期预览") } catch (_: IllegalStateException) { }
            assertEquals(123L, f.db.budgetCategoryDao().getById("b-main")!!.plannedCents)
        }
    }
    @Test fun restoreDefaultsPreservesCustomTasksAndDoesNotReseedIntentionalEmptyState() = runTest {
        PolishFixture().use { f ->
            f.initialize()
            val stage = f.cache.knowledge!!.stages.first()
            f.container.taskRepo.addTaskToStage(stage.id, "自己加的")
            val custom = f.db.taskTemplateDao().listByStage(stage.id).first { it.text == "自己加的" }
            f.container.taskRepo.restoreDefaultTasks(stage.id, stage.tasks.mapIndexed { i, t -> TaskTemplateEntity(t.id, stage.id, i, t.text, t.tip) })
            assertNotNull(f.db.taskTemplateDao().getById(custom.id))
            f.db.taskTemplateDao().clear()
            f.container.seeder.seedIfEmpty(f.today)
            assertEquals(0, f.db.taskTemplateDao().count())
        }
    }
    @Test fun expenseAggregateOverflowIsRejectedBeforeWriting() = runTest {
        PolishFixture().use { f ->
            f.initialize()
            f.container.budgetRepo.addExpenseCents("极限金额", Long.MAX_VALUE, "b-main", f.today, null, f.today)
            try {
                f.container.budgetRepo.addExpenseCents("不能再加一分", 1L, "b-main", f.today, null, f.today)
                fail("金额合计溢出必须被拒绝")
            } catch (_: ArithmeticException) { }
            assertEquals(1, f.db.expenseDao().exportAll().size)
            assertEquals(Long.MAX_VALUE, f.db.expenseDao().totalExcept(""))
        }
    }

}
