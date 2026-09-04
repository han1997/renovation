package com.renovation.guardian.data.repo

import com.renovation.guardian.data.db.AppDatabase
import com.renovation.guardian.data.db.StageOverrideEntity
import com.renovation.guardian.data.db.StageProgressRow
import com.renovation.guardian.data.db.StageTaskView
import com.renovation.guardian.data.db.TaskCompletionEntity
import com.renovation.guardian.data.db.TaskEntity
import com.renovation.guardian.data.db.TaskTemplateEntity
import com.renovation.guardian.util.IdGen
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val db: AppDatabase) {

    fun observeBetween(startIso: String, endIso: String): Flow<List<StageTaskView>> =
        db.taskDao().observeBetween(startIso, endIso)

    fun observeAllProgress(): Flow<List<StageProgressRow>> =
        db.taskDao().observeAllProgress()

    fun observeTemplates(stageId: String): Flow<List<TaskTemplateEntity>> =
        db.taskTemplateDao().observeByStage(stageId)

    fun observeTemplateCompletion(templateId: String): Flow<TaskCompletionEntity?> =
        db.taskDao().observeCompletion(templateId)

    fun observeCustomByStage(stageId: String): Flow<List<TaskEntity>> =
        db.taskDao().observeByStage(stageId)

    suspend fun listTemplatesForStage(stageId: String) =
        db.taskTemplateDao().listByStage(stageId)

    suspend fun listCustomTasksForStage(stageId: String): List<TaskEntity> {
        // 简化：直接查所有 task，过滤 stage；数据量小可接受
        val all = db.taskDao().listAllForStage(stageId)
        return all
    }

    suspend fun setTemplateDone(templateId: String, done: Boolean, today: String) =
        db.taskDao().setTemplateDone(templateId, done, today)

    suspend fun setTemplateDueDate(templateId: String, date: String?) =
        db.taskDao().setTemplateDueDate(templateId, date)

    suspend fun setCustomTaskDone(taskId: String, done: Boolean, today: String) {
        val existing = db.taskDao().getById(taskId) ?: return
        db.taskDao().upsert(
            existing.copy(
                done = done,
                doneAt = if (done) today else null,
            ),
        )
    }

    suspend fun addCustom(stageId: String, text: String, dueDate: String?, today: String) {
        val task = TaskEntity(
            id = IdGen.new("ct"),
            stageId = stageId,
            sourceType = TaskEntity.TYPE_CUSTOM,
            text = text,
            tip = null,
            spaceId = null,
            dueDate = dueDate,
            done = false,
            doneAt = null,
            createdAt = today,
        )
        if (dueDate != null) {
            db.taskDao().upsert(task)
            // 不写到 task_completion（仅内置模板任务走 completion）；customTasks 的日期直接存 task.due_date
        } else {
            db.taskDao().upsert(task)
        }
    }

    suspend fun deleteCustom(taskId: String) {
        db.taskDao().deleteTask(taskId)
    }

    suspend fun setStageOverride(stageId: String, done: Boolean, today: String) {
        db.stageOverrideDao().setOverride(stageId, done, today)
    }

    suspend fun isStageOverridden(stageId: String): Boolean =
        db.stageOverrideDao().get(stageId) != null

    suspend fun getCompletion(templateId: String): TaskCompletionEntity? =
        db.taskDao().getCompletion(templateId)
}