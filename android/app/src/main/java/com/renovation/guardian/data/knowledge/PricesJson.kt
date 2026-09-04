package com.renovation.guardian.data.knowledge

import kotlinx.serialization.Serializable

/**
 * `assets/prices.json` 顶层结构。镜像 Web `js/data/prices.js` 的 `window.PRICES`
 * 数据字段（剔除函数）。
 *
 * 加 `version` 字段以备后续 schema 升级。
 */
@Serializable
data class PricesJson(
    val version: Int = 1,
    val reserveRatio: Double = 0.08,
    val tiers: List<TierJson>,
    val grades: List<GradeJson>,
    val rates: List<RateJson>,
    val reference: List<ReferenceGroupJson>,
)

@Serializable
data class TierJson(
    val id: String,
    val name: String,
    val factor: Double,
)

@Serializable
data class GradeJson(
    val id: String,
    val name: String,
    val emoji: String,
    val desc: String,
)

@Serializable
data class RateJson(
    val id: String,
    val name: String,
    val emoji: String,
    val eco: Int,
    val mid: Int,
    val high: Int,
)

@Serializable
data class ReferenceGroupJson(
    val group: String,
    val emoji: String,
    val items: List<ReferenceItemJson>,
)

@Serializable
data class ReferenceItemJson(
    val name: String,
    val price: String,
    val note: String? = null,
)