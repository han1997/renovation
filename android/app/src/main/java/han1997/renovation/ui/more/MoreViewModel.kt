package han1997.renovation.ui.more

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import han1997.renovation.data.repo.ImportResult
import han1997.renovation.ui.AppViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MoreViewModel(application: Application) : AppViewModel(application) {

    private val today: String get() = container.todayProvider()

    val profile: Flow<han1997.renovation.data.db.HouseProfileEntity?> = container.houseProfileRepo.observe()
    val contacts = container.contactRepo.observeAll()
    val notes = container.noteRepo.observeAll()
    val quickNotes = container.quickNoteRepo.observeAll()

    /** 14 阶段目录(随手记「阶段备忘」关联用)。 */
    val stages = container.stageRepo.observeAll()

    val tiers = container.knowledge.prices?.tiers ?: emptyList()
    val modes = container.knowledge.knowledge?.modes ?: emptyList()
    val grades = container.knowledge.prices?.grades ?: emptyList()
    val styles = container.knowledge.knowledge?.styles ?: emptyList()

    var preparedImport by androidx.compose.runtime.mutableStateOf<han1997.renovation.data.repo.PreparedImport?>(null)
        private set
    fun prepareBackupImport(text: String) = perform(success = null) {
        preparedImport = container.importExportRepo.prepareImport(text)
    }
    fun dismissImport() { preparedImport = null }
    fun confirmImport() = perform("导入成功", singleFlight = true) {
        val prepared = requireNotNull(preparedImport)
        val result = container.importExportRepo.restore(prepared)
        check(result.success) { result.error ?: "导入失败" }
        preparedImport = null
    }
    fun saveHouse(area: Double, tier: String, mode: String, grade: String, date: String?, budget: Long, onSaved: () -> Unit) =
        perform(onSuccess = onSaved, singleFlight = true) { container.houseProfileRepo.saveSettings(area, tier, mode, grade, date, budget) }
    fun updateProfile(areaM2: Double, tierId: String, modeId: String, gradeId: String, startDate: String?) =
        perform { container.houseProfileRepo.updateProfile(areaM2, tierId, modeId, gradeId, startDate) }
    fun setTotalBudgetCents(cents: Long) = perform { require(cents >= 0); container.houseProfileRepo.setTotalBudgetCents(cents) }
    fun updateStyle(styleId: String, onSaved: () -> Unit = {}) = perform(onSuccess = onSaved, singleFlight = true) { container.houseProfileRepo.updateStyle(styleId) }
    fun upsertContact(name: String, role: String?, phone: String?, note: String?, existingId: String?, onSaved: () -> Unit = {}) = perform(onSuccess = onSaved, singleFlight = true) {
        require(name.isNotBlank()) { "请填写联系人姓名" }
        container.contactRepo.upsert(name.trim(), role, phone, note, existingId, today)
    }
    fun deleteContact(id: String) = perform("已删除") { container.contactRepo.delete(id) }
    fun upsertNote(title: String, body: String, existingId: String?, onSaved: () -> Unit = {}) = perform(onSuccess = onSaved, singleFlight = true) {
        require(title.isNotBlank() || body.isNotBlank()) { "请填写笔记内容" }
        container.noteRepo.upsert(title, body, today, existingId)
    }
    fun deleteNote(id: String) = perform("已删除") { container.noteRepo.delete(id) }
    fun upsertQuickNote(content: String, type: String, category: String?, stageId: String?, existingId: String?, onSaved: () -> Unit = {}) = perform(onSuccess = onSaved, singleFlight = true) {
        require(content.isNotBlank()) { "请填写内容" }
        container.quickNoteRepo.upsert(content, type, category, stageId, existingId, today)
    }
    fun setQuickNoteDone(id: String, done: Boolean) = perform(success = null) { container.quickNoteRepo.setDone(id, done, today) }
    fun deleteQuickNote(id: String) = perform("已删除") { container.quickNoteRepo.delete(id) }
    suspend fun exportJsonString(): String = container.importExportRepo.exportJson()
    suspend fun importJson(text: String): ImportResult = container.importExportRepo.importJson(text)
    fun resetAll() = perform("已清空用户数据", singleFlight = true) { container.clearAllData() }
}
