package han1997.renovation.ui.planner

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import han1997.renovation.data.knowledge.DecoboxStateJson
import han1997.renovation.ui.AppViewModel
import han1997.renovation.ui.planner.engine.*
import han1997.renovation.domain.planner.*
import han1997.renovation.util.IdGen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlannerViewModel(application: Application) : AppViewModel(application) {
    val catalog get() = container.knowledge.decoboxRequirements
    var state by mutableStateOf(PlannerState()); private set
    var loading by mutableStateOf(true); private set
    var loadError by mutableStateOf<String?>(null); private set
    var saveError by mutableStateOf<String?>(null); private set
    var saving by mutableStateOf(false); private set
    private var revision = 0L
    private val edits = Channel<Pair<Long, PlannerState>>(Channel.CONFLATED)

    init {
        reload()
        viewModelScope.launch {
            for ((version, snapshot) in edits) {
                try {
                    val text = DecoboxStateJson.json.encodeToString(PlannerState.serializer(), snapshot)
                    withContext(NonCancellable) { container.quotePlanRepo.savePlannerState(text, container.todayProvider()) }
                    if (version == revision) { saving = false; saveError = null }
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { if (version == revision) { saving = false; saveError = "自动保存失败，当前编辑仍保留，请重试" } }
            }
        }
    }
    fun reload() {
        loading = true; loadError = null
        viewModelScope.launch {
            try {
                val saved = container.quotePlanRepo.getPlannerState()
                val restored = saved?.let { PlannerRules.normalize(DecoboxStateJson.json.decodeFromString(PlannerState.serializer(), it.stateJson)) } ?: PlannerState()
                catalog?.let { PlannerRules.validateCatalogReferences(restored, it) }
                state = restored
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { loadError = "规划读取失败，原数据未修改" }
            finally { loading = false }
        }
    }
    private fun change(update: (PlannerState) -> PlannerState) {
        if (loading || loadError != null) return
        state = PlannerRules.normalize(update(state)); persist()
    }
    fun persist() {
        if (loading || loadError != null) return
        saving = true; saveError = null; edits.trySend(++revision to state)
    }
    fun setStep(step: Int) {
        if (step == 3 && (state.rooms.isEmpty() || state.picked.isEmpty())) { notify("请先选择需求并添加空间"); return }
        if (step == 4 && state.picked.isEmpty()) { notify("请先选择需求"); return }
        change { it.copy(step = step.coerceIn(1, 4)) }
    }
    fun togglePick(pick: DemandPick) = change { s ->
        val picked = if (s.picked.any { it.itemKey == pick.itemKey }) s.picked.filterNot { it.itemKey == pick.itemKey } else s.picked + pick
        s.copy(picked = picked)
    }
    fun addRoom(name: String, presetKey: String?, area: Double = 0.0) {
        val clean = name.trim()
        if (clean.isEmpty() || state.rooms.any { it.name == clean }) { notify("空间名不能为空或重复"); return }
        change { it.copy(rooms = it.rooms + PlannerRoom(IdGen.new("pr"), presetKey, clean, area)) }
    }
    fun addPreset(key: String, name: String) {
        var candidate = name
        var number = 2
        while (state.rooms.any { it.name == candidate }) candidate = "$name${number++}"
        addRoom(candidate, key)
    }
    fun renameRoom(id: String, name: String) {
        if (name.isBlank() || state.rooms.any { it.id != id && it.name == name.trim() }) { notify("空间名不能为空或重复"); return }
        change { it.copy(rooms = it.rooms.map { r -> if (r.id == id) r.copy(name = name.trim()) else r }) }
    }
    fun removeRoom(id: String) = change { it.copy(rooms = it.rooms.filterNot { r -> r.id == id }) }
    fun recommendations() = PlannerRules.recommend(state)
    fun applyRecommendations(values: List<Assignment>) = change {
        val allowed = PlannerRules.recommend(it).map { a -> a.itemKey to a.roomId }.toSet()
        it.copy(assignments = it.assignments + values.filter { a -> (a.itemKey to a.roomId) in allowed })
    }
    fun toggleAssignment(pick: DemandPick, roomId: String, checked: Boolean) = change { s ->
        val current = s.assignments.filterNot { it.itemKey == pick.itemKey && it.roomId == roomId }
        val room = s.rooms.firstOrNull { it.id == roomId }
        s.copy(assignments = if (checked && room != null) current + Assignment(pick.itemKey, pick.itemName, room.id, room.name) else current)
    }
    fun setImportance(itemKey: String, roomId: String, importance: Importance) = change {
        it.copy(assignments = it.assignments.map { a -> if (a.itemKey == itemKey && a.roomId == roomId) a.copy(importance = importance) else a })
    }
    fun clearPicks() = change { it.copy(picked = emptyList(), assignments = emptyList(), step = 1) }
    fun lines() = PlannerRules.lines(state)
}
