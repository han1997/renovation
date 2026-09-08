package com.renovation.guardian.ui.planner

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.renovation.guardian.data.knowledge.DecoboxRequirementsJson
import com.renovation.guardian.data.knowledge.DecoboxStateJson
import com.renovation.guardian.ui.AppViewModel
import com.renovation.guardian.ui.planner.engine.Assignment
import com.renovation.guardian.ui.planner.engine.DemandPick
import com.renovation.guardian.ui.planner.engine.Importance
import com.renovation.guardian.ui.planner.engine.PlannerLine
import com.renovation.guardian.ui.planner.engine.PlannerRoom
import com.renovation.guardian.ui.planner.engine.PlannerState
import kotlinx.coroutines.launch

class PlannerViewModel(application: Application) : AppViewModel(application) {

    val catalog: DecoboxRequirementsJson?
        get() = container.knowledge.decoboxRequirements

    var state by mutableStateOf(PlannerState())
        private set

    private val today: String get() = container.todayProvider()

    init {
        viewModelScope.launch {
            val saved = container.quotePlanRepo.getPlannerState() ?: return@launch
            val decoded = runCatching {
                DecoboxStateJson.json.decodeFromString(PlannerState.serializer(), saved.stateJson)
            }.getOrNull()
            if (decoded != null) state = decoded
        }
    }

    fun setStep(step: Int) {
        state = state.copy(step = step)
        persist()
    }

    fun togglePick(pick: DemandPick) {
        val exists = state.picked.any { it.itemKey == pick.itemKey }
        state = state.copy(picked = if (exists) state.picked.filterNot { it.itemKey == pick.itemKey } else state.picked + pick)
        persist()
    }

    fun clearPicks() {
        state = state.copy(picked = emptyList(), assignments = emptyList())
        persist()
    }

    fun addRoom(name: String, presetKey: String?, area: Double = 0.0) {
        val id = "pr${System.currentTimeMillis()}"
        state = state.copy(rooms = state.rooms + PlannerRoom(id, presetKey, name, area))
        persist()
    }

    fun removeRoom(id: String) {
        state = state.copy(
            rooms = state.rooms.filterNot { it.id == id },
            assignments = state.assignments.filterNot { it.roomId == id },
        )
        persist()
    }

    /** 自动分配:把已选需求平均分到所有空间。 */
    fun autoAssign() {
        if (state.rooms.isEmpty() || state.picked.isEmpty()) return
        val assigns = mutableListOf<Assignment>()
        state.picked.forEachIndexed { i, pick ->
            val room = state.rooms[i % state.rooms.size]
            assigns += Assignment(pick.itemKey, pick.itemName, room.id, room.name, Importance.NORMAL)
        }
        state = state.copy(assignments = assigns)
        persist()
    }

    fun toggleAssignment(pick: DemandPick, roomId: String, checked: Boolean) {
        val current = state.assignments.filterNot { it.itemKey == pick.itemKey && it.roomId == roomId }
        val updated = if (checked) {
            val room = state.rooms.firstOrNull { it.id == roomId } ?: return
            current + Assignment(pick.itemKey, pick.itemName, room.id, room.name, Importance.NORMAL)
        } else current
        state = state.copy(assignments = updated)
        persist()
    }

    fun setImportance(itemKey: String, roomId: String, importance: Importance) {
        state = state.copy(
            assignments = state.assignments.map { a ->
                if (a.itemKey == itemKey && a.roomId == roomId) a.copy(importance = importance) else a
            },
        )
        persist()
    }

    /** 清单行:未分配的空间占位提示;按空间分组保持 pick 顺序。 */
    fun lines(): List<PlannerLine> {
        val unassigned = state.picked.filter { p -> state.assignments.none { it.itemKey == p.itemKey } }
        val assignedLines = state.assignments.map { PlannerLine(it.roomName, it.itemName, it.importance) }
        val unassignedLines = if (unassigned.isNotEmpty() && state.rooms.isNotEmpty()) {
            listOf(PlannerLine(state.rooms.first().name, unassigned.joinToString("、") { it.itemName } + "(未分配)", Importance.NORMAL))
        } else emptyList()
        return assignedLines + unassignedLines
    }

    fun persist() {
        val json = DecoboxStateJson.json.encodeToString(PlannerState.serializer(), state)
        viewModelScope.launch { container.quotePlanRepo.savePlannerState(json, today) }
    }
}
