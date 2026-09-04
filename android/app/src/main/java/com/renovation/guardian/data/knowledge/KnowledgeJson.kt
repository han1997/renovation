package com.renovation.guardian.data.knowledge

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `assets/knowledge.json` 顶层结构。镜像 Web `js/data/knowledge.js` 的 `window.DATA`
 * 字段（仅保留数据，剔除 IIFE / 函数）。
 *
 * 加 `version` 字段以备后续 schema 升级时做兼容性判断。
 */
@Serializable
data class KnowledgeJson(
    val version: Int = 1,
    val totalDurationNote: String,
    val stages: List<StageJson>,
    val checklists: List<ChecklistJson>,
    val tipTopics: List<String>,
    val tips: List<TipJson>,
    val acceptIntro: String,
    val styles: List<StyleJson>,
    val styleQuiz: StyleQuizJson,
    val materialTimeline: List<MaterialTimelineJson>,
    val modes: List<ModeJson>,
    val whoBuilds: List<WhoBuildsJson>,
    val glossary: List<GlossaryJson>,
    val spaceNeeds: List<SpaceNeedJson>,
)

@Serializable
data class StageJson(
    val id: String,
    val phase: String,
    val emoji: String,
    val name: String,
    val duration: String,
    val goal: String,
    val tasks: List<TaskTemplateJson>,
    val warnings: List<String>,
    val buy: List<BuyItemJson>,
    val acceptIds: List<String>,
)

@Serializable
data class TaskTemplateJson(
    val id: String,
    val text: String,
    val tip: String? = null,
)

@Serializable
data class BuyItemJson(
    val item: String,
    val note: String? = null,
)

@Serializable
data class ChecklistJson(
    val id: String,
    val emoji: String,
    val name: String,
    val note: String? = null,
    val items: List<ChecklistItemJson>,
)

@Serializable
data class ChecklistItemJson(
    val id: String,
    val text: String,
)

@Serializable
data class TipJson(
    val title: String,
    val level: String,
    val topic: String,
    val body: String,
)

@Serializable
data class StyleJson(
    val id: String,
    val emoji: String,
    val name: String,
    val tagline: String,
    val colors: List<String>,
    val cost: String,
    val costNote: String,
    val desc: String,
    val fit: String,
    val elements: List<String>,
    val pitfalls: List<String>,
)

@Serializable
data class StyleQuizJson(
    val questions: List<StyleQuizQuestionJson>,
)

@Serializable
data class StyleQuizQuestionJson(
    val q: String,
    val options: List<StyleQuizOptionJson>,
)

@Serializable
data class StyleQuizOptionJson(
    val text: String,
    val scores: Map<String, Int>,
)

@Serializable
data class MaterialTimelineJson(
    val emoji: String,
    val period: String,
    @SerialName("when") val whenText: String,
    val note: String? = null,
    val items: List<MaterialItemJson>,
)

@Serializable
data class MaterialItemJson(
    val name: String,
    val note: String? = null,
    val lead: String,
)

@Serializable
data class ModeJson(
    val id: String,
    val emoji: String,
    val name: String,
    val priceShort: String,
    val short: String,
    val desc: String,
    val pros: List<String>,
    val cons: List<String>,
    val fit: String,
)

@Serializable
data class WhoBuildsJson(
    val emoji: String,
    val name: String,
    val pros: List<String>,
    val cons: List<String>,
    val fit: String,
)

@Serializable
data class GlossaryJson(
    val term: String,
    val def: String,
)

@Serializable
data class SpaceNeedJson(
    val id: String,
    val emoji: String,
    val name: String,
    val desc: String,
    val stageIds: List<String>,
    val tasks: List<SpaceNeedTaskJson>,
    val budgetCat: String,
    val budgetNote: String,
)

@Serializable
data class SpaceNeedTaskJson(
    val stageId: String,
    val text: String,
)