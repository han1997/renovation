package com.renovation.guardian.data.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Room Migration v1 → v2（新增 `quick_note` 表）单元测试。
 *
 * AGP 9 不合并 unit test source set 的 assets，`MigrationTestHelper` 拿不到
 * 导出的 schema JSON，故改为手工建 v1 库：从 `app/schemas/.../1.json` 读取
 * 建表语句与 identityHash，用 `FrameworkSQLiteOpenHelperFactory` 落一个真实
 * 的 v1 数据库文件，再交给 Room 跑 [AppDatabase.MIGRATION_1_2] 打开校验：
 * - 迁移后 Room schema 校验通过（无 fallbackToDestructiveMigration 丢库）；
 * - v1 既有用户数据（profile / expense / note）迁移后原样保留；
 * - 迁移后可直接读写 quick_note。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(TEST_DB)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DB)
    }

    /** 从导出的 schema JSON 手工建指定版本的数据库文件。 */
    private fun createDatabaseAtVersion(version: Int): SupportSQLiteDatabase {
        val schema = loadSchemaJson(version)
        val database = schema["database"]!!.jsonObject
        val entities = database["entities"]!!.jsonArray
        val setupQueries = database["setupQueries"]!!.jsonArray

        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(TEST_DB)
            .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    entities.forEach { entity ->
                        val obj = entity.jsonObject
                        val table = obj["tableName"]!!.jsonPrimitive.content
                        db.execSQL(obj["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                        obj["indices"]?.jsonArray?.forEach { index ->
                            db.execSQL(
                                index.jsonObject["createSql"]!!.jsonPrimitive.content
                                    .replace("\${TABLE_NAME}", table),
                            )
                        }
                    }
                    setupQueries.forEach { db.execSQL(it.jsonPrimitive.content) }
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        return androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(configuration).writableDatabase
    }

    private fun createV1Database(): SupportSQLiteDatabase = createDatabaseAtVersion(1)

    private fun loadSchemaJson(version: Int): kotlinx.serialization.json.JsonObject {
        val candidates = listOf(
            File("schemas/com.renovation.guardian.data.db.AppDatabase/$version.json"),
            File("app/schemas/com.renovation.guardian.data.db.AppDatabase/$version.json"),
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("找不到 schema 文件，尝试过: ${candidates.map { it.absolutePath }}")
        return Json.parseToJsonElement(file.readText()).jsonObject
    }

    @Test
    fun migrate1To2_preservesExistingData() = runBlocking {
        // 1) 手工建 v1 库并写入用户数据
        createV1Database().use { v1 ->
            v1.execSQL(
                """
                INSERT INTO house_profile
                    (id, area_m2, tier_id, mode_id, grade_id, start_date, total_budget_cents, style_id, style_quiz_at, created_at)
                VALUES
                    (1, 88.0, 'mid', 'full', 'eco', '2026-01-01', 150000, 'style_japandi', NULL, '2026-01-01')
                """.trimIndent(),
            )
            v1.execSQL(
                """
                INSERT INTO expense (id, name, amount_cents, category_id, date, note, created_at)
                VALUES ('ex1', '瓷砖', 150000, 'bc1', '2026-01-02', '客厅', '2026-01-02')
                """.trimIndent(),
            )
            v1.execSQL(
                """
                INSERT INTO note (id, title, body, updated_at, created_at)
                VALUES ('nt1', '标题', '正文', '2026-01-03', '2026-01-03')
                """.trimIndent(),
            )
        }

        // 2) Room 打开 v1 文件库（自动跑 MIGRATION_1_2 并做 schema 校验）
        val db = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

        try {
            val raw = db.openHelper.writableDatabase

            // 3) 既有数据不丢
            raw.query("SELECT area_m2, total_budget_cents, style_id FROM house_profile WHERE id = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(88.0, c.getDouble(0), 0.0)
                assertEquals(150000L, c.getLong(1))
                assertEquals("style_japandi", c.getString(2))
            }
            raw.query("SELECT name, amount_cents FROM expense WHERE id = 'ex1'").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("瓷砖", c.getString(0))
                assertEquals(150000L, c.getLong(1))
            }
            raw.query("SELECT title, body FROM note WHERE id = 'nt1'").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("标题", c.getString(0))
            }

            // 4) 新表可用（走 DAO 读写 quick_note）
            db.quickNoteDao().upsert(
                QuickNoteEntity(
                    id = "qn1",
                    content = "买冰箱",
                    type = QuickNoteEntity.TYPE_WISH,
                    category = QuickNoteEntity.CATEGORY_APPLIANCE,
                    stageId = null,
                    isDone = false,
                    createdAt = "2026-09-01",
                    updatedAt = "2026-09-01",
                ),
            )
            val saved = db.quickNoteDao().getById("qn1")
            assertEquals("买冰箱", saved?.content)
            assertEquals(QuickNoteEntity.TYPE_WISH, saved?.type)
        } finally {
            db.close()
        }
    }

    @Test
    fun migrate2To3_dropsSpaceNeed_andAddsQuoteTables() = runBlocking {
        // 1) 手工建 v2 库并写入 space_need + expense 用户数据
        createDatabaseAtVersion(2).use { v2 ->
            v2.execSQL(
                """
                INSERT INTO house_profile
                    (id, area_m2, tier_id, mode_id, grade_id, start_date, total_budget_cents, style_id, style_quiz_at, created_at)
                VALUES
                    (1, 88.0, 'mid', 'full', 'eco', '2026-01-01', 150000, 'style_japandi', NULL, '2026-01-01')
                """.trimIndent(),
            )
            v2.execSQL(
                """
                INSERT INTO space_need (id, preset_id, name, emoji, description, budget_category_id, budget_note, is_custom, created_at)
                VALUES ('sp1', 'preset_1', '主卧', '🛏️', '旧空间需求', NULL, NULL, 0, '2026-01-02')
                """.trimIndent(),
            )
            v2.execSQL(
                """
                INSERT INTO space_need_stage (space_id, stage_id, order_index)
                VALUES ('sp1', 's1', 0)
                """.trimIndent(),
            )
            v2.execSQL(
                """
                INSERT INTO expense (id, name, amount_cents, category_id, date, note, created_at)
                VALUES ('ex1', '瓷砖', 150000, 'bc1', '2026-01-02', '客厅', '2026-01-02')
                """.trimIndent(),
            )
        }

        // 2) Room 打开 v2 文件库(自动跑 MIGRATION_2_3)
        val db = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

        try {
            // 3) 旧 space_need 表被删除;数据不再可见
            val tables = db.openHelper.readableDatabase
                .query("SELECT name FROM sqlite_master WHERE type='table'").use { c ->
                    val names = mutableListOf<String>()
                    while (c.moveToNext()) names += c.getString(0)
                    names
                }
            assertTrue("space_need" !in tables)
            assertTrue("space_need_stage" !in tables)

            // 4) 既有 expense 数据不丢
            db.openHelper.readableDatabase.query("SELECT amount_cents FROM expense WHERE id = 'ex1'").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(150000L, c.getLong(0))
            }

            // 5) 新表可读写
            db.quotePlanDao().upsert(
                QuotePlanEntity(
                    id = "qp1", name = "我的方案", mode = "full",
                    stateJson = "{}", createdAt = "2026-09-08", updatedAt = "2026-09-08",
                ),
            )
            assertEquals("我的方案", db.quotePlanDao().getById("qp1")?.name)

            db.plannerStateDao().upsert(
                PlannerStateEntity(
                    id = PlannerStateEntity.SINGLE_ROW_ID,
                    stateJson = "{}", updatedAt = "2026-09-08",
                ),
            )
            assertEquals(PlannerStateEntity.SINGLE_ROW_ID, db.plannerStateDao().get()?.id)
        } finally {
            db.close()
        }
    }

    companion object {
        private const val TEST_DB = "migration-test.db"
    }
}
