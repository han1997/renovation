package han1997.renovation.data.knowledge

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `assets/decobox_requirements.json` 顶层结构(提取自 decobox.online v1.4.0 bundle)。
 *
 * - `spaceList`:10 个预设空间
 * - `typeListBySpace`:每个空间下的需求类型列表(共 150)
 * - `functionsByType`:每个需求类型下的叶子需求项(共 819,816 唯一)
 *
 * 键均为 snake/camel 混合取自站点 bundle 内部 key,保持原样以便追溯。
 */
@Serializable
data class DecoboxRequirementsJson(
    val version: String = "",
    val source: String = "",
    @SerialName("spaceList") val spaceList: List<SpaceJson> = emptyList(),
    @SerialName("typeListBySpace")
    val typeListBySpace: Map<String, List<RequirementTypeJson>> = emptyMap(),
    @SerialName("functionsByType")
    val functionsByType: Map<String, List<RequirementItemJson>> = emptyMap(),
)

@Serializable
data class SpaceJson(
    val key: String,
    val name: String,
)

/** 需求类型(如 玄关 的「(功能)进出门转换」)。name 可带 `(功能)/(收纳)/(设备)` 分类前缀。 */
@Serializable
data class RequirementTypeJson(
    val key: String,
    val name: String,
)

/** 叶子需求项(如「坐下换鞋」)。 */
@Serializable
data class RequirementItemJson(
    val key: String,
    val name: String,
)
