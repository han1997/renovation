package han1997.renovation.ui.quote.engine

import han1997.renovation.domain.quote.*

import han1997.renovation.data.knowledge.CraftStep
import han1997.renovation.data.knowledge.DecoboxCatalogJson
import han1997.renovation.data.knowledge.DefaultHouse
import han1997.renovation.data.knowledge.DoorBrand
import han1997.renovation.data.knowledge.DoorSeries
import han1997.renovation.data.knowledge.IdLabel
import han1997.renovation.data.knowledge.MainMaterial
import han1997.renovation.data.knowledge.MainMaterialType
import han1997.renovation.data.knowledge.MaterialSpec
import han1997.renovation.data.knowledge.MaterialVariant
import han1997.renovation.data.knowledge.OpeningSize
import han1997.renovation.data.knowledge.SurfaceCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 高级计价场景:局部吊顶、自购剔除、门超规、窗台石、洁具矩阵、主材 sourcing。
 */
class QuoteCalculatorAdvancedTest {

    private val wallPaint = SurfaceCategory(
        id = "wall-paint", label = "乳胶漆",
        variants = listOf(MaterialVariant("nippon", "立邦", "竹炭", listOf(MaterialSpec("nippon-5l", "5L", "桶", 237, 35.0)))),
        craftSteps = (1..7).map { CraftStep("c$it", "步$it", 5) }, // 工艺合计 35/m2
    )
    private val ceilingGypsum = SurfaceCategory(
        id = "ceiling-gypsum", label = "石膏板吊顶",
        variants = listOf(MaterialVariant("taishan", "泰山", "平顶", listOf(MaterialSpec("ts-flat", "标准", "m2", 130, 1.0)))),
        craftSteps = listOf(CraftStep("g-frame", "龙骨基层", 45), CraftStep("g-board", "石膏板安装", 20)),
        fixedCrafts = true,
    )

    private val catalog = DecoboxCatalogJson(
        version = "t", source = "t",
        defaultHouse = DefaultHouse(90.0, 200_000.0, 2.4),
        managementFeeRate = 8,
        wall = listOf(wallPaint),
        ceiling = listOf(ceilingGypsum),
        doors = listOf(
            DoorBrand(
                id = "guanzun", label = "冠尊",
                standardOpening = OpeningSize(2100, 900, 240),
                customOpening = true,
                surcharge = han1997.renovation.data.knowledge.DoorSurcharge(
                    tallFrom = 2100, tallTo = 2300, wideFrom = 900, wideTo = 1000,
                    perLeaf = 60, tubeFrom = 2300, tubePerLeaf = 60,
                ),
                series = listOf(DoorSeries("baked", "烤漆门", 655, mapOf("single" to 53, "double" to 64))),
            ),
        ),
        otherMains = listOf(
            MainMaterial(
                id = "door", label = "门", defaultQty = 1,
                types = listOf(MainMaterialType("door-wood", "木门", emptyList())),
            ),
            MainMaterial(
                id = "windowsill", label = "窗台石", defaultQty = 2,
                types = listOf(
                    MainMaterialType("ws-marble", "大理石", listOf(
                        MaterialSpec("ws-w22", "≤22cm", "延米", 150, 1.0),
                        MaterialSpec("ws-w28", "22-28cm", "延米", 190, 1.0),
                    )),
                ),
            ),
            MainMaterial(
                id = "sanitary", label = "洁具", defaultQty = 1,
                types = listOf(
                    MainMaterialType("sanitary-heater", "风暖一体机", emptyList()),
                    MainMaterialType("sanitary-toilet", "坐便", emptyList()),
                ),
            ),
        ),
        windowsillEdges = listOf(han1997.renovation.data.knowledge.WindowsillEdge("round", "圆边", 0)),
        windowsillDefaults = han1997.renovation.data.knowledge.WindowsillDefaults(1.5, 3, 3),
        heaterBrands = listOf(IdLabel("opple", "欧普")),
        heaterTiers = listOf(IdLabel("three", "三合一")),
        heaterMatrix = mapOf("opple" to mapOf("three" to 400, "four" to 600)),
        toiletBrands = listOf(IdLabel("arrow", "箭牌")),
        toiletKinds = listOf(IdLabel("normal", "普通")),
        toiletMatrix = mapOf("arrow" to mapOf("normal" to 900, "smart" to 2600)),
        spaceExtras = listOf(
            han1997.renovation.data.knowledge.SpaceExtra("waterproof", "防水工程", "area", "m2", 65),
            han1997.renovation.data.knowledge.SpaceExtra("pipeWrap", "包管", "count", "根", 380),
        ),
    )

    private fun fullRoom() = QuoteRoomState(
        id = "r1", name = "主卧", area = 16.0,
        wall = SurfaceSelection(
            categoryId = "wall-paint", variantId = "nippon", specId = "nippon-5l",
            craftIds = listOf("c1", "c2", "c3", "c4", "c5", "c6", "c7"),
            sourcing = Sourcing.INCLUDED,
        ),
    )

