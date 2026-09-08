package com.renovation.guardian.ui.quote.engine

import kotlinx.serialization.Serializable

/** 报价模式。 */
@Serializable
enum class QuoteMode(val label: String) {
    FULL("整装全包"),
    SEMI("半包"),
    PARTIAL("局改"),
}

/** 房屋信息。金额统一 cents(Long);面积单位 m2。 */
@Serializable
data class HouseInfo(
    val totalArea: Double = 90.0,
    val budgetCents: Long = 0L,
    val ceilingHeight: Double = 2.4,
)

/** 吊顶方案。 */
@Serializable
enum class CeilingPlan(val label: String) {
    NONE("不吊顶"),
    PARTIAL("局部吊顶"),
    FULL("全部吊顶"),
}

/** 墙面/顶面/地面某部位的选材。 */
@Serializable
data class SurfaceSelection(
    val categoryId: String = "",
    val variantId: String? = null,
    val specId: String? = null,
    /** 勾选的基层工艺(石膏找平/批腻子等);瓷砖固定自动含铺贴工艺。 */
    val craftIds: List<String> = emptyList(),
    /** 涂料/瓷砖等主材采购方式。 */
    val sourcing: Sourcing = Sourcing.SELF,
    /** 手动覆盖面积(m2),null 用估算。 */
    val areaOverride: Double? = null,
    /** 材料单价覆盖(元),null 用目录价。 */
    val priceOverrideYuan: Int? = null,
)

@Serializable
enum class Sourcing(val label: String) {
    SELF("自购"),
    INCLUDED("施工方代购"),
}

/** 空间附加项输入(防水/包管/拆改)。 */
@Serializable
data class ExtraInput(
    val enabled: Boolean = false,
    /** waterproof: 防水地面面积(m2),防水墙面面积(m2),遍数 */
    val waterproofFloorArea: Double = 0.0,
    val waterproofWallArea: Double = 0.0,
    val waterproofCoats: Int = 2,
    /** pipeWrap: 包管根数 */
    val pipeWrapCount: Int = 1,
    /** wallDemolition: 拆改面积 m2 */
    val wallDemolitionArea: Double = 0.0,
)

/** 其他主材单项选择(门/踢脚线/窗台石/灯具/洁具/五金/面板)。 */
@Serializable
data class MainSelection(
    val mainId: String = "",
    val typeId: String = "",
    val specId: String = "",
    /** door(木门):品牌 id(tata/guanzun…) */
    val doorBrandId: String? = null,
    /** door: 门套风格 single/double + 开门信息;qty 覆盖 */
    val doorFrame: String = "single",
    val doorHeightMm: Int = 2100,
    val doorWidthMm: Int = 900,
    val doorGlassId: String? = null,       // 金属移门玻璃
    val windowsillEdgeId: String? = null,  // 窗台石边型
    val windowsillWidthCm: Int = 20,       // 窗台石宽度档
    /** sanitary 矩阵 */
    val heaterBrandId: String? = null,
    val heaterTierId: String? = null,
    val toiletBrandId: String? = null,
    val toiletKindId: String? = null,
    val toiletSmartAddon: Boolean = false,
    val qtyOverride: Double? = null,
    val priceOverride: Int? = null,
    val sourcing: Sourcing = Sourcing.SELF,
)

/** 房间报价状态。 */
@Serializable
data class QuoteRoomState(
    val id: String = "",
    val name: String = "",
    /** 地面面积(m2);null 由默认面积比例给出 */
    val area: Double = 0.0,
    val wall: SurfaceSelection = SurfaceSelection(),
    val floor: SurfaceSelection = SurfaceSelection(),
    val ceilingPlan: CeilingPlan = CeilingPlan.NONE,
    val partialCeilingArea: Double = 0.0,
    val ceiling: SurfaceSelection = SurfaceSelection(),
    val extras: Map<String, ExtraInput> = emptyMap(),
    val mains: List<MainSelection> = emptyList(),
    /** 是否来自面积推荐预设(用于报价页标识) */
    val isDefault: Boolean = true,
)

/** 全屋工程(整装/半包可开关)。 */
@Serializable
data class HouseWorkInput(
    val enabled: Boolean = false,
    val priceOverride: Int? = null,
)

/** 局改事项行。 */
@Serializable
data class PartialWorkInput(
    val itemId: String = "",          // wall-refresh | floor-replace
    val tierId: String = "",
    val enabled: Boolean = false,
    val qtyRows: List<PartialQtyRow> = emptyList(),
    val paintVariantId: String? = null,   // wall-refresh full 档的乳胶漆
    val paintSpecId: String? = null,
    val paintSourcing: Sourcing = Sourcing.INCLUDED,
    val priceOverride: Int? = null,
)

@Serializable
data class PartialQtyRow(
    val roomName: String = "",
    val area: Double = 0.0,
)

/** 一次报价的完整状态(可序列化存入 Room)。 */
@Serializable
data class QuotePlanState(
    val mode: QuoteMode = QuoteMode.FULL,
    val house: HouseInfo = HouseInfo(),
    val rooms: List<QuoteRoomState> = emptyList(),
    val houseWorks: Map<String, HouseWorkInput> = emptyMap(),   // full/semi
    val partialWorks: List<PartialWorkInput> = emptyList(),     // partial
    val partialFees: Map<String, PartialFeeInput> = emptyMap(), // demo/protect/hauling
    val managementRateOverride: Int? = null,
    val wizardStep: Int = 1,
)

@Serializable
data class PartialFeeInput(
    val enabled: Boolean = false,
    val amountOverrideCents: Long? = null,
)
