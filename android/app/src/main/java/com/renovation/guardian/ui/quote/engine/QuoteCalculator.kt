package com.renovation.guardian.ui.quote.engine

import com.renovation.guardian.data.knowledge.DecoboxCatalogJson
import com.renovation.guardian.data.knowledge.MaterialSpec
import com.renovation.guardian.data.knowledge.SurfaceCategory
import kotlin.math.ceil
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** 报价明细行(金额 cents;每行小计圆整到元)。 */
data class QuoteLine(
    val key: String,
    val roomId: String?,
    val roomName: String?,
    val partLabel: String,   // 人工辅材 / 主材 / 全屋工程 / 管理费 / 事项 / 局改费用
    val label: String,
    val quantityText: String,
    val unit: String,
    val unitPriceCents: Long,
    val subtotalCents: Long,
    val sourcing: Sourcing,
    val note: String? = null,
)

/** 报价汇总(全部 cents)。 */
data class QuoteSummary(
    val laborAuxCents: Long = 0L,        // 人工辅材(工艺/施工)
    val includedMainCents: Long = 0L,    // 代购主材
    val selfMainCents: Long = 0L,        // 自购主材(不计入总价)
    val houseWorkCents: Long = 0L,       // 全屋工程
    val managementCents: Long = 0L,      // 管理费(整装)
    val partialItemCents: Long = 0L,     // 局改事项
    val partialFeeCents: Long = 0L,      // 局改费用
) {
    /** 计入总价的部分(自购主材除外)。 */
    val chargeable: Long
        get() = laborAuxCents + includedMainCents + houseWorkCents + managementCents +
                partialItemCents + partialFeeCents
}

/** 报价结果。 */
data class QuoteResult(
    val lines: List<QuoteLine>,
    val summary: QuoteSummary,
    val totalCents: Long,
    val budgetCents: Long,
    val budgetRemainCents: Long,
    val overBudgetCents: Long,
    val top3: List<QuoteLine>,
) {
    val overBudget: Boolean get() = overBudgetCents > 0L
}

/** 面积估算结果。 */
data class AreaEstimates(
    val wall: Double,
    val ceiling: Double,
    val floor: Double,
)

/**
 * decobox 报价计算引擎(纯 Kotlin,无 Android 依赖)。
 *
 * 计价规则还原 decobox.online v1.4.0(research/decobox-data-model.md):
 * - 面积保留 1 位小数;金额行小计圆整到元;
 * - 自购主材不计入总价、单独列出;
 * - 整装管理费 = 8% ×(代购主材 + 人工辅材),半包无;
 * - 局改 demo=25元/m2、protect=max(8×量,300)、hauling=800(可开关/覆盖)。
 */
object QuoteCalculator {

    // ── 数学 ──
    private fun z(v: Double): Double = round(v * 10) / 10.0
    private fun centsOf(yuan: Double): Long = round(yuan * 100).toLong()
    private fun qtyText(q: Double): String {
        val r = round(q * 100) / 100
        return if (r == r.roundToInt().toDouble()) r.roundToInt().toString()
        else r.toString()
    }

    // ── 面积估算 ──
    fun estimateAreas(floorArea: Double, ceilingHeight: Double): AreaEstimates {
        val f = z(floorArea)
        val wall = z(4.0 * sqrt(f) * ceilingHeight)
        return AreaEstimates(wall, f, f)
    }

    fun suggestPartialCeiling(floorArea: Double): Double {
        val a = floorArea.coerceAtLeast(0.0)
        val bound = (4.0 * sqrt(a) * 0.6).coerceAtLeast(3.0)
        return z(a.coerceAtMost(bound))
    }

    // ── 入口 ──
    fun calculate(catalog: DecoboxCatalogJson, state: QuotePlanState): QuoteResult =
        if (state.mode == QuoteMode.PARTIAL) calcPartial(catalog, state) else calcFullSemi(catalog, state)

