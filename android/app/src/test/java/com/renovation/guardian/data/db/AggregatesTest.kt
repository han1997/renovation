package com.renovation.guardian.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.renovation.guardian.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 覆盖 room-schema.md §4 的 4 个关键聚合：
 * 1) 首页三段分组（observeBetween）
 * 2) 阶段完成度（observeAllProgress）
 * 3) 分类超支（observeWithSpent）
 * 4) CSV 扁平视图（observeForCsv）
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AggregatesTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun homeThreeSegmentGrouping_groupsByDueDate() = runTest {
        db.stageDao().seedAll(
            listOf(
                StageEntity("s1", "p", "🏠", "阶段一", "1天", "目标", 0, "[]", "[]", "[]"),
            ),
        )
        db.taskTemplateDao().seedAll(
            listOf(TaskTemplateEntity("t-over", "s1", 0, "逾期任务", null)),
        )
        db.taskDao().upsertCompletion(TaskCompletionEntity("t-over", "2020-01-01", false, null))

        val overdue = db.taskDao().observeBetween("1970-01-01", "2020-01-01").first()
        assertEquals(1, overdue.size)
        assertEquals("t-over", overdue.first().id)
        assertEquals("TEMPLATE", overdue.first().source)
    }

    @Test
    fun stageCompletionPct_countsTemplatesAndDone() = runTest {
        db.stageDao().seedAll(
            listOf(StageEntity("s1", "p", "🏠", "阶段一", "1天", "目标", 0, "[]", "[]", "[]")),
        )
        db.taskTemplateDao().seedAll(
            listOf(
                TaskTemplateEntity("t1", "s1", 0, "任务1", null),
                TaskTemplateEntity("t2", "s1", 1, "任务2", null),
            ),
        )
        db.taskDao().upsertCompletion(TaskCompletionEntity("t1", null, true, null))

        val rows = db.taskDao().observeAllProgress().first()
        val row = rows.first { it.stageId == "s1" }
        assertEquals(2, row.total)
        assertEquals(1, row.done)
        assertEquals(50, row.pct)
        assertEquals(false, row.isDone)
    }

    @Test
    fun categoryOverspend_computesOverCents() = runTest {
        db.budgetCategoryDao().upsert(
            BudgetCategoryEntity("c1", "主材", "🧱", 1000, 0, false, "2026-01-01"),
        )
        db.expenseDao().upsert(
            ExpenseEntity("e1", "瓷砖", 1500, "c1", "2026-01-01", null, "2026-01-01"),
        )

        val cat = db.budgetCategoryDao().observeWithSpent().first().first { it.id == "c1" }
        assertEquals(1000, cat.plannedCents)
        assertEquals(1500, cat.spentCents)
        assertEquals(500, cat.overCents)
        assertEquals(150, cat.pct)
    }

    @Test
    fun csvFlatView_joinsCategoryName() = runTest {
        db.budgetCategoryDao().upsert(
            BudgetCategoryEntity("c1", "主材", "🧱", 1000, 0, false, "2026-01-01"),
        )
        db.expenseDao().upsert(
            ExpenseEntity("e1", "瓷砖", 1500, "c1", "2026-02-02", "客厅", "2026-02-02"),
        )

        val rows = db.expenseDao().observeForCsv().first()
        assertEquals(1, rows.size)
        val r = rows.first()
        assertEquals("2026-02-02", r.date)
        assertEquals("主材", r.categoryName)
        assertEquals("瓷砖", r.name)
        assertEquals(1500, r.amountCents)
        assertEquals("客厅", r.note)
    }
}
