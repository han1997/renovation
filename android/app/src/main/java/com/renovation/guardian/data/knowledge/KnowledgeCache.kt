package com.renovation.guardian.data.knowledge

import android.content.Context
import kotlinx.serialization.json.Json

/**
 * 只读知识数据缓存(启动时一次性读入)。
 *
 * - 从 `assets/knowledge.json` / `prices.json` / `decobox_catalog.json` /
 *   `decobox_requirements.json` 解析;
 * - 暴露给 ViewModel 用作"参考数据";
 * - 不写入 Room([KnowledgeSeeder] 负责把"目录数据"写入 Room)。
 */
class KnowledgeCache(private val context: Context) {

    @Volatile var knowledge: KnowledgeJson? = null
        private set
    @Volatile var prices: PricesJson? = null
        private set
    @Volatile var decoboxCatalog: DecoboxCatalogJson? = null
        private set
    @Volatile var decoboxRequirements: DecoboxRequirementsJson? = null
        private set

    @Synchronized
    fun load() {
        if (knowledge != null && prices != null && decoboxCatalog != null && decoboxRequirements != null) return
        val json = Json {
            ignoreUnknownKeys = true
            prettyPrint = false
            isLenient = true
        }
        knowledge = json.decodeFromString(
            KnowledgeJson.serializer(),
            context.assets.open(KNOWLEDGE_ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() },
        )
        prices = json.decodeFromString(
            PricesJson.serializer(),
            context.assets.open(PRICES_ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() },
        )
        decoboxCatalog = json.decodeFromString(
            DecoboxCatalogJson.serializer(),
            context.assets.open(DECOBOX_CATALOG_ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() },
        )
        decoboxRequirements = json.decodeFromString(
            DecoboxRequirementsJson.serializer(),
            context.assets.open(DECOBOX_REQUIREMENTS_ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() },
        )
    }

    companion object {
        const val KNOWLEDGE_ASSET = "knowledge.json"
        const val PRICES_ASSET = "prices.json"
        const val DECOBOX_CATALOG_ASSET = "decobox_catalog.json"
        const val DECOBOX_REQUIREMENTS_ASSET = "decobox_requirements.json"
    }
}