    // ═══════════════════════════ 整装 / 半包 ═══════════════════════════
    private fun calcFullSemi(catalog: DecoboxCatalogJson, state: QuotePlanState): QuoteResult {
        val lines = mutableListOf<QuoteLine>()

        for (room in state.rooms) {
            val areas = estimateAreas(room.area, state.house.ceilingHeight)
            val wallCat = catOf(catalog, "wall", room.wall.categoryId)
            val floorCat = catOf(catalog, "floor", room.floor.categoryId)
            val ceilingCat = catOf(catalog, "ceiling", room.ceiling.categoryId)

            // 墙面:主材 + 工艺
            if (room.wall.categoryId.isNotBlank() && wallCat.id.isNotBlank()) {
                val area = room.wall.areaOverride ?: areas.wall
                addMaterialRow(lines, wallCat, room.wall, area, room, "墙面")
                addCraftRow(lines, wallCat, room.wall, area, room, "墙面基层工艺")
            }
            // 地面
            if (room.floor.categoryId.isNotBlank() && floorCat.id.isNotBlank()) {
                val area = room.floor.areaOverride ?: areas.floor
                addMaterialRow(lines, floorCat, room.floor, area, room, "地面")
                addCraftRow(lines, floorCat, room.floor, area, room, "地面铺贴工艺")
            }
            // 顶面
            when (room.ceilingPlan) {
                CeilingPlan.NONE -> {
                    // 不吊顶:墙面为乳胶漆时顶面走同款乳胶漆 + 基层工艺
                    if (room.wall.categoryId == "wall-paint" && wallCat.id == "wall-paint") {
                        addMaterialRow(lines, wallCat, room.wall, areas.ceiling, room, "顶面")
                        addCraftRow(lines, wallCat, room.wall, areas.ceiling, room, "顶面基层工艺")
                    }
                }
                CeilingPlan.PARTIAL, CeilingPlan.FULL -> {
                    if (room.ceiling.categoryId.isNotBlank() && ceilingCat.id.isNotBlank()) {
                        val planArea = if (room.ceilingPlan == CeilingPlan.FULL) areas.ceiling
                        else room.partialCeilingArea.coerceIn(0.0, areas.ceiling)
                            .coerceAtMost(suggestPartialCeiling(room.area))
                        if (planArea > 0) {
                            addMaterialRow(lines, ceilingCat, room.ceiling, planArea, room, "顶面")
                            addCraftRow(lines, ceilingCat, room.ceiling, planArea, room, "顶面安装工艺")
                        }
                        if (room.ceilingPlan == CeilingPlan.PARTIAL && planArea < areas.ceiling &&
                            room.wall.categoryId == "wall-paint"
                        ) {
                            val remain = areas.ceiling - planArea
                            addMaterialRow(lines, wallCat, room.wall, remain, room, "顶面(基层)")
                            addCraftRow(lines, wallCat, room.wall, remain, room, "顶面基层工艺")
                        }
                    }
                }
            }

            // 附加项(防水/包管/拆改)归人工辅材
            room.extras.forEach { (id, ex) ->
                if (!ex.enabled) return@forEach
                val extra = catalog.spaceExtras.firstOrNull { it.id == id } ?: return@forEach
                val qty = extraQty(id, ex, room.area, state.house.ceilingHeight)
                if (qty <= 0) return@forEach
                addRow(lines, room, "人工辅材", extra.label, qty, qty * extra.unitPrice, extra.unitPrice,
                    extra.unit, Sourcing.INCLUDED, extra.desc.takeIf { it.isNotBlank() })
            }

            // 主材(门/踢脚/窗台/灯具/洁具/五金/面板)
            room.mains.forEach { sel ->
                mainItem(catalog, sel)?.let { calc ->
                    addRow(lines, room, "主材", calc.label, calc.qty, calc.cents, calc.unitPriceYuan,
                        calc.unit, calc.sourcing, calc.note)
                }
            }
        }

        // 全屋工程
        state.houseWorks.forEach { (id, input) ->
            if (!input.enabled) return@forEach
            val base = catalog.houseWorks[id] ?: return@forEach
            val price = input.priceOverride ?: base
            val total = if (id == "plumbing") state.house.totalArea * price else price.toDouble()
            addRow(lines, null, "全屋工程", houseWorkLabel(id), 1.0, total, price, "项", Sourcing.INCLUDED)
        }

        // 管理费(整装):8% ×(人工辅材 + 代购主材);自购不含
        if (state.mode == QuoteMode.FULL) {
            val rate = (state.managementRateOverride ?: catalog.managementFeeRate) / 100.0
            val labor = lines.filter { it.partLabel == PART_LABOR && it.sourcing == Sourcing.INCLUDED }
                .sumOf { it.subtotalCents } / 100.0
            val incl = materialBaseYuan(lines)
            val base = labor + incl
            if (base > 0) {
                val amount = base * rate
                addRow(lines, null, "管理费", "管理费杂费(${(rate * 100).roundToInt()}%)", 1.0, amount,
                    amount.roundToInt(), "项", Sourcing.INCLUDED,
                    "${(rate * 100).roundToInt()}% ×(主材 + 人工辅材)")
            }
        }

        return finish(lines, state.house.budgetCents)
    }

