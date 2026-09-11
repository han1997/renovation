package han1997.renovation.data.knowledge

import androidx.room.withTransaction
import han1997.renovation.data.db.AppDatabase
import han1997.renovation.data.db.BudgetCategoryEntity
import han1997.renovation.data.db.ChecklistEntity
import han1997.renovation.data.db.ChecklistItemEntity
import han1997.renovation.data.db.StageEntity
import han1997.renovation.data.db.TaskTemplateEntity
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * 把 `assets/knowledge.json` / `prices.json` 中的"目录数据"写入 Room。
 *
 * - 仅在数据库为空时执行；
 * - 写入 stage / task_template / checklist / checklist_item；
 * - 写入 budget_category 时按 [profile] 计算各档位预算模板。
 */
class KnowledgeSeeder(
    private val db: AppDatabase,
    private val cache: KnowledgeCache,
) {

    suspend fun seedIfEmpty(today: String) {
        cache.load()
        val knowledge = cache.knowledge ?: return
        db.withTransaction {
            seedStages(knowledge)
            seedChecklists(knowledge)
        }
    }

    private suspend fun seedStages(knowledge: KnowledgeJson) {
        if (db.stageDao().count() > 0) return // 用户主动删空任务不能触发重新播种。
        val stages = knowledge.stages.mapIndexed { idx, st ->
            StageEntity(
                id = st.id,
                phase = st.phase,
                emoji = st.emoji,
                name = st.name,
                duration = st.duration,
                goal = st.goal,
                orderIndex = idx,
                warningsJson = Json.encodeToString(ListSerializer(String.serializer()), st.warnings),
                buyJson = Json.encodeToString(
                    ListSerializer(BuyItemJson.serializer()),
                    st.buy,
                ),
                acceptIdsJson = Json.encodeToString(ListSerializer(String.serializer()), st.acceptIds),
            )
        }
        val templates = knowledge.stages.flatMap { st ->
            st.tasks.mapIndexed { idx, t ->
                TaskTemplateEntity(
                    id = t.id,
                    stageId = st.id,
                    orderIndex = idx,
                    text = t.text,
                    tip = t.tip,
                )
            }
        }
        db.stageDao().seedAll(stages)
        db.taskTemplateDao().seedAll(templates)
    }

    suspend fun restoreTaskTemplates() {
        cache.load()
        val knowledge = requireNotNull(cache.knowledge)
        db.taskTemplateDao().clear()
        db.taskTemplateDao().seedAll(knowledge.stages.flatMap { stage ->
            stage.tasks.mapIndexed { index, task -> TaskTemplateEntity(task.id, stage.id, index, task.text, task.tip) }
        })
    }

    private suspend fun seedChecklists(knowledge: KnowledgeJson) {
        if (db.checklistDao().count() > 0) return
        val checklists = knowledge.checklists.map { ChecklistEntity(it.id, it.emoji, it.name, it.note) }
        val items = knowledge.checklists.flatMap { cl ->
            cl.items.mapIndexed { idx, it ->
                ChecklistItemEntity(it.id, cl.id, idx, it.text)
            }
        }
        db.checklistDao().seedChecklists(checklists)
        db.checklistDao().seedItems(items)
    }

    /**
     * 用当前房屋信息生成分类预算模板；用于向导完成 / "按推荐比例重算"。
     *
     * - 不直接写入 Room；返回列表由调用方（[han1997.renovation.data.repo.HouseProfileRepository]）负责 upsert。
     */
    fun buildBudgetTemplate(profile: ProfileSnapshot, today: String): List<BudgetCategoryEntity> {
        val prices = cache.prices ?: return emptyList()
        val gradeKey = profile.gradeId
        val tierFactor = prices.tiers.firstOrNull { it.id == profile.tierId }?.factor ?: 1.0
        val area = profile.areaM2.coerceAtLeast(15.0)
        val base = prices.rates.associate { r ->
            r.id to round100(((gradeValueOf(r, gradeKey)) * area * tierFactor).toLong())
        }
        val cats = mutableListOf<BudgetCategoryEntity>()
        var order = 0
        fun add(id: String, name: String, emoji: String, planned: Long) {
            cats += BudgetCategoryEntity(
                id = id,
                name = name,
                emoji = emoji,
                plannedCents = planned,
                orderIndex = order++,
                isFromTemplate = true,
                createdAt = today,
            )
        }
        when (profile.modeId) {
            "full" -> add("b-full", "全包合同价（施工+主材）", "📦", (base["b-construct"] ?: 0L) + (base["b-main"] ?: 0L))
            "whole" -> add("b-whole", "整装合同价（施工+主材+定制）", "🎁", (base["b-construct"] ?: 0L) + (base["b-main"] ?: 0L) + (base["b-custom"] ?: 0L))
            "clear" -> {
                add("b-labor", "人工费", "👷", round100(((base["b-construct"] ?: 0L) * 0.5).toLong()))
                add("b-aux", "辅材（水泥沙子腻子等）", "🪣", round100(((base["b-construct"] ?: 0L) * 0.45).toLong()))
                add("b-main", "主材建材", "🧱", base["b-main"] ?: 0L)
            }
            else -> {
                add("b-construct", "基础施工（人工+辅材）", "🧰", base["b-construct"] ?: 0L)
                add("b-main", "主材建材", "🧱", base["b-main"] ?: 0L)
            }
        }
        if (profile.modeId != "whole") {
            add("b-custom", "定制柜（橱柜+衣柜）", "🗄️", base["b-custom"] ?: 0L)
        }
        add("b-window", "门窗封阳台", "🪟", base["b-window"] ?: 0L)
        add("b-appliance", "家电", "📺", base["b-appliance"] ?: 0L)
        add("b-furniture", "家具", "🛏️", base["b-furniture"] ?: 0L)
        add("b-soft", "软装布艺（窗帘灯饰）", "🪴", base["b-soft"] ?: 0L)
        add("b-misc", "杂项（清运/保洁/美缝等）", "🧾", base["b-misc"] ?: 0L)
        val subtotal = cats.sumOf { it.plannedCents }
        add("b-reserve", "备用金（应急）", "🧯", round100((subtotal * (prices.reserveRatio)).toLong()))
        return cats
    }

    private fun gradeValueOf(rate: han1997.renovation.data.knowledge.RateJson, gradeKey: String): Int = when (gradeKey) {
        "eco" -> rate.eco
        "high" -> rate.high
        else -> rate.mid
    }

    private fun round100(n: Long): Long = ((n + 50) / 100) * 100
}

/** 向导或编辑模式下的"房屋信息快照"。 */
data class ProfileSnapshot(
    val areaM2: Double,
    val tierId: String,
    val modeId: String,
    val gradeId: String,
)