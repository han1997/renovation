package han1997.renovation.ui.quote.engine

import han1997.renovation.domain.quote.*

import androidx.test.core.app.ApplicationProvider
import han1997.renovation.data.knowledge.KnowledgeCache
import han1997.renovation.data.db.BudgetCategoryEntity
import han1997.renovation.ui.quote.mainDefault
import han1997.renovation.ui.quote.surfaceDefault
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuoteCatalogPolishTest {
    private val catalog get() = KnowledgeCache(ApplicationProvider.getApplicationContext()).also { it.load() }.decoboxCatalog!!
    private val categories = listOf(BudgetCategoryEntity("all", "报价计划", "", 0L, 0, false, "2026-09-10"))
    private fun assertTotals(result: QuoteResult) {
        assertEquals(result.totalCents + result.summary.selfMainCents, result.estimatedTotalCents)
        val buckets = QuoteBudgetMapping.buckets(result, categories)
        assertEquals(result.estimatedTotalCents, buckets.sumOf { it.cents })
        assertEquals(result.estimatedTotalCents, QuoteBudgetMapping.preview(result, categories, buckets.associate { it.key to "all" }).changes.single().afterCents)
    }
    @Test fun everyMainMaterialTypeCanBeConfiguredAndProducesACost() {
        val c = catalog
        c.otherMains.forEach { main -> main.types.forEach { type ->
            val selected = mainDefault(c, main, type).copy(sourcing = Sourcing.INCLUDED)
            val state = QuotePlanState(mode = QuoteMode.SEMI, rooms = listOf(QuoteRoomState("r", "测试空间", 20.0, mains = listOf(selected))))
            QuoteValidation.validate(state, c)
            val result = QuoteCalculator.calculate(c, state)
            assertTrue("${main.id}/${type.id}", result.totalCents > 0)
            assertTotals(result)
        } }
    }
    @Test fun allModesIncludeSelfPurchasedMaterialsInBudgetComparison() {
        val c = catalog
        listOf(QuoteMode.FULL, QuoteMode.SEMI).forEach { mode ->
            val state = QuotePlanState(mode = mode, house = HouseInfo(90.0, 1L, 2.4),
                rooms = listOf(QuoteRoomState("r", "客厅", 20.0, wall = surfaceDefault(c.wall.first()), floor = surfaceDefault(c.floor.first(), Sourcing.INCLUDED))))
            val result = QuoteCalculator.calculate(c, state)
            assertTrue(result.summary.selfMainCents > 0)
            assertEquals(result.estimatedTotalCents - 1, result.overBudgetCents)
            assertTotals(result)
        }
        val paint = c.wall.first { it.id == "wall-paint" }.variants.first()
        val partial = QuotePlanState(mode = QuoteMode.PARTIAL, house = HouseInfo(90.0, 1L, 2.4), partialWorks = listOf(
            PartialWorkInput("wall-refresh", "full", true, listOf(PartialQtyRow("客厅", 30.0)), paint.id, paint.specs.first().id, Sourcing.SELF)))
        val result = QuoteCalculator.calculate(c, partial)
        assertTrue(result.summary.selfMainCents > 0)
        assertEquals(result.estimatedTotalCents - 1, result.overBudgetCents)
        assertTotals(result)
    }
    @Test fun explicitWaterproofAreaDoesNotFallBackToEstimatedWallArea() {
        val c = catalog
        val state = QuotePlanState(mode = QuoteMode.SEMI, rooms = listOf(QuoteRoomState("r", "卫生间", 20.0,
            extras = mapOf("waterproof" to ExtraInput(enabled = true, waterproofFloorArea = 5.0, waterproofWallArea = 0.0, waterproofUseEstimate = false)))))
        assertEquals(5L * c.spaceExtras.first { it.id == "waterproof" }.unitPrice * 100, QuoteCalculator.calculate(c, state).totalCents)
    }
    @Test fun partialCeilingInputIsValidatedAgainstTheCatalogLimit() {
        val c = catalog
        val state = QuotePlanState(mode = QuoteMode.SEMI, rooms = listOf(QuoteRoomState("r", "客厅", 20.0,
            ceilingPlan = CeilingPlan.PARTIAL, partialCeilingArea = 9.0, ceiling = surfaceDefault(c.ceiling.first(), Sourcing.INCLUDED))))
        QuoteValidation.validate(state, c)
        val ceiling = QuoteCalculator.calculate(c, state).lines.first { it.partLabel == "顶面" }
        assertEquals("9", ceiling.quantityText)
        assertThrows(IllegalArgumentException::class.java) { QuoteValidation.validate(state.copy(rooms = state.rooms.map { it.copy(partialCeilingArea = 99.0) }), c) }
    }
    @Test fun malformedOrOverflowingStatesAreRejectedBeforeSaving() {
        val c = catalog
        assertThrows(IllegalArgumentException::class.java) { QuoteValidation.validate(QuotePlanState(house = HouseInfo(Double.POSITIVE_INFINITY)), c) }
        assertThrows(IllegalArgumentException::class.java) { QuoteValidation.validate(QuotePlanState(managementRateOverride = 101), c) }
        assertThrows(IllegalArgumentException::class.java) { QuoteValidation.validate(QuotePlanState(rooms = listOf(QuoteRoomState("r", "房间", 10.0, floor = SurfaceSelection("missing")))), c) }
    }
}