    // ═══════════════════════════ 局改 ═══════════════════════════
    private fun calcPartial(catalog: DecoboxCatalogJson, state: QuotePlanState): QuoteResult {
        val lines = mutableListOf<QuoteLine>()

        state.partialWorks.filter { it.enabled }.forEach { w ->
            val item = catalog.partialItems.firstOrNull { it.id == w.itemId } ?: return@forEach
            val tier = item.tiers.firstOrNull { it.id == w.tierId } ?: return@forEach
            when (w.itemId) {
                "wall-refresh" -> {
                    val qty = w.qtyRows.sumOf { it.area.coerceAtLeast(0.0) }
                    if (qty <= 0) return@forEach
                    val unitPrice = w.priceOverride ?: tier.unitPrice
                    addRow(lines, null, "事项", "墙面刷新(${tier.label})", qty, qty * unitPrice,
                        unitPrice, "m2", Sourcing.INCLUDED)
                    if (w.tierId == "full" && w.paintSpecId != null) {
                        paintFor(w.paintVariantId, w.paintSpecId, qty, catalog)?.let { pc ->
                            val src = if (w.paintSourcing == Sourcing.INCLUDED) Sourcing.INCLUDED else Sourcing.SELF
                            addRow(lines, null, "事项", "乳胶漆材料(刷新另计)", pc.units, pc.cost,
                                pc.unitPrice, "桶", src)
                        }
                    }
                }
                "floor-replace" -> {
                    val floorCat = catalog.floor.firstOrNull { it.id == tier.catalogCategoryId } ?: return@forEach
                    w.qtyRows.forEach { row ->
                        if (row.area <= 0) return@forEach
                        val sel = SurfaceSelection(
                            categoryId = floorCat.id,
                            variantId = floorCat.variants.firstOrNull()?.id,
                            specId = floorCat.variants.firstOrNull()?.specs?.firstOrNull()?.id,
                            craftIds = emptyList(),
                            sourcing = Sourcing.INCLUDED,
                            areaOverride = row.area,
                        )
                        addMaterialRow(lines, floorCat, sel, row.area, null, "事项(${row.roomName})")
                        val unitCraft = floorCat.craftSteps.sumOf { it.unitPrice }
                        if (unitCraft > 0) {
                            addRow(lines, null, "事项", "${floorCat.label}铺贴(${row.roomName})",
                                row.area, row.area * unitCraft, unitCraft, "m2", Sourcing.INCLUDED)
                        }
                    }
                }
            }
        }

        // 局改费用
        val pf = catalog.partialFees
        val totalQty = state.partialWorks.filter { it.enabled }
            .sumOf { w -> w.qtyRows.sumOf { it.area.coerceAtLeast(0.0) } }
        val demoQty = state.partialWorks.filter { it.enabled && it.itemId == "wall-refresh" }
            .sumOf { w -> w.qtyRows.sumOf { it.area.coerceAtLeast(0.0) } }
        val defaults = mapOf(
            "demo" to demoQty * pf.demoRatePerSqm,
            "protect" to (totalQty * pf.protectRatePerSqm).coerceAtLeast(pf.protectMin.toDouble()),
            "hauling" to pf.hauling.toDouble(),
        )
        defaults.forEach { (id, default) ->
            val input = state.partialFees[id] ?: return@forEach
            if (!input.enabled) return@forEach
            val amount = input.amountOverrideCents?.let { it / 100.0 } ?: default
            if (amount <= 0) return@forEach
            addRow(lines, null, "局改费用", partialFeeLabel(id), 1.0, amount, amount.roundToInt(),
                "项", Sourcing.INCLUDED, partialFeeDetail(id))
        }

        return finish(lines, state.house.budgetCents)
    }

