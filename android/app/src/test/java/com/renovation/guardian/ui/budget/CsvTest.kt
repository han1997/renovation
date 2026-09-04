package com.renovation.guardian.ui.budget

import com.renovation.guardian.data.db.ExpenseExportRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** `buildCsv` 纯函数：CSV 拼装（表头、金额换算为元、字段转义、空列表兜底）。 */
class CsvTest {

    @Test
    fun buildCsv_writesHeaderAndRows() {
        val rows = listOf(
            ExpenseExportRow("2026-02-02", "主材", "瓷砖", 1500, "客厅"),
            ExpenseExportRow("2026-02-03", "水电", "改线", 200_00, null),
        )
        val csv = buildCsv(rows)
        val lines = csv.trimEnd().lines()
        assertEquals("日期,分类,项目,金额(元),备注", lines[0])
        assertEquals("2026-02-02,主材,瓷砖,15.00,客厅", lines[1])
        assertEquals("2026-02-03,水电,改线,200.00,", lines[2])
    }

    @Test
    fun buildCsv_escapesCommaAndQuote() {
        val rows = listOf(
            ExpenseExportRow("2026-02-02", "主材,辅材", "地,砖", 100, "备注\"含税\""),
        )
        val csv = buildCsv(rows)
        val lines = csv.trimEnd().lines()
        assertEquals(
            "2026-02-02,\"主材,辅材\",\"地,砖\",1.00,\"备注\"\"含税\"\"\"",
            lines[1],
        )
    }

    @Test
    fun buildCsv_emptyRows_emitsPlaceholder() {
        val csv = buildCsv(emptyList())
        val lines = csv.trimEnd().lines()
        assertEquals(2, lines.size)
        assertEquals("(暂无支出记录)", lines[1])
    }

    @Test
    fun buildCsv_amountUsesTwoDecimals() {
        val rows = listOf(ExpenseExportRow("2026-02-02", "主材", "水泥", 12345, null))
        val csv = buildCsv(rows)
        assertEquals("123.45", csv.trimEnd().lines()[1].split(',')[3])
    }
}