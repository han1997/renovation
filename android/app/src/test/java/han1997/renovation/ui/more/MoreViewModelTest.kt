package han1997.renovation.ui.more

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import han1997.renovation.RenovationApp
import han1997.renovation.data.db.AppDatabase
import han1997.renovation.data.db.QuickNoteEntity
import han1997.renovation.data.knowledge.KnowledgeCache
import han1997.renovation.data.repo.AppContainer
import han1997.renovation.data.repo.DefaultAppContainer
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * MoreViewModel 随手记操作单元测试（Robolectric + 真实 in-memory Room）：
 * - upsertQuickNote 新增：id 生成 `qn_` 前缀，createdAt / updatedAt 均注入 todayProvider 日期；
 * - upsertQuickNote 编辑：保留原 id / createdAt，内容更新；
 * - setQuickNoteDone：isDone 翻转；
 * - deleteQuickNote：记录删除。
 *
 * 说明：
 * - MockK 的 suspend 桩在 `viewModelScope.launch` 内会丢失调用记录，故走真实 Room 链路断言落库结果；
 * - Room 的 query / transaction executor 绑定到 testScheduler，挂起 DAO 与
 *   `viewModelScope.launch` 全部跑在虚拟时间上，`advanceUntilIdle()` 后读写无竞态；
 * - todayProvider 固定为 "2026-09-04"。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class MoreViewModelTest {

    /** 真实仓库容器 + 虚拟时间 executor + 固定 today；MockK 仅用于 Application 与 KnowledgeCache 的非挂起属性。 */
    private fun makeVm(dispatcher: kotlinx.coroutines.CoroutineDispatcher): Pair<MoreViewModel, AppDatabase> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(dispatcher.asExecutor())
            .setTransactionExecutor(dispatcher.asExecutor())
            .build()
        val real = DefaultAppContainer(db, mockk<KnowledgeCache>(relaxed = true))
        val container = object : AppContainer by real {
            override val todayProvider: () -> String = { "2026-09-04" }
        }
        val app = mockk<RenovationApp>(relaxed = true)
        every { app.container } returns container
        return MoreViewModel(app) to db
    }

    @Test
    fun upsertQuickNote_newNote_usesTodayForBothTimestamps() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val (vm, db) = makeVm(dispatcher)
            vm.upsertQuickNote("买冰箱", QuickNoteEntity.TYPE_WISH, QuickNoteEntity.CATEGORY_APPLIANCE, null, null)
            advanceUntilIdle()

            val notes = db.quickNoteDao().observeAll().first()
            assertEquals(1, notes.size)
            val note = notes[0]
            assertTrue(note.id.startsWith("qn_"))
            assertEquals("买冰箱", note.content)
            assertEquals(QuickNoteEntity.TYPE_WISH, note.type)
            assertEquals(QuickNoteEntity.CATEGORY_APPLIANCE, note.category)
            assertEquals(false, note.isDone)
            assertEquals("2026-09-04", note.createdAt)
            assertEquals("2026-09-04", note.updatedAt)
            db.close()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun upsertQuickNote_edit_keepsIdAndCreatedAt() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val (vm, db) = makeVm(dispatcher)
            vm.upsertQuickNote("买冰箱", QuickNoteEntity.TYPE_WISH, QuickNoteEntity.CATEGORY_APPLIANCE, null, null)
            advanceUntilIdle()
            val original = db.quickNoteDao().observeAll().first().single()

            vm.upsertQuickNote("买冰箱（改）", QuickNoteEntity.TYPE_WISH, QuickNoteEntity.CATEGORY_DAILY, null, original.id)
            advanceUntilIdle()

            val notes = db.quickNoteDao().observeAll().first()
            assertEquals(1, notes.size)
            val edited = notes[0]
            assertEquals(original.id, edited.id)
            assertEquals("买冰箱（改）", edited.content)
            assertEquals(QuickNoteEntity.CATEGORY_DAILY, edited.category)
            assertEquals("2026-09-04", edited.createdAt)
            db.close()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun setQuickNoteDone_togglesIsDone() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val (vm, db) = makeVm(dispatcher)
            vm.upsertQuickNote("买冰箱", QuickNoteEntity.TYPE_WISH, QuickNoteEntity.CATEGORY_APPLIANCE, null, null)
            advanceUntilIdle()
            val id = db.quickNoteDao().observeAll().first().single().id

            vm.setQuickNoteDone(id, true)
            advanceUntilIdle()

            val done = db.quickNoteDao().getById(id)
            assertEquals(true, done?.isDone)
            db.close()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun deleteQuickNote_removesRow() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val (vm, db) = makeVm(dispatcher)
            vm.upsertQuickNote("买冰箱", QuickNoteEntity.TYPE_WISH, QuickNoteEntity.CATEGORY_APPLIANCE, null, null)
            advanceUntilIdle()
            val id = db.quickNoteDao().observeAll().first().single().id

            vm.deleteQuickNote(id)
            advanceUntilIdle()

            val notes = db.quickNoteDao().observeAll().first()
            assertEquals(0, notes.size)
            db.close()
        } finally {
            Dispatchers.resetMain()
        }
    }
}
