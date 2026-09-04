package com.renovation.guardian.ui.stages

import com.renovation.guardian.data.knowledge.BuyItemJson
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** 解析 `stage.warnings_json` / `buy_json` / `accept_ids_json` 列。 */
object StageJsonParse {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun warnings(jsonStr: String): List<String> =
        runCatching { json.decodeFromString(ListSerializer(String.serializer()), jsonStr) }
            .getOrDefault(emptyList())

    fun buy(jsonStr: String): List<BuyItemJson> =
        runCatching { json.decodeFromString(ListSerializer(BuyItemJson.serializer()), jsonStr) }
            .getOrDefault(emptyList())

    fun acceptIds(jsonStr: String): List<String> =
        runCatching { json.decodeFromString(ListSerializer(String.serializer()), jsonStr) }
            .getOrDefault(emptyList())
}
