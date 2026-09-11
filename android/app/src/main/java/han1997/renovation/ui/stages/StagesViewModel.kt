package han1997.renovation.ui.stages

import android.app.Application
import androidx.lifecycle.viewModelScope
import han1997.renovation.data.db.ChecklistItemRow
import han1997.renovation.data.db.StageEntity
import han1997.renovation.data.db.TaskEntity
import han1997.renovation.data.db.TaskTemplateEntity
import han1997.renovation.data.knowledge.BuyItemJson
import han1997.renovation.ui.AppViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class TemplateTaskUi(
    val id: String,
    val text: String,
    val tip: String?,
    val done: Boolean,
    val dueDate: String? = null,
)

data class ChecklistGroupUi(
    val id: String,
    val name: String,
    val emoji: String,
    val items: List<ChecklistItemRow>,
)

data class StageDetailUi(
    val stage: StageEntity,
    val warnings: List<String>,
    val buy: List<BuyItemJson>,
    val templates: List<TemplateTaskUi>,
    val customTasks: List<TaskEntity>,
    val checklists: List<ChecklistGroupUi>,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StagesViewModel(application: Application) : AppViewModel(application) {

    private val today: String get() = container.todayProvider()

    val stages = container.stageRepo.observeAll()
    val progress = container.taskRepo.observeAllProgress()

    fun observeStageDetail(stage: StageEntity): Flow<StageDetailUi> {
        val warnings = StageJsonParse.warnings(stage.warningsJson)
        val buy = StageJsonParse.buy(stage.buyJson)
        val acceptIds = StageJsonParse.acceptIds(stage.acceptIdsJson)

        val templatesWithDone: Flow<List<TemplateTaskUi>> =
            container.taskRepo.observeTemplates(stage.id).flatMapLatest { list ->
                if (list.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        list.map { t ->
                            container.taskRepo.observeTemplateCompletion(t.id)
                                .map { c -> TemplateTaskUi(t.id, t.text, t.tip, c?.done ?: false, c?.dueDate) }
                        },
                    ) { arr -> arr.toList() }
                }
            }

        val checklistFlows: Flow<List<ChecklistGroupUi>> =
            if (acceptIds.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    acceptIds.map { id ->
                        container.checklistRepo.observeItems(id).map { items ->
                            val meta = container.knowledge.knowledge?.checklists?.firstOrNull { it.id == id }
                            ChecklistGroupUi(id, meta?.name ?: id, meta?.emoji ?: "✅", items)
                        }
                    },
                ) { arr -> arr.toList() }
            }

        return combine(templatesWithDone, container.taskRepo.observeCustomByStage(stage.id), checklistFlows) { tmpls, customs, checks ->
            StageDetailUi(stage, warnings, buy, tmpls, customs, checks)
        }
    }

    fun toggleTemplate(templateId: String, done: Boolean) {
        perform { container.taskRepo.setTemplateDone(templateId, done, today) }
    }

    fun toggleCustom(taskId: String, done: Boolean) {
        perform { container.taskRepo.setCustomTaskDone(taskId, done, today) }
    }

    fun addCustom(stageId: String, text: String, onSaved: () -> Unit = {}) {
        if (text.isBlank()) return
        perform(onSuccess = onSaved, singleFlight = true) { container.taskRepo.addTaskToStage(stageId, text) }
    }

    fun deleteCustom(taskId: String) {
        perform("已删除") { container.taskRepo.deleteCustom(taskId) }
    }

    fun deleteTemplate(templateId: String) {
        perform("已删除") { container.taskRepo.deleteTask(templateId, isTemplate = true) }
    }

    fun updateTemplateText(templateId: String, text: String, note: String?) {
        perform { container.taskRepo.updateTaskText(templateId, text, note, isTemplate = true) }
    }

    fun updateCustom(taskId: String, text: String, note: String?) {
        perform { container.taskRepo.updateTaskText(taskId, text, note, isTemplate = false) }
    }

    fun saveTask(id: String, text: String, note: String?, date: String?, template: Boolean, onSaved: () -> Unit) =
        perform(onSuccess = onSaved, singleFlight = true) { container.taskRepo.saveTask(id, text, note, date, template) }

    /** 恢复指定阶段的默认任务清单（从 assets/knowledge.json 重新写入）。 */
    fun restoreDefaultTasks(stageId: String) {
        perform("已恢复默认清单", singleFlight = true) {
            val knowledge = container.knowledge.knowledge ?: return@perform
            val stage = knowledge.stages.firstOrNull { it.id == stageId } ?: return@perform
            val templates = stage.tasks.mapIndexed { idx, t ->
                TaskTemplateEntity(
                    id = t.id,
                    stageId = stageId,
                    orderIndex = idx,
                    text = t.text,
                    tip = t.tip,
                )
            }
            container.taskRepo.restoreDefaultTasks(stageId, templates)
        }
    }

    /** 默认清单的任务数（用于判断「恢复默认清单」入口是否可用）。 */
    fun defaultTemplateCount(stageId: String): Int =
        container.knowledge.knowledge?.stages?.firstOrNull { it.id == stageId }?.tasks?.size ?: 0

    fun toggleChecklistItem(itemId: String, done: Boolean) {
        perform { container.checklistRepo.setChecked(itemId, done, today) }
    }
}
