package com.renovation.guardian.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.renovation.guardian.data.db.AppDatabase
import com.renovation.guardian.data.db.BudgetCategoryEntity
import com.renovation.guardian.data.db.ExpenseEntity
import com.renovation.guardian.data.db.HouseProfileEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * JSON 备份 / 恢复（`ImportExportRepository`）单元测试：
 * - 导出内容包含 profile / 预算分类 / 支出等核心字段；
 * - 导出 → 导入到空库的往返（round-trip）保持数据一致；
 * - 非法 JSON 导入返回 success=false 而非抛异常。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImportExportRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: ImportExportRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = ImportExportRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun exportJson_containsCoreFields() = runTest {
        db.houseProfileDao().upsert(
            HouseProfileEntity(
                id = HouseProfileEntity.SINGLE_ROW_ID,
                areaM2 = 88.0,
                tierId = "mid",
                modeId = "half",
                gradeId = "eco",
                startDate = "2026-01-01",
                totalBudgetCents = 150000,
                styleId = "style_minimal",
                styleQuizAt = null,
                createdAt = "2026-01-01",
            ),
        )
        db.budgetCategoryDao().upsert(
            BudgetCategoryEntity("bc1", "主材", "🧱", 100000, 0, false, "2026-01-01"),
        )
        db.expenseDao().upsert(
            ExpenseEntity("ex1", "瓷砖", 150000, "bc1", "2026-01-02", "客厅", "2026-01-02"),
        )

        val json = repo.exportJson()

        assertTrue("导出应包含 schema_version", json.contains("\"schema_version\""))
        assertTrue("导出应包含 profile", json.contains("\"profile\""))
        assertTrue("导出应包含总面积", json.contains("88.0"))
        assertTrue("导出应包含预算分类", json.contains("\"budgetCategories\""))
        assertTrue("导出应包含支出", json.contains("\"expenses\""))
        assertTrue("导出应包含主材分类", json.contains("主材"))
    }

    @Test
    fun importJson_roundTrip_preservesData() = runTest {
        // 源库写入数据并导出
        db.houseProfileDao().upsert(
            HouseProfileEntity(
                id = HouseProfileEntity.SINGLE_ROW_ID,
                areaM2 = 92.5,
                tierId = "high",
                modeId = "full",
                gradeId = "mid",
                startDate = "2026-03-01",
                totalBudgetCents = 300000,
                styleId = "style_japandi",
                styleQuizAt = "2026-03-01",
                createdAt = "2026-03-01",
            ),
        )
        db.budgetCategoryDao().upsert(
            BudgetCategoryEntity("bc1", "主材", "🧱", 200000, 0, false, "2026-03-01"),
        )
        db.expenseDao().upsert(
            ExpenseEntity("ex1", "瓷砖", 50000, "bc1", "2026-03-02", "厨房", "2026-03-02"),
        )
        val exported = repo.exportJson()

        // 目标库（清空）导入
        val target = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        try {
            val targetRepo = ImportExportRepository(target)
            val result = targetRepo.importJson(exported)
            assertTrue(result.success)

            val profile = target.houseProfileDao().get()
            assertEquals(92.5, profile?.areaM2 ?: 0.0, 0.0)
            assertEquals(300000, profile?.totalBudgetCents ?: 0L)
            assertEquals("style_japandi", profile?.styleId)

            val cats = target.budgetCategoryDao().listAll()
            assertEquals(1, cats.size)
            assertEquals("主材", cats[0].name)
            assertEquals(200000, cats[0].plannedCents)

            val expenses = target.expenseDao().exportAll()
            assertEquals(1, expenses.size)
            assertEquals("瓷砖", expenses[0].name)
            assertEquals(50000, expenses[0].amountCents)
        } finally {
            target.close()
        }
    }

    @Test
    fun importJson_invalidInput_returnsFailure() = runTest {
        val result = repo.importJson("{ not valid json")
        assertFalse(result.success)
    }
}