    // ── 汇总从行聚合 ──
    private const val PART_LABOR = "人工辅材"
    private const val PART_HOUSE = "全屋工程"
    private const val PART_MGMT = "管理费"
    private const val PART_ITEM = "事项"
    private const val PART_FEE = "局改费用"
    private const val PART_MAIN = "主材"

    /** 表面材料行(墙面/顶面/地面等)在整装/半包中计入「代购主材」。 */
    private val SURFACE_PARTS = setOf("墙面", "地面", "顶面", "顶面(基层)")

    private fun isMaterialLine(l: QuoteLine): Boolean = l.partLabel == PART_MAIN || l.partLabel in SURFACE_PARTS

    private fun materialBaseYuan(lines: List<QuoteLine>): Double =
        lines.filter { it.sourcing == Sourcing.INCLUDED && isMaterialLine(it) }
            .sumOf { it.subtotalCents } / 100.0

    private fun finish(lines: List<QuoteLine>, budgetCents: Long): QuoteResult {
        fun partSum(p: String, includedOnly: Boolean = false): Long =
            lines.filter {
                it.partLabel == p && (!includedOnly || it.sourcing == Sourcing.INCLUDED)
            }.sumOf { it.subtotalCents }

        val labor = partSum(PART_LABOR, includedOnly = true)
        val included = lines.filter { it.sourcing == Sourcing.INCLUDED && isMaterialLine(it) }
            .sumOf { it.subtotalCents }
        val self = lines.filter { it.sourcing == Sourcing.SELF && isMaterialLine(it) }
            .sumOf { it.subtotalCents }
        val house = partSum(PART_HOUSE)
        val mgmt = partSum(PART_MGMT)
        val items = lines.filter { it.partLabel.startsWith(PART_ITEM) && it.sourcing == Sourcing.INCLUDED }
            .sumOf { it.subtotalCents }
        val fees = partSum(PART_FEE)
        val total = lines.filter { it.sourcing != Sourcing.SELF }.sumOf { it.subtotalCents }
        val remain = budgetCents - total
        val over = (-remain).coerceAtLeast(0L)
        val top3 = lines.filter { it.sourcing != Sourcing.SELF }
            .sortedByDescending { it.subtotalCents }
            .take(3)
        return QuoteResult(
            lines = lines,
            summary = QuoteSummary(
                laborAuxCents = labor,
                includedMainCents = included,
                selfMainCents = self,
                houseWorkCents = house,
                managementCents = mgmt,
                partialItemCents = items,
                partialFeeCents = fees,
            ),
            totalCents = total,
            budgetCents = budgetCents,
            budgetRemainCents = remain.coerceAtLeast(0L),
            overBudgetCents = over,
            top3 = top3,
        )
    }

