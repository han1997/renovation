package han1997.renovation.domain.planner

import kotlinx.serialization.Serializable

/** 重要度 4 档。 */
@Serializable
enum class Importance(val label: String) {
    NORMAL("普通"),
    MUST("必备"),
    IMPORTANT("重要"),
    CRITICAL("非常重要"),
}

/** 已选需求(一棵需求项,归属某类型)。 */
@Serializable
data class DemandPick(
    val itemKey: String,
    val itemName: String,
    val typeKey: String,
    val typeName: String,
    val spaceKey: String,   // 该需求所属预设空间目录(key)
    val spaceName: String,
)

/** 规划中的空间(预设或自定义)。 */
@Serializable
data class PlannerRoom(
    val id: String,
    val presetKey: String?, // 非空 = 来自预设空间
    val name: String,
    val area: Double = 0.0,
)

/** 需求 → 空间分配(含重要度)。同一需求可分配多个空间。 */
@Serializable
data class Assignment(
    val itemKey: String,
    val itemName: String,
    val roomId: String,
    val roomName: String,
    val importance: Importance = Importance.NORMAL,
)

/** 需求规划完整状态(存 Room planner_state)。 */
@Serializable
data class PlannerState(
    val step: Int = 1,
    val picked: List<DemandPick> = emptyList(),
    val rooms: List<PlannerRoom> = emptyList(),
    val assignments: List<Assignment> = emptyList(),
)

/** 生成清单的展示行(按空间分组)。 */
data class PlannerLine(
    val roomName: String,
    val itemName: String,
    val importance: Importance?,
)
