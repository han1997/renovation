package com.renovation.guardian.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    ],
    version = 1,
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

    companion object {
        private const val DB_NAME = "renovation.db"

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
                    .fallbackToDestructiveMigration() // v1 only; v2+ 需要写 Migration
                    .build()
                    .also { instance = it }
            }
    }
}