    // ── 表面材料 / 工艺行 ──
    private data class MaterialQuote(
        val units: Double,
        val unitLabel: String,
        val unitPrice: Int,
        val cost: Double,
        val specLabel: String?,
    )

    private fun catOf(catalog: DecoboxCatalogJson, surface: String, categoryId: String): SurfaceCategory =
        when (surface) {
            "wall" -> catalog.wall.firstOrNull { it.id == categoryId }
            "ceiling" -> catalog.ceiling.firstOrNull { it.id == categoryId }
            else -> catalog.floor.firstOrNull { it.id == categoryId }
        } ?: SurfaceCategory("", "")

    private fun materialQuote(cat: SurfaceCategory, sel: SurfaceSelection, area: Double): MaterialQuote? {
        val variant = cat.variants.firstOrNull { it.id == sel.variantId } ?: cat.variants.firstOrNull() ?: return null
        val spec = variant.specs.firstOrNull { it.id == sel.specId } ?: variant.specs.firstOrNull() ?: return null
        val coverage = spec.coveragePerUnit
        val units = if (coverage == 1.0) area else ceil(area / coverage)
        if (units <= 0) return null
        val price = sel.priceOverrideYuan ?: spec.unitPrice
        return MaterialQuote(units, spec.unitLabel.ifBlank { "m2" }, price, units * price, spec.label)
    }

    private fun addMaterialRow(
        lines: MutableList<QuoteLine>,
        cat: SurfaceCategory,
        sel: SurfaceSelection,
        area: Double,
        room: QuoteRoomState?,
        part: String,
    ) {
        val q = materialQuote(cat, sel, area) ?: return
        addRow(lines, room, part, cat.label + (q.specLabel?.let { " · $it" } ?: ""),
            q.units, q.cost, q.unitPrice, q.unitLabel, sel.sourcing)
    }

    private fun addCraftRow(
        lines: MutableList<QuoteLine>,
        cat: SurfaceCategory,
        sel: SurfaceSelection,
        area: Double,
        room: QuoteRoomState?,
        label: String,
    ) {
        if (area <= 0) return
        val steps = if (cat.fixedCrafts) cat.craftSteps else cat.craftSteps.filter { it.id in sel.craftIds }
        if (steps.isEmpty()) return
        val unitPrice = steps.sumOf { it.unitPrice }
        addRow(lines, room, PART_LABOR, label, area, area * unitPrice, unitPrice, "m2", Sourcing.INCLUDED)
    }

    private data class PaintQuote(val units: Double, val unitPrice: Int, val cost: Double)

    private fun paintFor(variantId: String?, specId: String?, area: Double, catalog: DecoboxCatalogJson): PaintQuote? {
        val cat = catalog.wall.firstOrNull { it.id == "wall-paint" } ?: return null
        val variant = cat.variants.firstOrNull { it.id == variantId } ?: cat.variants.firstOrNull() ?: return null
        val spec = variant.specs.firstOrNull { it.id == specId } ?: variant.specs.firstOrNull() ?: return null
        val units = if (spec.coveragePerUnit == 1.0) area else ceil(area / spec.coveragePerUnit)
        return PaintQuote(units, spec.unitPrice, units * spec.unitPrice)
    }

    private fun extraQty(id: String, ex: ExtraInput, floorArea: Double, ceilingHeight: Double): Double = when (id) {
        "waterproof" -> {
            val floor = if (ex.waterproofFloorArea > 0) ex.waterproofFloorArea else floorArea
            val wall = if (ex.waterproofWallArea > 0) ex.waterproofWallArea
            else estimateAreas(floor, ceilingHeight).wall
            z(floor + wall)
        }
        "pipeWrap" -> ex.pipeWrapCount.toDouble()
        "wallDemolition" -> z(ex.wallDemolitionArea)
        else -> 0.0
    }

