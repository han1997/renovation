package com.renovation.guardian.ui.quote.export

import com.renovation.guardian.ui.quote.engine.QuoteLine
import com.renovation.guardian.ui.quote.engine.QuoteResult
import com.renovation.guardian.ui.quote.engine.QuoteSummary
import com.renovation.guardian.ui.quote.engine.Sourcing
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteTextBuilderTest {

    private fun fakeLine(label: String, subtotalCents: Long, sourcing: Sourcing = Sourcing.INCLUDED) =
        QuoteLine(
            key = label, roomId = "r1", roomName = "客厅",
            partLabel = "墙面", label = label,
            quantityText = "1", unit = "m2",
            unitPriceCents = subtotalCents, subtotalCents = subtotalCents,
            sourcing = sourcing,
        )

    @Test
    fun `文本含房间分组与自购标注`() {
        val lines = listOf(
            fakeLine("乳胶漆", 474_00L),
            fakeLine("乳胶漆(自购)", 200_00L, Sourcing.SELF),
            QuoteLine("g1", null, null, "管理费", "管理费杂费(8%)", "1", "项",
                400_00L, 400_00L, Sourcing.INCLUDED, null),
        )
        val result = QuoteResult(
            lines = lines,
            summary = QuoteSummary(laborAuxCents = 0, includedMainCents = 474_00L,
                selfMainCents = 200_00L, managementCents = 400_00L),
            totalCents = 874_00L,
            budgetCents = 1000_00L,
            budgetRemainCents = 126_00L,
            overBudgetCents = 0,
            top3 = emptyList(),
        )
        val text = QuoteTextBuilder.build(result, "整装全包", 90.0, 2.4)
        assertTrue(text.contains("装修宝典(整装全包)"))
        assertTrue(text.contains("【客厅】"))
        assertTrue(text.contains("(自购)"))
        assertTrue(text.contains("预计总价:¥874"))
        assertTrue(text.contains("预算剩余:¥126"))
    }
}
