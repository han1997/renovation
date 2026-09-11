package han1997.renovation.domain.quote

import han1997.renovation.data.knowledge.DecoboxCatalogJson
import han1997.renovation.data.knowledge.SurfaceCategory

/** 保存、载入、恢复共用的数值与目录引用校验；草稿可为空，非法字段不能被忽略。 */
object QuoteValidation {
    fun validate(state: QuotePlanState, catalog: DecoboxCatalogJson? = null) {
        fun number(value: Double, label: String, positive: Boolean = false) {
            require(value.isFinite() && (if (positive) value > 0 else value >= 0) && value <= 100000000) { "$label 超出有效范围" }
        }
        fun surface(value: SurfaceSelection, categories: List<SurfaceCategory>?) {
            value.areaOverride?.let { number(it, "施工面积") }
            require(value.priceOverrideYuan == null || value.priceOverrideYuan >= 0) { "主材单价不能为负数" }
            if (categories != null && value.categoryId.isNotEmpty()) {
                val cat = requireNotNull(categories.firstOrNull { it.id == value.categoryId }) { "材料目录引用无效" }
                val variant = if (value.variantId == null) cat.variants.firstOrNull() else cat.variants.firstOrNull { it.id == value.variantId }
                require(variant != null) { "材料品牌引用无效" }
                require(value.specId == null || variant.specs.any { it.id == value.specId }) { "材料规格引用无效" }
                require(value.craftIds.all { id -> cat.craftSteps.any { it.id == id } }) { "工艺引用无效" }
            }
        }
        number(state.house.totalArea, "建筑面积", true); number(state.house.ceilingHeight, "层高", true)
        require(state.house.budgetCents >= 0) { "方案预算不能为负数" }
        require(state.wizardStep in 1..4) { "向导步骤无效" }
        require(state.managementRateOverride == null || state.managementRateOverride in 0..100) { "管理费率应在 0–100% 之间" }
        require(state.rooms.map { it.id }.distinct().size == state.rooms.size && state.rooms.all { it.id.isNotBlank() }) { "空间 ID 无效或重复" }
        state.rooms.forEach { room ->
            number(room.area, "空间面积")
            surface(room.wall, catalog?.wall); surface(room.floor, catalog?.floor); surface(room.ceiling, catalog?.ceiling)
            number(room.partialCeilingArea, "局部吊顶面积")
            if (room.ceilingPlan != CeilingPlan.NONE) require((room.ceiling.areaOverride ?: if (room.ceilingPlan == CeilingPlan.PARTIAL) room.partialCeilingArea else room.area) <= room.area) { "吊顶面积不能超过空间地面面积" }
            if (room.ceilingPlan == CeilingPlan.PARTIAL) require((room.ceiling.areaOverride ?: room.partialCeilingArea) <= QuoteCalculator.suggestPartialCeiling(room.area)) { "局部吊顶面积超出目录规则上限" }
            room.extras.forEach { (id, input) ->
                if (catalog != null) require(catalog.spaceExtras.any { it.id == id }) { "附加工程引用无效" }
                number(input.waterproofFloorArea, "防水地面面积"); number(input.waterproofWallArea, "防水墙面面积")
                number(input.wallDemolitionArea, "拆改面积")
                require(input.pipeWrapCount > 0 && input.waterproofCoats > 0) { "附加工程数量无效" }
            }
            room.mains.forEach { value ->
                value.qtyOverride?.let { number(it, "主材数量", true) }
                require(value.priceOverride == null || value.priceOverride >= 0) { "主材单价无效" }
                require(value.doorHeightMm > 0 && value.doorWidthMm > 0) { "门洞尺寸无效" }
                if (catalog != null) {
                    val main = requireNotNull(catalog.otherMains.firstOrNull { it.id == value.mainId }) { "主材目录引用无效" }
                    val type = requireNotNull(main.types.firstOrNull { it.id == value.typeId }) { "主材类型引用无效" }
                    when (value.typeId) {
                        "door-wood" -> {
                            val brand = requireNotNull(catalog.doors.firstOrNull { it.id == value.doorBrandId }) { "木门品牌无效" }
                            val series = requireNotNull(brand.series.firstOrNull { it.id == value.specId }) { "木门系列无效" }
                            require(series.framePrices.isEmpty() || value.doorFrame in series.framePrices) { "门套引用无效" }
                        }
                        "sanitary-heater" -> require(catalog.heaterMatrix[value.heaterBrandId]?.containsKey(value.heaterTierId) == true) { "风暖品牌或档位无效" }
                        "sanitary-toilet" -> require(catalog.toiletMatrix[value.toiletBrandId]?.containsKey(value.toiletKindId) == true) { "坐便品牌或类型无效" }
                        else -> require(type.specs.any { it.id == value.specId }) { "主材规格引用无效" }
                    }
                }
            }
        }
        state.houseWorks.forEach { (id, input) ->
            if (catalog != null) require(id in catalog.houseWorks) { "全屋工程引用无效" }
            require(input.priceOverride == null || input.priceOverride >= 0) { "全屋工程单价无效" }
        }
        state.partialWorks.forEach { work ->
            require(work.priceOverride == null || work.priceOverride >= 0) { "局改单价无效" }
            work.qtyRows.forEach { number(it.area, "局改面积") }
            surface(work.floorSelection ?: SurfaceSelection(), catalog?.floor)
            if (catalog != null) {
                val item = requireNotNull(catalog.partialItems.firstOrNull { it.id == work.itemId }) { "局改事项引用无效" }
                require(item.tiers.any { it.id == work.tierId }) { "局改档位无效" }
            }
        }
        state.partialFees.forEach { (id, fee) ->
            require(id in setOf("demo", "protect", "hauling") && (fee.amountOverrideCents == null || fee.amountOverrideCents >= 0)) { "局改费用无效" }
        }
        // 同时验证金额能完整落入 Long，禁止计算溢出后再导入。
        catalog?.let { QuoteCalculator.calculate(it, state) }
    }
}
