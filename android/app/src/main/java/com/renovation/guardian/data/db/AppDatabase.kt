package com.renovation.guardian.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        HouseProfileEntity::class,
        StageEntity::class,
        TaskTemplateEntity::class,
        TaskEntity::class,
        TaskCompletionEntity::class,
        StageOverrideEntity::class,
        BudgetCategoryEntity::class,
        ExpenseEntity::class,
        SpaceNeedEntity::class,
        SpaceNeedStageEntity::class,
        ChecklistEntity::class,
        ChecklistItemEntity::class,
        ChecklistItemCheckEntity::class,
        NoteEntity::class,
        ContactEntity::class,
        QuickNoteEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun houseProfileDao(): HouseProfileDao
    abstract fun stageDao(): StageDao
    abstract fun taskTemplateDao(): TaskTemplateDao
    abstract fun taskDao(): TaskDao
    abstract fun stageOverrideDao(): StageOverrideDao
    abstract fun budgetCategoryDao(): BudgetCategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun spaceNeedDao(): SpaceNeedDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun noteDao(): NoteDao
    abstract fun contactDao(): ContactDao
    abstract fun quickNoteDao(): QuickNoteDao

    companion object {
        private const val DB_NAME = "renovation.db"

        /** v1 → v2：新增 `quick_note` 表（随手记），不触碰既有表，不丢数据。 */
        // internal：供 MigrationTest 复用（MigrationTestHelper 校验 schema 一致性）
        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 列定义与导出 schema（2.json）严格一致，避免 Room 迁移校验失败
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `quick_note` (
                        `id` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `category` TEXT,
                        `stage_id` TEXT,
                        `is_done` INTEGER NOT NULL,
                        `created_at` TEXT NOT NULL,
                        `updated_at` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME,
                )
                    .addCallback(object : Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // ensure foreign keys are on (Room enables by default but be explicit)
                            db.execSQL("PRAGMA foreign_keys = ON")
                        }
                    })
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration() // 兜底：仅对未定义 Migration 的版本跳变生效；v1→v2 已显式迁移
                    .build()
                    .also { instance = it }
            }
    }
}
