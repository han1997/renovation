package com.renovation.guardian.ui.more

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.renovation.guardian.data.repo.ImportResult
import com.renovation.guardian.ui.AppViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MoreViewModel(application: Application) : AppViewModel(application) {

    private val today: String get() = container.todayProvider()

    val profile: Flow<com.renovation.guardian.data.db.HouseProfileEntity?> = container.houseProfileRepo.observe()
    val contacts = container.contactRepo.observeAll()
    val notes = container.noteRepo.observeAll()
    val quickNotes = container.quickNoteRepo.observeAll()
    val spaces = container.spaceNeedRepo.observeAll()

    /** 14 阶段目录（随手记「阶段备忘」关联用）。 */
    val stages = container.stageRepo.observeAll()

    val tiers = container.knowledge.prices?.tiers ?: emptyList()
    val modes = container.knowledge.knowledge?.modes ?: emptyList()
    val grades = container.knowledge.prices?.grades ?: emptyList()
    val styles = container.knowledge.knowledge?.styles ?: emptyList()
    val spacePresets = container.knowledge.knowledge?.spaceNeeds ?: emptyList()

    fun updateProfile(areaM2: Double, tierId: String, modeId: String, gradeId: String, startDate: String?) {
        viewModelScope.launch { container.houseProfileRepo.updateProfile(areaM2, tierId, modeId, gradeId, startDate) }
    }

    fun setTotalBudgetCents(cents: Long) {
        viewModelScope.launch { container.houseProfileRepo.setTotalBudgetCents(cents) }
    }

    fun updateStyle(styleId: String) {
        viewModelScope.launch { container.houseProfileRepo.updateStyle(styleId) }
    }

    fun upsertContact(name: String, role: String?, phone: String?, note: String?, existingId: String?) {
        viewModelScope.launch { container.contactRepo.upsert(name, role, phone, note, existingId, today) }
    }

    fun deleteContact(id: String) {
        viewModelScope.launch { container.contactRepo.delete(id) }
    }

    fun upsertNote(title: String, body: String, existingId: String?) {
        viewModelScope.launch { container.noteRepo.upsert(title, body, today, existingId) }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch { container.noteRepo.delete(id) }
    }

    /** 新增 / 编辑随手记（type: wish|memo；category 仅 wish；stageId 仅 memo）。 */
    fun upsertQuickNote(content: String, type: String, category: String?, stageId: String?, existingId: String?) {
        viewModelScope.launch {
            container.quickNoteRepo.upsert(content, type, category, stageId, existingId, today)
        }
    }

    /** 勾选 / 取消随手记完成。 */
    fun setQuickNoteDone(id: String, done: Boolean) {
        viewModelScope.launch { container.quickNoteRepo.setDone(id, done, today) }
    }

    fun deleteQuickNote(id: String) {
        viewModelScope.launch { container.quickNoteRepo.delete(id) }
    }

    fun addSpaceFromPreset(presetId: String) {
        viewModelScope.launch {
            val p = spacePresets.firstOrNull { it.id == presetId } ?: return@launch
            container.spaceNeedRepo.addPreset(
                presetId = p.id,
                name = p.name,
                emoji = p.emoji,
                desc = p.desc,
                stageIds = p.stageIds,
                budgetCategoryId = p.budgetCat,
                budgetNote = p.budgetNote,
                tasks = p.tasks.map { it.stageId to it.text },
                today = today,
            )
        }
    }

    fun deleteSpace(id: String) {
        viewModelScope.launch { container.spaceNeedRepo.delete(id) }
    }

    suspend fun exportJsonString(): String = container.importExportRepo.exportJson()

    suspend fun importJson(text: String): ImportResult = container.importExportRepo.importJson(text)

    fun resetAll() {
        viewModelScope.launch { container.clearAllData() }
    }
}
