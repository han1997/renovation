package com.renovation.guardian.data.repo

import com.renovation.guardian.data.db.AppDatabase
import com.renovation.guardian.data.knowledge.KnowledgeCache
import com.renovation.guardian.data.knowledge.KnowledgeSeeder
import androidx.room.withTransaction

/**
 * 仓库层容器。ViewModel 通过 [AppContainer] 拿到需要的仓库，避免到处构造
 * `AppDatabase` / `KnowledgeCache`。
 */
interface AppContainer {
    val db: AppDatabase
    val knowledge: KnowledgeCache
    val seeder: KnowledgeSeeder
    val houseProfileRepo: HouseProfileRepository
    val stageRepo: StageRepository
    val taskRepo: TaskRepository
    val budgetRepo: BudgetRepository
    val spaceNeedRepo: SpaceNeedRepository
    val checklistRepo: ChecklistRepository
    val noteRepo: NoteRepository
    val contactRepo: ContactRepository
    val quickNoteRepo: QuickNoteRepository
    val importExportRepo: ImportExportRepository
    val todayProvider: () -> String

    /** 清除全部用户数据（保留只读种子：阶段 / 模板任务 / 验收清单）。 */
    suspend fun clearAllData()
}

class DefaultAppContainer(
    override val db: AppDatabase,
    override val knowledge: KnowledgeCache,
) : AppContainer {

    override val seeder: KnowledgeSeeder by lazy { KnowledgeSeeder(db, knowledge) }
    override val houseProfileRepo: HouseProfileRepository by lazy { HouseProfileRepository(db, seeder, ::today) }
    override val stageRepo: StageRepository by lazy { StageRepository(db) }
    override val taskRepo: TaskRepository by lazy { TaskRepository(db) }
    override val budgetRepo: BudgetRepository by lazy { BudgetRepository(db) }
    override val spaceNeedRepo: SpaceNeedRepository by lazy { SpaceNeedRepository(db, taskRepo) }
    override val checklistRepo: ChecklistRepository by lazy { ChecklistRepository(db) }
    override val noteRepo: NoteRepository by lazy { NoteRepository(db) }
    override val contactRepo: ContactRepository by lazy { ContactRepository(db) }
    override val quickNoteRepo: QuickNoteRepository by lazy { QuickNoteRepository(db) }
    override val importExportRepo: ImportExportRepository by lazy { ImportExportRepository(db) }

    override val todayProvider: () -> String = ::today

    override suspend fun clearAllData() {
        db.withTransaction {
            db.houseProfileDao().deleteAll()
            db.taskDao().clearCompletions()
            db.taskDao().clearTasks()
            db.taskDao().clearOverrides()
            db.budgetCategoryDao().clear()
            db.expenseDao().clear()
            db.checklistDao().clearChecks()
            db.noteDao().clearAll()
            db.contactDao().clearAll()
            db.quickNoteDao().clearAll()
            db.spaceNeedDao().clearAll()
        }
    }

    companion object {
        fun today(): String = com.renovation.guardian.util.DateUtil.today()
    }
}