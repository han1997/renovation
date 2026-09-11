package han1997.renovation.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
 * 随手记（quick_note）DAO 单元测试：
 * - upsert / delete / getById；
 * - observeAll 排序（未完成在前、已完成置底、组内按 updated_at 倒序）；
 * - observeByType 分组查询（wish / memo 各自过滤）；
 * - exportAll / clearAll（备份与重置路径）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuickNoteDaoTest {

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

    private fun note(
        id: String,
        type: String = QuickNoteEntity.TYPE_WISH,
        category: String? = QuickNoteEntity.CATEGORY_FURNITURE,
        stageId: String? = null,
        isDone: Boolean = false,
        updatedAt: String = "2026-09-01",
    ) = QuickNoteEntity(
        id = id,
        content = "内容-$id",
        type = type,
        category = category,
        stageId = stageId,
        isDone = isDone,
        createdAt = "2026-09-01",
        updatedAt = updatedAt,
    )

    @Test
    fun upsert_insertsAndReplaces() = runTest {
        db.quickNoteDao().upsert(note("q1"))
        db.quickNoteDao().upsert(note("q1", updatedAt = "2026-09-02"))

        val loaded = db.quickNoteDao().getById("q1")
        assertEquals("2026-09-02", loaded?.updatedAt)
        assertEquals(1, db.quickNoteDao().exportAll().size)
    }

    @Test
    fun observeAll_putsDoneItemsLast() = runTest {
        db.quickNoteDao().upsert(note("q-done", isDone = true, updatedAt = "2026-09-03"))
        db.quickNoteDao().upsert(note("q-open-old", updatedAt = "2026-09-01"))
        db.quickNoteDao().upsert(note("q-open-new", updatedAt = "2026-09-02"))

        val all = db.quickNoteDao().observeAll().first()
        assertEquals(listOf("q-open-new", "q-open-old", "q-done"), all.map { it.id })
    }

    @Test
    fun observeByType_filtersWishAndMemo() = runTest {
        db.quickNoteDao().upsert(note("w1", type = QuickNoteEntity.TYPE_WISH, category = QuickNoteEntity.CATEGORY_APPLIANCE))
        db.quickNoteDao().upsert(note("m1", type = QuickNoteEntity.TYPE_MEMO, category = null, stageId = "tiling"))
        db.quickNoteDao().upsert(note("m2", type = QuickNoteEntity.TYPE_MEMO, category = null, stageId = "design"))

        val wishes = db.quickNoteDao().observeByType(QuickNoteEntity.TYPE_WISH).first()
        assertEquals(listOf("w1"), wishes.map { it.id })

        val memos = db.quickNoteDao().observeByType(QuickNoteEntity.TYPE_MEMO).first()
        assertEquals(listOf("m1", "m2"), memos.map { it.id })
    }

    @Test
    fun delete_removesOnlyTargetRow() = runTest {
        db.quickNoteDao().upsert(note("q1"))
        db.quickNoteDao().upsert(note("q2"))

        db.quickNoteDao().delete("q1")

        val rest = db.quickNoteDao().exportAll()
        assertEquals(1, rest.size)
        assertEquals("q2", rest.first().id)
    }

    @Test
    fun exportAll_returnsAllRows() = runTest {
        db.quickNoteDao().upsert(note("q1"))
        db.quickNoteDao().upsert(note("q2"))
        db.quickNoteDao().upsert(note("q3"))

        assertEquals(3, db.quickNoteDao().exportAll().size)
    }

    @Test
    fun clearAll_emptiesTable() = runTest {
        db.quickNoteDao().upsert(note("q1"))
        db.quickNoteDao().upsert(note("q2"))

        db.quickNoteDao().clearAll()

        assertTrue(db.quickNoteDao().exportAll().isEmpty())
    }
}
