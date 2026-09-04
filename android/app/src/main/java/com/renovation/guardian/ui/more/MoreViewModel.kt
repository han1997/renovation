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
    val spaces = container.spaceNeedRepo.observeAll()

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
        viewModelScope.launch { container.contactRepo.upsert(name, role, phone, note, existingId) }
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
