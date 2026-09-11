package han1997.renovation.data.knowledge

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import han1997.renovation.data.db.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 回归防护：种子写入（seedIfEmpty）必须可用且幂等。
 *
 * 背景：`seedIfEmpty` 曾定义了但无任何调用方，导致 stage / task_template /
 * checklist 三张种子表永远为空、流程页完全空白。本测试用真实 assets
 * （Robolectric `isIncludeAndroidResources = true` 下主代码可读 assets）验证：
 * 1) 空库首次 seed 后三张种子表非空（14 阶段 / 98 模板任务 / 7 验收清单）；
 * 2) 二次 seed 幂等，行数不变。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KnowledgeSeederTest {

    private lateinit var db: AppDatabase
    private lateinit var cache: KnowledgeCache
    private lateinit var seeder: KnowledgeSeeder

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        cache = KnowledgeCache(ApplicationProvider.getApplicationContext()).also { it.load() }
        seeder = KnowledgeSeeder(db, cache)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun seedIfEmpty_fillsSeedTablesFromRealAssets() = runTest {
        // 前置：空库
        assertEquals(0, db.stageDao().count())
        assertEquals(0, db.taskTemplateDao().count())
        assertEquals(0, db.checklistDao().count())

        seeder.seedIfEmpty("2026-09-05")

        // 与 assets/knowledge.json 实际内容对齐：14 阶段 / 98 模板任务 / 7 验收清单
        assertEquals(14, db.stageDao().count())
        assertEquals(98, db.taskTemplateDao().count())
        assertEquals(7, db.checklistDao().count())
    }

    @Test
    fun seedIfEmpty_isIdempotent_onSecondCall() = runTest {
        seeder.seedIfEmpty("2026-09-05")
        val stages = db.stageDao().count()
        val templates = db.taskTemplateDao().count()
        val checklists = db.checklistDao().count()
        org.junit.Assert.assertTrue("种子表应非空", stages > 0 && templates > 0 && checklists > 0)

        // 二次调用：count>0 内置跳过，行数不变
        seeder.seedIfEmpty("2026-09-05")

        assertEquals(stages, db.stageDao().count())
        assertEquals(templates, db.taskTemplateDao().count())
        assertEquals(checklists, db.checklistDao().count())
    }
}
