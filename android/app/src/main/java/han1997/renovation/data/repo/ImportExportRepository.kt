package han1997.renovation.data.repo

import androidx.room.withTransaction
import han1997.renovation.data.db.*
import han1997.renovation.data.knowledge.DecoboxStateJson
import han1997.renovation.data.knowledge.KnowledgeCache
import han1997.renovation.domain.quote.QuotePlanState
import han1997.renovation.domain.quote.QuoteValidation
import han1997.renovation.domain.planner.PlannerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.*

/** v2 是 Android 完整快照；实体金额为整数分，不再经浮点往返。 */
@Serializable
internal data class BackupV2(
    @SerialName("schema_version") val schemaVersion: Int = 2,
    val appId: String = "han1997.renovation",
    val profile: HouseProfileEntity? = null,
    val taskTemplates: List<TaskTemplateEntity> = emptyList(),
    val taskCompletions: List<TaskCompletionEntity> = emptyList(),
    val customTasks: List<TaskEntity> = emptyList(),
    val stageOverrides: List<StageOverrideEntity> = emptyList(),
    val budgetCategories: List<BudgetCategoryEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val checks: List<ChecklistItemCheckEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val contacts: List<ContactEntity> = emptyList(),
    val quickNotes: List<QuickNoteEntity> = emptyList(),
    val quotePlans: List<QuotePlanEntity> = emptyList(),
    val plannerState: PlannerStateEntity? = null,
)

/** 预览持有已校验快照，确认时不重新读取文件。 */
class PreparedImport internal constructor(internal val snapshot: BackupV2, val summary: String, internal val legacyRoot: JsonObject? = null)

