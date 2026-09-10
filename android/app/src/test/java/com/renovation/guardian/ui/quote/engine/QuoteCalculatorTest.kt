package com.renovation.guardian.ui.quote.engine

import com.renovation.guardian.domain.quote.*

import com.renovation.guardian.data.knowledge.CraftStep
import com.renovation.guardian.data.knowledge.DecoboxCatalogJson
import com.renovation.guardian.data.knowledge.MaterialSpec
import com.renovation.guardian.data.knowledge.MaterialVariant
import com.renovation.guardian.data.knowledge.SurfaceCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 计算引擎单元测试。数值按 decobox-data-model.md 公式手工推导,不依赖 assets。
 */
class QuoteCalculatorTest {

    // 夹具:墙面乳胶漆(竹炭 5L 237 元/桶,35m2/桶,7 步工艺)
    private val wallPaintCat = SurfaceCategory(
        id = "wall-paint",
        label = "乳胶漆",
        variants = listOf(
            MaterialVariant(
                id = "nippon-bamboo",
                brand = "立邦",
                series = "竹炭金装净味",
                specs = listOf(MaterialSpec("nippon-bamboo-5l", "5L 桶", "桶", 237, 35.0)),
            ),
        ),
        craftSteps = listOf(
            CraftStep("wall-scrape", "原墙大白铲除", 8),
            CraftStep("wall-primer", "墙锢涂膜", 4),
            CraftStep("wall-gypsum", "石膏找平", 15),
            CraftStep("wall-putty", "批刮腻子", 12),
            CraftStep("wall-sand", "砂纸打磨", 4),
            CraftStep("wall-corner", "阴阳角找直", 6),
            CraftStep("wall-paintwork", "刷乳胶漆", 10),
        ),
        fixedCrafts = false,
    )

    private val floorWoodCat = SurfaceCategory(
        id = "floor-wood",
        label = "木地板",
        variants = listOf(
            MaterialVariant(
                id = "deer",
                brand = "德尔",
                series = "强化",
                specs = listOf(MaterialSpec("deer-12", "1200×195×12mm", "m2", 74, 1.0)),
            ),
        ),
        craftSteps = emptyList(),
        fixedCrafts = false,
        includesSkirting = true,
    )

    private val catalog = DecoboxCatalogJson(
        version = "test",
        source = "test",
        craft = com.renovation.guardian.data.knowledge.CraftParams(15, 20),
        managementFeeRate = 8,
        defaultHouse = com.renovation.guardian.data.knowledge.DefaultHouse(90.0, 120000.0, 2.4),
        wall = listOf(wallPaintCat),
        floor = listOf(floorWoodCat),
        houseWorks = mapOf(
            "plumbing" to 150,
            "hauling" to 1500,
            "cleaning" to 500,
            "protection" to 500,
        ),
        partialFees = com.renovation.guardian.data.knowledge.PartialFees(25, 8, 300, 800),
        partialItems = listOf(
            com.renovation.guardian.data.knowledge.PartialItem(
                id = "wall-refresh",
                label = "墙面刷新",
                unit = "m2",
                integerQty = false,
                needsArea = false,
                tiers = listOf(
                    com.renovation.guardian.data.knowledge.PartialTier("light", "墙面结实,只是旧了", 45),
                    com.renovation.guardian.data.knowledge.PartialTier("full", "有掉皮、发霉、开裂", 85),
                ),
            ),
        ),
    )

    private val paintSel = SurfaceSelection(
        categoryId = "wall-paint",
        variantId = "nippon-bamboo",
        specId = "nippon-bamboo-5l",
        craftIds = listOf(
            "wall-scrape", "wall-primer", "wall-gypsum", "wall-putty",
            "wall-sand", "wall-corner", "wall-paintwork",
        ),
        sourcing = Sourcing.INCLUDED,
    )

    @Test
    fun `面积估算 - 墙面=4倍sqrt地面x层高,保留1位`() {
        val a = QuoteCalculator.estimateAreas(30.6, 2.4)
        // 4*sqrt(30.6)*2.4 = 53.10
        assertEquals(53.1, a.wall, 1e-6)
        assertEquals(30.6, a.ceiling, 1e-6)
        assertEquals(30.6, a.floor, 1e-6)
    }