    // ── 主材计价 ──
    private data class MainCalc(
        val label: String,
        val qty: Double,
        val unit: String,
        val cents: Double,
        val unitPriceYuan: Int,
        val sourcing: Sourcing,
        val note: String? = null,
    )

    private fun mainItem(catalog: DecoboxCatalogJson, sel: MainSelection): MainCalc? {
        val main = catalog.otherMains.firstOrNull { it.id == sel.mainId } ?: return null
        val type = main.types.firstOrNull { it.id == sel.typeId } ?: return null
        return when (main.id) {
            "door" -> doorCalc(catalog, sel, type)
            "windowsill" -> windowsillCalc(catalog, sel, type)
            "sanitary" -> sanitaryCalc(catalog, sel)
            else -> plainSpec(catalog, sel, type, main.defaultQty)
        }
    }

    private fun unitLabelOf(spec: MaterialSpec): String = spec.unitLabel.ifBlank { "个" }

    private fun plainSpec(
        catalog: DecoboxCatalogJson,
        sel: MainSelection,
        type: com.renovation.guardian.data.knowledge.MainMaterialType,
        fallbackDefault: Int,
    ): MainCalc? {
        val spec = type.specs.firstOrNull { it.id == sel.specId } ?: return null
        val qty = sel.qtyOverride ?: (catalog.defaultQtys[spec.id] ?: fallbackDefault).toDouble()
        if (qty <= 0) return null
        val price = sel.priceOverride ?: spec.unitPrice
        return MainCalc("${type.label} · ${spec.label}", qty, unitLabelOf(spec), qty * price, price, sel.sourcing)
    }

    private fun windowsillCalc(
        catalog: DecoboxCatalogJson,
        sel: MainSelection,
        type: com.renovation.guardian.data.knowledge.MainMaterialType,
    ): MainCalc? {
        val spec = type.specs.firstOrNull { it.id == sel.specId } ?: return null
        val def = catalog.windowsillDefaults
        val qty = sel.qtyOverride ?: (def.opening + (def.earLeft + def.earRight) / 100.0)
        val edge = catalog.windowsillEdges.firstOrNull { it.id == sel.windowsillEdgeId }?.unitPrice ?: 0
        val price = spec.unitPrice + edge
        return MainCalc("${type.label} · ${spec.label}", qty, spec.unitLabel, qty * price, price, sel.sourcing)
    }

    /** 门:木门(品牌系列 + 门套 + 超规)或金属移门(m2 + 玻璃)。 */
    private fun doorCalc(
        catalog: DecoboxCatalogJson,
        sel: MainSelection,
        type: com.renovation.guardian.data.knowledge.MainMaterialType,
    ): MainCalc? {
        if (type.id == "door-metal-slide") {
            val spec = type.specs.firstOrNull { it.id == sel.specId } ?: return null
            val glass = sel.doorGlassId?.let { gid ->
                catalog.doorGlasses[spec.id]?.firstOrNull { it.id == gid }?.unitPrice ?: 0
            } ?: 0
            val price = spec.unitPrice + glass
            val qty = sel.qtyOverride ?: ((sel.doorHeightMm / 1000.0) * (sel.doorWidthMm / 1000.0))
            return MainCalc("${type.label} · ${spec.label}", qty, "m2", qty * price, price, sel.sourcing,
                note = "门洞 ${sel.doorWidthMm}×${sel.doorHeightMm}mm")
        }
        val door = catalog.doors.firstOrNull { it.id == sel.doorBrandId } ?: return null
        val series = door.series.firstOrNull { it.id == sel.specId } ?: return null
        val framePrice = series.framePrices[sel.doorFrame] ?: 0
        var extra = 0
        val notes = mutableListOf<String>()
        door.surcharge?.let { sur ->
            if (sel.doorHeightMm > sur.tubeFrom) {
                extra += sur.tubePerLeaf
                notes += "超高加方管"
            } else if (sel.doorHeightMm in (sur.tallFrom + 1)..sur.tallTo) {
                extra += sur.perLeaf
                notes += "超高门"
            }
            if (sel.doorWidthMm in (sur.wideFrom + 1)..sur.wideTo) {
                extra += sur.perLeaf
                notes += "超宽门"
            }
        }
        val perUnit = series.unitPrice + framePrice + extra
        val qty = sel.qtyOverride ?: 1.0
        val note = notes.takeIf { it.isNotEmpty() }?.joinToString(",")
        return MainCalc("${door.label} · ${series.label}", qty, "樘", qty * perUnit, perUnit, sel.sourcing, note)
    }

