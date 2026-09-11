package han1997.renovation.domain.planner

import han1997.renovation.data.knowledge.DecoboxRequirementsJson

/** 搜索、推荐、引用归一化与导出共用的纯规则。 */
object PlannerRules {
    fun search(catalog: DecoboxRequirementsJson, query: String, spaceKey: String = ""): List<DemandPick> {
        val q = query.trim()
        return catalog.spaceList.filter { spaceKey.isBlank() || it.key == spaceKey }.flatMap { space ->
            catalog.typeListBySpace[space.key].orEmpty().flatMap { type ->
                catalog.functionsByType[type.key].orEmpty().filter { q.isBlank() || type.name.contains(q, true) || it.name.contains(q, true) }.map {
                    DemandPick(it.key, it.name, type.key, type.name, space.key, space.name)
                }
            }
        }.distinctBy { it.itemKey }
    }
    fun validateCatalogReferences(state: PlannerState, catalog: DecoboxRequirementsJson) {
        require(state.rooms.all { room -> room.presetKey == null || catalog.spaceList.any { it.key == room.presetKey } }) { "空间类型引用无效" }
        require(state.picked.all { pick ->
            catalog.typeListBySpace[pick.spaceKey].orEmpty().any { it.key == pick.typeKey } &&
                catalog.functionsByType[pick.typeKey].orEmpty().any { it.key == pick.itemKey }
        }) { "所选需求引用了不存在的目录项" }
    }
    fun normalize(state: PlannerState): PlannerState {
        val rooms = state.rooms.distinctBy { it.id }
        val picks = state.picked.distinctBy { it.itemKey }
        val assignments = state.assignments.distinctBy { it.itemKey to it.roomId }.mapNotNull { a ->
            val room = rooms.firstOrNull { it.id == a.roomId } ?: return@mapNotNull null
            val pick = picks.firstOrNull { it.itemKey == a.itemKey } ?: return@mapNotNull null
            a.copy(roomName = room.name, itemName = pick.itemName)
        }
        return state.copy(step = state.step.coerceIn(1, 4), rooms = rooms, picked = picks, assignments = assignments)
    }
    fun recommend(state: PlannerState): List<Assignment> = state.picked.filter { p -> state.assignments.none { it.itemKey == p.itemKey } }.flatMap { p ->
        state.rooms.filter { it.presetKey == p.spaceKey }.map { Assignment(p.itemKey, p.itemName, it.id, it.name, Importance.NORMAL) }
    }
    fun lines(state: PlannerState): List<PlannerLine> = state.rooms.flatMap { room ->
        state.picked.mapNotNull { p -> state.assignments.firstOrNull { it.roomId == room.id && it.itemKey == p.itemKey }?.let {
            PlannerLine(room.name, p.itemName, it.importance)
        } }
    } + state.picked.filter { p -> state.assignments.none { it.itemKey == p.itemKey } }.map { PlannerLine("未分配", it.itemName, null) }
}
