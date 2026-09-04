package com.renovation.guardian.ui.stages

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.renovation.guardian.data.db.ChecklistItemRow
import com.renovation.guardian.data.db.StageEntity
import com.renovation.guardian.data.db.TaskEntity
import com.renovation.guardian.data.knowledge.BuyItemJson
import com.renovation.guardian.ui.AppViewModel
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
                                .map { c -> TemplateTaskUi(t.id, t.text, t.tip, c?.done ?: false) }
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
                            val meta = container.knowledge?.knowledge?.checklists?.firstOrNull { it.id == id }
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
        viewModelScope.launch { container.taskRepo.setTemplateDone(templateId, done, today) }
    }

    fun toggleCustom(taskId: String, done: Boolean) {
        viewModelScope.launch { container.taskRepo.setCustomTaskDone(taskId, done, today) }
    }

    fun addCustom(stageId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { container.taskRepo.addCustom(stageId, text, null, today) }
    }

    fun deleteCustom(taskId: String) {
        viewModelScope.launch { container.taskRepo.deleteCustom(taskId) }
    }

    fun toggleChecklistItem(itemId: String, done: Boolean) {
        viewModelScope.launch { container.checklistRepo.setChecked(itemId, done, today) }
    }
}
