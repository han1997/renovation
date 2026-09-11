package han1997.renovation.data.knowledge

import kotlinx.serialization.json.Json

/**
 * 报价/规划方案状态 JSON 编解码配置(状态以 JSON blob 存 Room,结构演进不受 schema 约束)。
 */
object DecoboxStateJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }
}