    @Test
    fun `局部吊顶 - 面积受限且板材含强制工艺`() {
        val room = fullRoom().copy(
            ceilingPlan = CeilingPlan.PARTIAL,
            partialCeilingArea = 99.0, // 超过限制,应被夹到建议值
            ceiling = SurfaceSelection(
                categoryId = "ceiling-gypsum", variantId = "taishan", specId = "ts-flat",
                sourcing = Sourcing.INCLUDED,
            ),
        )
        val state = QuotePlanState(
            mode = QuoteMode.FULL,
            house = HouseInfo(90.0, 200_000_00L, 2.4),
            rooms = listOf(room),
        )
        val r = QuoteCalculator.calculate(catalog, state)
        // 局部吊顶面积 <= 建议值:4*sqrt(16)*0.6 = 9.6,max(3,9.6)=9.6;area16 → min(16,9.6)=9.6
        val ceiling = r.lines.first { it.partLabel == "顶面" && it.label.contains("石膏板") }
        assertEquals(9.6, ceiling.quantityText.toDouble(), 1e-6)
        // 强制工艺 45+20=65/m2 × 9.6 = 624
        val craft = r.lines.first { it.label == "顶面安装工艺" }
        assertEquals(624_00L, craft.subtotalCents)
        // 板材 130×9.6=1248
        assertEquals(124_800L, ceiling.subtotalCents)
    }

    @Test
    fun `自购乳胶漆 - 不计入总价单独列出`() {
        val room = fullRoom().copy(
            wall = fullRoom().wall.copy(sourcing = Sourcing.SELF),
        )
        val state = QuotePlanState(
            mode = QuoteMode.FULL,
            house = HouseInfo(90.0, 200_000_00L, 2.4),
            rooms = listOf(room),
        )
        val r = QuoteCalculator.calculate(catalog, state)
        assertTrue(r.summary.selfMainCents > 0)
        // 自购行仍出现
        assertTrue(r.lines.any { it.sourcing == Sourcing.SELF && it.label.contains("乳胶漆") })
        // 总价不含自购:总价 = 工艺 + 管理费;工艺 35/m2 × 墙(4√16×2.4=38.4) + 顶(16×35)
        // 墙工艺 38.4*35=1344,顶工艺 16*35=560 → labor=1904;included=0;管理费 8%×1904=152.32→152
        assertEquals(152_00L, r.summary.managementCents)
        assertEquals(1904_00L + 152_00L, r.totalCents)
    }

    @Test
    fun `门超规加价与窗台石延米`() {
        val room = fullRoom().copy(
            mains = listOf(
                // 冠尊 烤漆门 655 + 双包 64;门洞 2200×950 → 超高60+超宽60
                MainSelection(
                    mainId = "door", typeId = "door-wood", specId = "baked",
                    doorBrandId = "guanzun",
                    doorFrame = "double", doorHeightMm = 2200, doorWidthMm = 950,
                    sourcing = Sourcing.INCLUDED,
                ),
                // 窗台石 大理石 ≤22 150 + 圆边(价格在 fixture 中未定义 edges price 显式为0?定义 IdLabel round 无 price)
                MainSelection(
                    mainId = "windowsill", typeId = "ws-marble", specId = "ws-w28",
                    windowsillEdgeId = null, sourcing = Sourcing.INCLUDED,
                ),
            ),
        )
        val state = QuotePlanState(
            mode = QuoteMode.FULL,
            house = HouseInfo(90.0, 200_000_00L, 2.4),
            rooms = listOf(room),
        )
        val r = QuoteCalculator.calculate(catalog, state)
        val door = r.lines.first { it.label.contains("冠尊") }
        assertEquals("樘", door.unit)
        // 655+64+60+60 = 839
        assertEquals(839_00L, door.subtotalCents)
        // 窗台石 qty = 1.5 + 0.06 = 1.56;190/m2? 延米;unitPrice 190,subtotal 1.56*190=296.4→296
        val ws = r.lines.first { it.label.contains("大理石") }
        assertEquals(1.56, ws.quantityText.toDouble(), 1e-6)
        assertEquals(296_00L, ws.subtotalCents)
    }

    @Test
    fun `洁具矩阵计价`() {
        val room = fullRoom().copy(
            mains = listOf(
                MainSelection(
                    mainId = "sanitary", typeId = "sanitary-heater", specId = "sanitary-heater-std",
                    heaterBrandId = "opple", heaterTierId = "three",
                    sourcing = Sourcing.INCLUDED,
                ),
                MainSelection(
                    mainId = "sanitary", typeId = "sanitary-toilet", specId = "sanitary-toilet-std",
                    toiletBrandId = "arrow", toiletKindId = "normal",
                    sourcing = Sourcing.SELF,
                ),
            ),
        )
        val state = QuotePlanState(
            mode = QuoteMode.SEMI,
            house = HouseInfo(90.0, 200_000_00L, 2.4),
            rooms = listOf(room),
        )
        val r = QuoteCalculator.calculate(catalog, state)
        val heater = r.lines.first { it.label.contains("风暖") }
        assertEquals(400_00L, heater.subtotalCents)
        val toilet = r.lines.first { it.label.contains("坐便") }
        assertEquals(Sourcing.SELF, toilet.sourcing)
        // 半包:坐便自购不计入总价。总价 = 工艺(1344+560) + 代购主材(墙漆 474 + 顶漆 237 + 风暖 400) = 3015 元
        assertEquals(301_500L, r.totalCents)
        assertEquals(900_00L, r.summary.selfMainCents)
    }
}
