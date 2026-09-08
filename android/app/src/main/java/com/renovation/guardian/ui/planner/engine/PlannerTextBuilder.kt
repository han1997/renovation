package com.renovation.guardian.ui.planner.engine

/**
 * 需求规划清单文本导出:标题「装修需求清单」,按空间分组,含重要度标注。
 */
object PlannerTextBuilder {

    fun build(lines: List<PlannerLine>): String {
        val sb = StringBuilder()
        sb.append("装修需求清单").append("\n")
        lines.groupBy { it.roomName }.forEach { (room, items) ->
            sb.append("\n【$room】").append("\n")
            items.forEach { l ->
                sb.append("  ${l.itemName}(${l.importance.label})").append("\n")
            }
        }
        return sb.toString().trimEnd()
    }
}
