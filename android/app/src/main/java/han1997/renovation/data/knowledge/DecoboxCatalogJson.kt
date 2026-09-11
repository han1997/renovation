package han1997.renovation.data.knowledge

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `assets/decobox_catalog.json` 顶层结构(提取自 decobox.online v1.4.0 bundle)。
 *
 * 字段与提取脚本 `tools/extract_decobox.py` 的输出键严格一致(camelCase)。
 * 价格单位均为「元」(计算引擎统一换算为 cents 后运算)。
 */
@Serializable
data class DecoboxCatalogJson(
    val version: String = "",
    val source: String = "",
    val craft: CraftParams = CraftParams(),
    val areas: AreaParams = AreaParams(),
    @SerialName("managementFeeRate") val managementFeeRate: Int = 8,
    @SerialName("defaultHouse") val defaultHouse: DefaultHouse = DefaultHouse(),
    @SerialName("roomPresets") val roomPresets: List<RoomPresetBucket> = emptyList(),
    val wall: List<SurfaceCategory> = emptyList(),
    val ceiling: List<SurfaceCategory> = emptyList(),
    val floor: List<SurfaceCategory> = emptyList(),
    @SerialName("spaceExtras") val spaceExtras: List<SpaceExtra> = emptyList(),
    @SerialName("otherMains") val otherMains: List<MainMaterial> = emptyList(),
    @SerialName("houseWorks") val houseWorks: Map<String, Int> = emptyMap(),
    @SerialName("partialItems") val partialItems: List<PartialItem> = emptyList(),
    @SerialName("partialFees") val partialFees: PartialFees = PartialFees(),
    val doors: List<DoorBrand> = emptyList(),
    @SerialName("doorGlasses") val doorGlasses: Map<String, List<GlassOption>> = emptyMap(),
    @SerialName("windowsillEdges") val windowsillEdges: List<WindowsillEdge> = emptyList(),
    @SerialName("windowsillDefaults") val windowsillDefaults: WindowsillDefaults = WindowsillDefaults(),
    @SerialName("heaterBrands") val heaterBrands: List<IdLabel> = emptyList(),
    @SerialName("heaterTiers") val heaterTiers: List<IdLabel> = emptyList(),
    @SerialName("heaterMatrix") val heaterMatrix: Map<String, Map<String, Int>> = emptyMap(),
    @SerialName("toiletBrands") val toiletBrands: List<IdLabel> = emptyList(),
    @SerialName("toiletAddonPrices") val toiletAddonPrices: List<Int> = emptyList(),
    @SerialName("toiletKinds") val toiletKinds: List<IdLabel> = emptyList(),
    @SerialName("toiletMatrix") val toiletMatrix: Map<String, Map<String, Int>> = emptyMap(),
    @SerialName("multiPickIds") val multiPickIds: List<String> = emptyList(),
    @SerialName("defaultQtys") val defaultQtys: Map<String, Int> = emptyMap(),
    @SerialName("houseExtras") val houseExtras: List<IdLabel> = emptyList(),
    @SerialName("extraWorks") val extraWorks: List<ExtraWork> = emptyList(),
)

@Serializable
data class CraftParams(
    @SerialName("gypsumLevel") val gypsumLevel: Int = 15,
    val grout: Int = 20,
)

@Serializable
data class AreaParams(
    @SerialName("wallFactor") val wallFactor: Int = 4,
    @SerialName("partialCeilingFactor") val partialCeilingFactor: Double = 0.6,
    @SerialName("partialCeilingMin") val partialCeilingMin: Int = 3,
)

@Serializable
data class DefaultHouse(
    @SerialName("totalArea") val totalArea: Double = 90.0,
    val budget: Double = 120000.0,
    @SerialName("ceilingHeight") val ceilingHeight: Double = 2.4,
)

/** 按总建面分档的房间推荐(maxArea = null 表示 ≥130)。 */
@Serializable
data class RoomPresetBucket(
    @SerialName("maxArea") val maxArea: Int? = null,
    val rooms: List<RoomPreset> = emptyList(),
)

@Serializable
data class RoomPreset(
    @SerialName("roomName") val roomName: String,
    val ratio: Double,
)

/** 墙面 / 顶面 / 地面目录条目(墙顶地共用)。 */
@Serializable
data class SurfaceCategory(
    val id: String,
    val label: String,
    val variants: List<MaterialVariant> = emptyList(),
    @SerialName("craftSteps") val craftSteps: List<CraftStep> = emptyList(),
    @SerialName("fixedCrafts") val fixedCrafts: Boolean = false,
    @SerialName("includesSkirting") val includesSkirting: Boolean = false,
)

@Serializable
data class MaterialVariant(
    val id: String,
    val brand: String = "",
    val series: String = "",
    val specs: List<MaterialSpec> = emptyList(),
)

