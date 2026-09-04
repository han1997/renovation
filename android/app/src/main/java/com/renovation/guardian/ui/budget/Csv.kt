package com.renovation.guardian.ui.budget

import com.renovation.guardian.data.db.ExpenseExportRow
import com.renovation.guardian.util.MoneyUtil

/** 把支出扁平视图拼成 CSV（含表头，金额换算为元）。 */
fun buildCsv(rows: List<ExpenseExportRow>): String {
    val sb = StringBuilder()
    sb.appendLine("日期,分类,项目,金额(元),备注")
    for (r in rows) {
        val amount = "%.2f".format(MoneyUtil.toYuan(r.amountCents))
        sb.appendLine("${csvCell(r.date)},${csvCell(r.categoryName)},${csvCell(r.name)},$amount,${csvCell(r.note ?: "")}")
    }
    if (rows.isEmpty()) {
        sb.appendLine("(暂无支出记录)")
    }
    return sb.toString()
}

private fun csvCell(s: String): String {
    return if (s.contains(',') || s.contains('"') || s.contains('\n')) {
        "\"" + s.replace("\"", "\"\"") + "\""
    } else {
        s
    }
}
