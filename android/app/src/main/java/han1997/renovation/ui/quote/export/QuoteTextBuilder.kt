package han1997.renovation.ui.quote.export

import han1997.renovation.domain.quote.QuoteLine
import han1997.renovation.domain.quote.QuoteResult
import han1997.renovation.util.MoneyUtil

/**
 * 报价清单文本导出(纯函数,可单测)。对齐 decobox 网站导出格式(research §13):
 * - 标题带模式名;房屋信息行;
 * - 按房间分组,行含「部位 · 名称 · 数量×单价」;
 * - 自购行标注 (自购) 且不进预计总价;
 * - 末尾分大项汇总 + 预计总价 + 预算对比。
 */
object QuoteTextBuilder {

    private const val LINE_SEP = "\n"

    fun build(result: QuoteResult, modeLabel: String, totalArea: Double, ceilingHeight: Double, unconfiguredRooms: List<String> = emptyList()): String {
        val sb = StringBuilder()
        sb.append("装修宝典($modeLabel)").append(LINE_SEP)
        sb.append("总建面 ${trimNum(totalArea)}m2 · 预算 ${MoneyUtil.format(result.budgetCents)} 元 · 完成面层高 ${trimNum(ceilingHeight)}m")
        sb.append(LINE_SEP).append(LINE_SEP)

        if (unconfiguredRooms.isNotEmpty()) sb.append("未配置选材的空间（未计空间费用）：${unconfiguredRooms.joinToString("、")}").append(LINE_SEP)

        // 按房间分组
        val byRoom = result.lines.filter { it.roomId != null }.groupBy { it.roomId }
        byRoom.forEach { (_, roomLines) ->
            val roomName = roomLines.firstNotNullOfOrNull { it.roomName } ?: ""
            sb.append("【$roomName】").append(LINE_SEP)
            roomLines.forEach { appendLine(sb, it) }
            sb.append(LINE_SEP)
        }

        // 非房间行(全屋工程 / 管理费 / 事项 / 局改费用)
        val global = result.lines.filter { it.roomId == null }
        if (global.isNotEmpty()) {
            sb.append("【其他】").append(LINE_SEP)
            global.forEach { appendLine(sb, it) }
            sb.append(LINE_SEP)
        }

        appendSummary(sb, result)
        return sb.toString().trimEnd()
    }

    private fun appendLine(sb: StringBuilder, l: QuoteLine) {
        val qty = l.quantityText
        val unitPrice = MoneyUtil.format(l.unitPriceCents)
        val subtotal = MoneyUtil.format(l.subtotalCents)
        val prefix = if (l.partLabel.isNotBlank() && l.roomId != null) "${l.partLabel} · " else ""
        val sourcing = if (l.sourcing == han1997.renovation.domain.quote.Sourcing.SELF) "(自购)" else ""
        sb.append("  $prefix${l.label}: ${qty}${l.unit} × ¥$unitPrice = ¥$subtotal$sourcing")
        l.note?.let { sb.append("($it)") }
        sb.append(LINE_SEP)
    }

    private fun appendSummary(sb: StringBuilder, r: QuoteResult) {
        val s = r.summary
        val lines = mutableListOf<Pair<String, Long>>()
        if (s.laborAuxCents > 0) lines += "人工辅材" to s.laborAuxCents
        if (s.includedMainCents > 0) lines += "代购主材" to s.includedMainCents
        if (s.houseWorkCents > 0) lines += "全屋工程" to s.houseWorkCents
        if (s.partialItemCents > 0) lines += "局改事项" to s.partialItemCents
        if (s.partialFeeCents > 0) lines += "局改费用" to s.partialFeeCents
        if (s.managementCents > 0) lines += "管理费" to s.managementCents
        sb.append(LINE_SEP).append("【费用小计】").append(LINE_SEP)
        lines.forEach { (k, v) ->
            sb.append("  $k:¥${MoneyUtil.format(v)}").append(LINE_SEP)
        }
        sb.append(LINE_SEP)
        sb.append("施工方报价:¥${MoneyUtil.format(r.totalCents)}").append(LINE_SEP)
        if (s.selfMainCents > 0) {
            sb.append("自购预计:¥${MoneyUtil.format(s.selfMainCents)}(不计入施工方报价)").append(LINE_SEP)
        }
        sb.append("本方案合计:¥${MoneyUtil.format(r.estimatedTotalCents)}").append(LINE_SEP)
        if (r.overBudget) {
            sb.append("超出预算:¥${MoneyUtil.format(r.overBudgetCents)}").append(LINE_SEP)
        } else {
            sb.append("预算剩余:¥${MoneyUtil.format(r.budgetRemainCents)}").append(LINE_SEP)
        }
    }

    private fun trimNum(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
}