@Serializable
data class MaterialSpec(
    val id: String,
    val label: String = "",
    @SerialName("unitLabel") val unitLabel: String = "",
    @SerialName("unitPrice") val unitPrice: Int = 0,
    @SerialName("coveragePerUnit") val coveragePerUnit: Double = 1.0,
)

@Serializable
data class CraftStep(
    val id: String,
    val label: String,
    @SerialName("unitPrice") val unitPrice: Int,
)

/** 空间附加项(防水 / 包管 / 拆改)。 */
@Serializable
data class SpaceExtra(
    val id: String,
    val label: String,
    val basis: String,          // "area" | "count"
    val unit: String,
    @SerialName("unitPrice") val unitPrice: Int,
    val desc: String = "",
)

/** 其他主材(门 / 踢脚线 / 窗台石 / 灯具 / 洁具 / 五金 / 面板)。 */
@Serializable
data class MainMaterial(
    val id: String,
    val label: String,
    @SerialName("defaultQty") val defaultQty: Int = 1,
    @SerialName("suggestQtyByPerimeter") val suggestQtyByPerimeter: Boolean = false,
    @SerialName("qtyDeduction") val qtyDeduction: Double = 0.0,
    val types: List<MainMaterialType> = emptyList(),
)

@Serializable
data class MainMaterialType(
    val id: String,
    val label: String,
    val specs: List<MaterialSpec> = emptyList(),
)

@Serializable
data class PartialFees(
    @SerialName("demoRatePerSqm") val demoRatePerSqm: Int = 25,
    @SerialName("protectRatePerSqm") val protectRatePerSqm: Int = 8,
    @SerialName("protectMin") val protectMin: Int = 300,
    val hauling: Int = 800,
)

/** 局改事项(墙面刷新两档 / 地面更换套地面目录)。 */
@Serializable
data class PartialItem(
    val id: String,
    val label: String,
    val unit: String = "m2",
    @SerialName("integerQty") val integerQty: Boolean = false,
    @SerialName("needsArea") val needsArea: Boolean = false,
    val tiers: List<PartialTier> = emptyList(),
)

@Serializable
data class PartialTier(
    val id: String,
    val label: String,
    @SerialName("unitPrice") val unitPrice: Int = 0,
    val note: String = "",
    @SerialName("catalogCategoryId") val catalogCategoryId: String? = null,
)

/** 木门品牌。 */
@Serializable
data class DoorBrand(
    val id: String,
    val label: String,
    @SerialName("standardOpening") val standardOpening: OpeningSize = OpeningSize(),
    @SerialName("customOpening") val customOpening: Boolean = false,
    val surcharge: DoorSurcharge? = null,
    val series: List<DoorSeries> = emptyList(),
)

@Serializable
data class OpeningSize(
    val height: Int = 2100,
    val width: Int = 900,
    @SerialName("wallThickness") val wallThickness: Int = 240,
)

@Serializable
data class DoorSurcharge(
    @SerialName("tallFrom") val tallFrom: Int = 0,
    @SerialName("tallTo") val tallTo: Int = 0,
    @SerialName("wideFrom") val wideFrom: Int = 0,
    @SerialName("wideTo") val wideTo: Int = 0,
    @SerialName("perLeaf") val perLeaf: Int = 0,
    @SerialName("tubeFrom") val tubeFrom: Int = 0,
    @SerialName("tubePerLeaf") val tubePerLeaf: Int = 0,
)

@Serializable
data class DoorSeries(
    val id: String,
    val label: String,
    @SerialName("unitPrice") val unitPrice: Int = 0,
    @SerialName("framePrices") val framePrices: Map<String, Int> = emptyMap(),
)

/** 金属移门玻璃选项。 */
@Serializable
data class GlassOption(
    val id: String,
    val label: String,
    @SerialName("unitPrice") val unitPrice: Int = 0,
)

/** 窗台石边缘(直边/圆边/法国边)。 */
@Serializable
data class WindowsillEdge(
    val id: String,
    val label: String,
    @SerialName("unitPrice") val unitPrice: Int = 0,
)

@Serializable
data class WindowsillDefaults(
    val opening: Double = 1.5,
    @SerialName("earLeft") val earLeft: Int = 3,
    @SerialName("earRight") val earRight: Int = 3,
)

/** 通用 id + 文案条目(品牌/档位等)。 */
@Serializable
data class IdLabel(
    val id: String,
    val label: String,
)

/** 空间扩展工程(灯槽 / 中央空调 / 窗帘盒)。 */
@Serializable
data class ExtraWork(
    val id: String,
    val label: String,
    val items: List<ExtraWorkItem> = emptyList(),
)

@Serializable
data class ExtraWorkItem(
    val id: String,
    val label: String,
    val unit: String = "",
    @SerialName("unitPrice") val unitPrice: Int = 0,
    @SerialName("defaultQty") val defaultQty: Int = 1,
)
