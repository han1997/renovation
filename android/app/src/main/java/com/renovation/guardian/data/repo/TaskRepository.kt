package com.renovation.guardian.data.repo

import com.renovation.guardian.data.db.AppDatabase
import com.renovation.guardian.data.db.StageOverrideEntity
import com.renovation.guardian.data.db.StageProgressRow
import com.renovation.guardian.data.db.StageTaskView
import com.renovation.guardian.data.db.TaskCompletionEntity
import com.renovation.guardian.data.db.TaskEntity
import com.renovation.guardian.data.db.TaskTemplateEntity
import com.renovation.guardian.util.IdGen
import androidx.room.withTransaction
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

    /** 编辑任务文案 / 备注：模板任务改 task_template，自定义任务改 task。 */
    suspend fun updateTaskText(taskId: String, text: String, note: String?, isTemplate: Boolean) {
        if (text.isBlank()) return
        if (isTemplate) {
            val existing = db.taskTemplateDao().getById(taskId) ?: return
            db.taskTemplateDao().seedAll(listOf(existing.copy(text = text, tip = note)))
        } else {
            val existing = db.taskDao().getById(taskId) ?: return
            db.taskDao().upsert(existing.copy(text = text, tip = note))
        }
    }

    suspend fun saveTask(taskId: String, text: String, note: String?, date: String?, isTemplate: Boolean) {
        require(text.isNotBlank()) { "任务名称不能为空" }
        date?.let { kotlinx.datetime.LocalDate.parse(it) }
        db.withTransaction {
            updateTaskText(taskId, text.trim(), note, isTemplate)
            if (isTemplate) setTemplateDueDate(taskId, date)
            else {
                val task = requireNotNull(db.taskDao().getById(taskId)) { "任务已不存在" }
                db.taskDao().upsert(task.copy(dueDate = date))
            }
        }
    }

    /** 删除任务：模板任务与自定义任务都允许。 */
    suspend fun deleteTask(taskId: String, isTemplate: Boolean) = db.withTransaction {
        if (isTemplate) {
            db.taskTemplateDao().deleteById(taskId)
            db.taskDao().deleteCompletion(taskId)
        } else {
            db.taskDao().deleteTask(taskId)
        }
    }

    /** 新增任务到指定阶段（统一写入模板表，保证进度统计与首页聚合一致）。 */
    suspend fun addTaskToStage(stageId: String, text: String) {
        if (text.isBlank()) return
        val nextIndex = (db.taskTemplateDao().maxOrderIndex(stageId) ?: -1) + 1
        db.taskTemplateDao().seedAll(
            listOf(
                TaskTemplateEntity(
                    id = IdGen.new("ct"),
                    stageId = stageId,
                    orderIndex = nextIndex,
                    text = text,
                    tip = null,
                ),
            ),
        )
    }

    /**
     * 恢复指定阶段的默认任务清单：清掉该阶段全部模板任务后按 assets 重写，
     * 用户的勾选记录保留（按模板 id 匹配，仍然有效）。自定义任务（task 表）不受影响。
     */
    suspend fun restoreDefaultTasks(stageId: String, templates: List<TaskTemplateEntity>) {
        db.withTransaction {
            val stageTemplates = db.taskTemplateDao().listByStage(stageId)
            val defaults = templates.map { it.id }.toSet()
            // 非内置 ID 的任务是用户新增项，恢复默认不能清掉它们。
            stageTemplates.filter { it.id in defaults }.forEach { db.taskTemplateDao().deleteById(it.id) }
            db.taskTemplateDao().seedAll(templates)
        }
    }

    suspend fun setStageOverride(stageId: String, done: Boolean, today: String) {
        db.stageOverrideDao().setOverride(stageId, done, today)
    }

    suspend fun isStageOverridden(stageId: String): Boolean =
        db.stageOverrideDao().get(stageId) != null

    suspend fun getCompletion(templateId: String): TaskCompletionEntity? =
        db.taskDao().getCompletion(templateId)
}