    @Test
    fun `整装单房间 - 乳胶漆全代购`() {
        val room = QuoteRoomState(
            id = "r1",
            name = "客餐厅",
            area = 30.6,
            wall = paintSel,
            floor = SurfaceSelection(), // 未选地面
        )
        val state = QuotePlanState(
            mode = QuoteMode.FULL,
            house = HouseInfo(totalArea = 90.0, budgetCents = 120_000_00L, ceilingHeight = 2.4),
            rooms = listOf(room),
        )
        val r = QuoteCalculator.calculate(catalog, state)
        // 墙面面积 53.1:由「墙面基层工艺」行承载(59 元/m2 × 53.1);材料 2 桶 ×237 = 474
        val wallCraft = r.lines.first { it.label == "墙面基层工艺" }
        assertEquals(53.1, wallCraft.quantityText.toDouble(), 1e-6)
        val paintLine = r.lines.first { it.label.contains("乳胶漆") && it.unit == "桶" }
        assertEquals(2, paintLine.quantityText.toInt()) // ceil(53.1/35) = 2
        assertEquals(474_00L, paintLine.subtotalCents) // 2*237 = 474 元
        // 顶面走基层(墙面为乳胶漆):顶面=30.6 用同款乳胶漆(1 桶)+ 工艺行
        val ceilingCraft = r.lines.first { it.label == "顶面基层工艺" }
        assertEquals(30.6, ceilingCraft.quantityText.toDouble(), 1e-6)
        val ceilingLine = r.lines.first { it.partLabel == "顶面" && it.unit == "桶" }
        assertEquals(1, ceilingLine.quantityText.toInt()) // ceil(30.6/35)=1
        assertEquals(237_00L, ceilingLine.subtotalCents)

        // 工艺:墙 round(53.1×59)=3133 + 顶 round(30.6×59)=1805 = 4938 元(行小计圆整到元)
        assertEquals(493_800L, r.summary.laborAuxCents)
        // 代购主材: 474 + 237 = 711
        assertEquals(711_00L, r.summary.includedMainCents)
        // 管理费 8%*(4938.3+711)=8%*5649.3=451.944 -> 452 元 (round)
        // 注意:引擎对管理费在元上 round 再 cents
        assertEquals(452_00L, r.summary.managementCents)
    }

    @Test
    fun `半包无管理费`() {
        val room = QuoteRoomState(id = "r1", name = "客餐厅", area = 30.6, wall = paintSel)
        val state = QuotePlanState(
            mode = QuoteMode.SEMI,
            house = HouseInfo(totalArea = 90.0, budgetCents = 0L, ceilingHeight = 2.4),
            rooms = listOf(room),
        )
        val r = QuoteCalculator.calculate(catalog, state)
        assertEquals(0L, r.summary.managementCents)
    }

    @Test
    fun `全屋工程按面积或固定价`() {
        val state = QuotePlanState(
            mode = QuoteMode.FULL,
            house = HouseInfo(totalArea = 90.0, budgetCents = 0L, ceilingHeight = 2.4),
            rooms = emptyList(),
            houseWorks = mapOf(
                "plumbing" to HouseWorkInput(true),
                "hauling" to HouseWorkInput(true),
            ),
        )
        val r = QuoteCalculator.calculate(catalog, state)
        // 水电 150*90=13500;垃圾清运 1500
        val plumbing = r.lines.first { it.label == "水电改造" }
        assertEquals(1_350_000L, plumbing.subtotalCents)
        val hauling = r.lines.first { it.label == "垃圾清运" }
        assertEquals(150_000L, hauling.subtotalCents)
    }

    @Test
    fun `局改 - 墙面刷新light档与费用`() {
        val state = QuotePlanState(
            mode = QuoteMode.PARTIAL,
            house = HouseInfo(totalArea = 90.0, budgetCents = 0L, ceilingHeight = 2.4),
            partialWorks = listOf(
                PartialWorkInput(
                    itemId = "wall-refresh",
                    tierId = "light",
                    enabled = true,
                    qtyRows = listOf(PartialQtyRow("主卧", 40.0)),
                ),
            ),
            partialFees = mapOf(
                "demo" to PartialFeeInput(enabled = true),
                "protect" to PartialFeeInput(enabled = true),
            ),
        )
        val r = QuoteCalculator.calculate(catalog, state)
        val item = r.lines.first { it.partLabel == "事项" }
        assertEquals(40 * 45 * 100L, item.subtotalCents)
        // demo = 40*25 = 1000;protect = max(40*8,300)=320
        val demo = r.lines.first { it.label == "拆旧费" }
        assertEquals(100_000L, demo.subtotalCents)
        val protect = r.lines.first { it.label == "成品保护费" }
        assertEquals(320_00L, protect.subtotalCents)
        // 无管理费
        assertEquals(0L, r.summary.managementCents)
    }

    @Test
    fun `超预算警示`() {
        val room = QuoteRoomState(id = "r1", name = "客餐厅", area = 30.6, wall = paintSel)
        val state = QuotePlanState(
            mode = QuoteMode.FULL,
            house = HouseInfo(totalArea = 90.0, budgetCents = 100_00L, ceilingHeight = 2.4),
            rooms = listOf(room),
        )
        val r = QuoteCalculator.calculate(catalog, state)
        assertTrue(r.overBudget)
        assertEquals(100_00L, r.budgetCents)
    }
}