    /** 洁具矩阵。 */
    private fun sanitaryCalc(catalog: DecoboxCatalogJson, sel: MainSelection): MainCalc? =
        when (sel.typeId) {
            "sanitary-heater" -> {
                val brand = sel.heaterBrandId ?: return null
                val tier = sel.heaterTierId ?: return null
                val price = catalog.heaterMatrix[brand]?.get(tier) ?: return null
                val bl = catalog.heaterBrands.firstOrNull { it.id == brand }?.label ?: brand
                val tl = catalog.heaterTiers.firstOrNull { it.id == tier }?.label ?: tier
                MainCalc("风暖一体机 · $bl·$tl", 1.0, "套", price.toDouble(), price, sel.sourcing)
            }
            "sanitary-toilet" -> {
                val brand = sel.toiletBrandId ?: return null
                val kind = sel.toiletKindId ?: return null
                val price = catalog.toiletMatrix[brand]?.get(kind) ?: return null
                val bl = catalog.toiletBrands.firstOrNull { it.id == brand }?.label ?: brand
                val kl = catalog.toiletKinds.firstOrNull { it.id == kind }?.label ?: kind
                MainCalc("坐便 · $bl·$kl", 1.0, "套", price.toDouble(), price, sel.sourcing)
            }
            else -> null
        }

    // ── 行构造 ──
    private fun addRow(
        lines: MutableList<QuoteLine>,
        room: QuoteRoomState?,
        part: String,
        label: String,
        qty: Double,
        cost: Double,
        unitPriceYuan: Int,
        unit: String,
        sourcing: Sourcing,
        note: String? = null,
    ) {
        lines += line(room, part, label, qty, cost, unitPriceYuan, unit, sourcing, note)
    }

    private fun line(
        room: QuoteRoomState?,
        part: String,
        label: String,
        qty: Double,
        cost: Double,
        unitPriceYuan: Int,
        unit: String,
        sourcing: Sourcing,
        note: String? = null,
    ): QuoteLine {
        val roundedCost = round(cost)
        return QuoteLine(
            key = "${room?.id ?: "global"}|$part|$label",
            roomId = room?.id,
            roomName = room?.name,
            partLabel = part,
            label = label,
            quantityText = qtyText(qty),
            unit = unit,
            unitPriceCents = centsOf(unitPriceYuan.toDouble()),
            subtotalCents = centsOf(roundedCost),
            sourcing = sourcing,
            note = note,
        )
    }

    private fun houseWorkLabel(id: String): String = when (id) {
        "plumbing" -> "水电改造"
        "hauling" -> "垃圾清运"
        "cleaning" -> "开荒保洁"
        "protection" -> "成品保护"
        else -> id
    }

    private fun partialFeeLabel(id: String): String = when (id) {
        "demo" -> "拆旧费"
        "protect" -> "成品保护费"
        else -> "垃圾清运"
    }

    private fun partialFeeDetail(id: String): String = when (id) {
        "demo" -> "25元/m2 × 相关事项施工量"
        "protect" -> "8元/m2 × 施工量总和,最低 300"
        else -> "固定 800"
    }
}