class ImportExportRepository(private val db: AppDatabase, private val knowledge: KnowledgeCache? = null) {
    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }

    private suspend fun snapshot() = BackupV2(
        profile = db.houseProfileDao().get(),
        taskTemplates = db.taskTemplateDao().listAll(),
        taskCompletions = db.taskDao().exportCompletions(),
        customTasks = db.taskDao().exportCustomTasks(),
        stageOverrides = db.taskDao().exportOverrides(),
        budgetCategories = db.budgetCategoryDao().listAll(),
        expenses = db.expenseDao().exportAll(),
        checks = db.checklistDao().exportChecks(),
        notes = db.noteDao().exportAll(),
        contacts = db.contactDao().exportAll(),
        quickNotes = db.quickNoteDao().exportAll(),
        quotePlans = db.quotePlanDao().listAll(),
        plannerState = db.plannerStateDao().get(),
    )

    suspend fun exportJson(): String = db.withTransaction {
        json.encodeToString(BackupV2.serializer(), snapshot())
    }

    suspend fun prepareImport(text: String): PreparedImport = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        val root = json.parseToJsonElement(text) as? JsonObject ?: error("请选择 Android JSON 备份")
        val version = root["schema_version"]?.jsonPrimitive?.intOrNull
            ?: error("缺少备份版本，未修改现有数据")
        val value = when (version) {
            2 -> {
                val required = setOf("appId", "profile", "taskTemplates", "taskCompletions", "customTasks",
                    "stageOverrides", "budgetCategories", "expenses", "checks", "notes", "contacts",
                    "quickNotes", "quotePlans", "plannerState")
                require(root.keys.containsAll(required)) { "备份不完整，未修改现有数据" }
                json.decodeFromJsonElement(BackupV2.serializer(), root).also {
                    require(it.appId == "han1997.renovation") { "不是装修管家 Android 备份" }
                }
            }
            1 -> legacy(root)
            else -> error("暂不支持备份版本 $version，请使用相应版本的 App")
        }
        validate(value)
        val warning = if (version == 1) "\n旧备份不含的报价、需求规划和模板修改将保留；旧版未记录的完成时间无法恢复。" else ""
        PreparedImport(value,
            "将恢复 ${value.taskTemplates.size + value.customTasks.size} 项任务、${value.expenses.size} 笔支出、${value.quotePlans.size} 个报价方案。\n确认后覆盖备份包含的数据，请先备份当前内容。$warning", if (version == 1) root else null)
    }

    private suspend fun legacy(root: JsonObject): BackupV2 = db.withTransaction {
        require(root.keys.containsAll(setOf("profile", "tasksDone", "budgetCategories", "expenses"))) { "旧备份缺少必要字段" }
        val old = json.decodeFromJsonElement(ExportPayload.serializer(), root)
        val current = snapshot()
        val customIds = old.customTasks.map { it.id }.toSet()
        val completionIds = (old.tasksDone.keys + old.taskDates.keys) - customIds
        val defaults = knowledge?.knowledge?.stages.orEmpty().flatMap { st ->
            st.tasks.mapIndexed { i, t -> TaskTemplateEntity(t.id, st.id, i, t.text, t.tip) }
        }
        val templates = current.taskTemplates + defaults.filter { t ->
            t.id in completionIds && current.taskTemplates.none { it.id == t.id }
        }
        current.copy(
            profile = old.profile?.toEntity(),
            taskTemplates = templates,
            taskCompletions = completionIds.map { TaskCompletionEntity(it, old.taskDates[it], old.tasksDone[it] ?: false, null) },
            customTasks = old.customTasks.map { it.toEntity() },
            stageOverrides = old.stageOverride.map { StageOverrideEntity(it.key, it.value.uppercase(), "1970-01-01") },
            budgetCategories = old.budgetCategories.map { it.toEntity() },
            expenses = old.expenses.map { it.toEntity() },
            checks = old.checks.map { ChecklistItemCheckEntity(it.key, it.value, null) },
            notes = if ("notes" in root) old.notes.map { it.toEntity() } else current.notes,
            contacts = if ("contacts" in root) old.contacts.map { it.toEntity() } else current.contacts,
            quickNotes = if ("quickNotes" in root) old.quickNotes.map { it.toEntity() } else current.quickNotes,
        )
    }

    private suspend fun validate(p: BackupV2) {
        fun unique(ids: List<String>) { require(ids.all { it.isNotBlank() } && ids.size == ids.toSet().size) { "备份有空 ID 或重复记录" } }
        fun date(value: String?) { if (value != null) { require(value.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}"))) { "日期格式不正确" }; kotlinx.datetime.LocalDate.parse(value) } }
        unique(p.taskTemplates.map { it.id }); unique(p.taskCompletions.map { it.templateId })
        unique(p.customTasks.map { it.id }); unique(p.stageOverrides.map { it.stageId })
        unique(p.budgetCategories.map { it.id }); unique(p.expenses.map { it.id })
        unique(p.notes.map { it.id }); unique(p.contacts.map { it.id }); unique(p.quickNotes.map { it.id })
        unique(p.quotePlans.map { it.id }); unique(p.checks.map { it.itemId })
        val stages = db.stageDao().observeAll().first().map { it.id }.toSet()
        val templates = p.taskTemplates.map { it.id }.toSet()
        require(p.customTasks.none { it.id in templates }) { "模板与自定义任务 ID 冲突" }
        if (stages.isNotEmpty()) {
            require((p.taskTemplates.map { it.stageId } + p.customTasks.map { it.stageId } + p.stageOverrides.map { it.stageId }).all { it in stages }) { "任务引用了不存在的阶段" }
            require(p.taskCompletions.all { it.templateId in templates }) { "完成记录引用了不存在的任务" }
        }
        p.profile?.let { require(it.id == HouseProfileEntity.SINGLE_ROW_ID && it.areaM2.isFinite() && it.areaM2 > 0 && it.totalBudgetCents >= 0) { "房屋面积或预算无效" }; date(it.startDate) }
        require(p.budgetCategories.all { it.name.isNotBlank() && it.plannedCents >= 0 }) { "分类名称或金额无效" }
        val cats = p.budgetCategories.map { it.id }.toSet()
        require(p.expenses.all { it.categoryId in cats && it.name.isNotBlank() && it.amountCents > 0 }) { "支出金额或分类引用无效" }
        p.expenses.forEach { date(it.date) }
        p.taskCompletions.forEach { date(it.dueDate); date(it.doneAt) }
        p.customTasks.forEach { require(it.text.isNotBlank()) { "任务名称不能为空" }; date(it.dueDate); date(it.doneAt) }
        require(p.stageOverrides.all { it.overrideState == StageOverrideEntity.STATE_DONE }) { "阶段状态无效" }
        p.taskTemplates.forEach { require(it.text.isNotBlank()) { "模板任务名称不能为空" } }
        p.quickNotes.forEach {
            require(it.type in setOf(QuickNoteEntity.TYPE_WISH, QuickNoteEntity.TYPE_MEMO)) { "随手记类型无效" }
            if (it.type == QuickNoteEntity.TYPE_WISH) require(it.category in setOf(QuickNoteEntity.CATEGORY_FURNITURE, QuickNoteEntity.CATEGORY_APPLIANCE, QuickNoteEntity.CATEGORY_DAILY) && it.stageId == null) { "购物愿望品类无效" }
            else require(it.category == null && (it.stageId == null || stages.isEmpty() || it.stageId in stages)) { "阶段备忘关联无效" }
            date(it.createdAt); date(it.updatedAt)
        }
        val checkIds = db.checklistDao().listAll().flatMap { cl -> db.checklistDao().observeItems(cl.id).first().map { it.id } }.toSet()
        if (checkIds.isNotEmpty()) require(p.checks.all { it.itemId in checkIds }) { "验收条目引用无效" }
        p.checks.forEach { date(it.doneAt) }
        p.budgetCategories.fold(0L) { sum, item -> Math.addExact(sum, item.plannedCents) }
        p.expenses.fold(0L) { sum, item -> Math.addExact(sum, item.amountCents) }
        p.quotePlans.forEach {
            require(it.name.isNotBlank()) { "报价方案名称不能为空" }
            val state = DecoboxStateJson.json.decodeFromString(QuotePlanState.serializer(), it.stateJson)
            require(state.mode.name.lowercase() == it.mode) { "报价模式与内容不一致" }
            QuoteValidation.validate(state, knowledge?.decoboxCatalog)
        }
        p.plannerState?.let {
            require(it.id == PlannerStateEntity.SINGLE_ROW_ID) { "规划记录 ID 无效" }
            val state = DecoboxStateJson.json.decodeFromString(PlannerState.serializer(), it.stateJson)
            knowledge?.decoboxRequirements?.let { catalog -> han1997.renovation.domain.planner.PlannerRules.validateCatalogReferences(state, catalog) }
            unique(state.rooms.map { r -> r.id }); unique(state.picked.map { pick -> pick.itemKey })
            require(state.step in 1..4 && state.rooms.all { r -> r.name.isNotBlank() && r.area.isFinite() && r.area >= 0 }) { "规划空间或步骤无效" }
            require(state.assignments.map { a -> a.itemKey to a.roomId }.distinct().size == state.assignments.size) { "分配记录重复" }
            require(state.assignments.all { a -> state.rooms.any { r -> r.id == a.roomId } && state.picked.any { pick -> pick.itemKey == a.itemKey } }) { "需求分配引用无效" }
        }
    }

    suspend fun restore(prepared: PreparedImport): ImportResult = try {
        db.withTransaction {
            val p = prepared.legacyRoot?.let { legacy(it) } ?: prepared.snapshot
            validate(p)
            db.taskDao().clearCompletions(); db.taskDao().clearTasks(); db.taskDao().clearOverrides()
            db.taskTemplateDao().clear(); db.expenseDao().clear(); db.budgetCategoryDao().clear()
            db.checklistDao().clearChecks(); db.noteDao().clearAll(); db.contactDao().clearAll()
            db.quickNoteDao().clearAll(); db.quotePlanDao().clearAll(); db.plannerStateDao().clearAll()
            db.houseProfileDao().deleteAll()
            p.profile?.let { db.houseProfileDao().upsert(it) }
            db.taskTemplateDao().seedAll(p.taskTemplates)
            p.taskCompletions.forEach { db.taskDao().upsertCompletion(it) }
            p.customTasks.forEach { db.taskDao().upsert(it) }
            p.stageOverrides.forEach { db.taskDao().insertOverride(it) }
            db.budgetCategoryDao().upsertAll(p.budgetCategories)
            p.expenses.forEach { db.expenseDao().upsert(it) }
            p.checks.forEach { db.checklistDao().upsertCheck(it) }
            p.notes.forEach { db.noteDao().upsert(it) }; p.contacts.forEach { db.contactDao().upsert(it) }
            p.quickNotes.forEach { db.quickNoteDao().upsert(it) }; p.quotePlans.forEach { db.quotePlanDao().upsert(it) }
            p.plannerState?.let { db.plannerStateDao().upsert(it) }
        }
        ImportResult(true)
    } catch (e: CancellationException) { throw e }
    catch (e: Exception) { ImportResult(false, e.message?.take(120) ?: "恢复失败，原数据未改变") }

    suspend fun importJson(text: String): ImportResult = try { restore(prepareImport(text)) }
    catch (e: CancellationException) { throw e }
    catch (e: Exception) { ImportResult(false, e.message?.take(120) ?: "备份格式不正确") }
}

data class ImportResult(val success: Boolean, val error: String? = null)

@Serializable
private data class ExportPayload(
    @kotlinx.serialization.SerialName("schema_version") val schemaVersion: Int = 1,
    val ver: Int = 1,
    val profile: ProfileExport? = null,
    val tasksDone: Map<String, Boolean> = emptyMap(),
    val taskDates: Map<String, String> = emptyMap(),
    val customTasks: List<TaskExport> = emptyList(),
    val stageOverride: Map<String, String> = emptyMap(),
    val budgetCategories: List<BudgetCategoryExport> = emptyList(),
    val expenses: List<ExpenseExport> = emptyList(),
    val checks: Map<String, Boolean> = emptyMap(),
    val notes: List<NoteExport> = emptyList(),
    val contacts: List<ContactExport> = emptyList(),
    val quickNotes: List<QuickNoteExport> = emptyList(),
)

@Serializable
private data class ProfileExport(
    val area: Double,
    val tier: String,
    val mode: String,
    val grade: String,
    val startDate: String? = null,
    val totalBudget: Double = 0.0,
    val styleId: String? = null,
    val createdAt: String,
) {
    fun toEntity() = HouseProfileEntity(
        id = HouseProfileEntity.SINGLE_ROW_ID,
        areaM2 = area,
        tierId = tier,
        modeId = mode,
        gradeId = grade,
        startDate = startDate,
        totalBudgetCents = han1997.renovation.util.MoneyUtil.fromYuan(totalBudget),
        styleId = styleId,
        styleQuizAt = null,
        createdAt = createdAt,
    )
}

@Serializable
private data class TaskExport(
    val id: String,
    val stageId: String,
    val text: String,
    val tip: String? = null,
    val spaceId: String? = null,
    val date: String? = null,
    val sourceType: String = TaskEntity.TYPE_CUSTOM,
    val createdAt: String,
) {
    fun toEntity() = TaskEntity(
        id = id,
        stageId = stageId,
        sourceType = sourceType,
        text = text,
        tip = tip,
        spaceId = spaceId,
        dueDate = date,
        done = false,
        doneAt = null,
        createdAt = createdAt,
    )
}

@Serializable
private data class BudgetCategoryExport(
    val id: String,
    val name: String,
    val emoji: String,
    val planned: Double,
    val orderIndex: Int,
    val isFromTemplate: Boolean = false,
    val createdAt: String,
) {
    fun toEntity() = han1997.renovation.data.db.BudgetCategoryEntity(
        id = id,
        name = name,
        emoji = emoji,
        plannedCents = han1997.renovation.util.MoneyUtil.fromYuan(planned),
        orderIndex = orderIndex,
        isFromTemplate = isFromTemplate,
        createdAt = createdAt,
    )
}

@Serializable
private data class ExpenseExport(
    val id: String,
    val name: String,
    val amount: Double,
    val catId: String,
    val date: String,
    val note: String? = null,
    val createdAt: String,
) {
    fun toEntity() = ExpenseEntity(
        id = id,
        name = name,
        amountCents = han1997.renovation.util.MoneyUtil.fromYuan(amount),
        categoryId = catId,
        date = date,
        note = note,
        createdAt = createdAt,
    )
}

@Serializable
private data class NoteExport(val id: String, val title: String, val text: String, val updatedAt: String) {
    fun toEntity() = NoteEntity(id, title, text, updatedAt, updatedAt)
}

@Serializable
private data class ContactExport(val id: String, val name: String, val role: String? = null, val phone: String? = null, val note: String? = null) {
    fun toEntity() = ContactEntity(id, name, role, phone, note, "1970-01-01")
}

/** 随手记导出/导入模型；旧备份缺 `quickNotes` 字段时取默认空列表，导入跳过。 */
@Serializable
private data class QuickNoteExport(
    val id: String,
    val content: String,
    val type: String,
    val category: String? = null,
    val stageId: String? = null,
    val isDone: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
) {
    fun toEntity() = QuickNoteEntity(
        id = id,
        content = content,
        type = type,
        category = category,
        stageId = stageId,
        isDone = isDone,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
