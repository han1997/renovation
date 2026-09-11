package han1997.renovation.ui.quote.engine

import han1997.renovation.domain.quote.*

import han1997.renovation.data.db.BudgetCategoryEntity
import han1997.renovation.data.repo.BudgetApplyPreview
import han1997.renovation.data.repo.CategoryBudgetChange

data class QuoteBudgetBucket(val key: String, val label: String, val cents: Long, val suggestedCategory: String?)
object QuoteBudgetMapping {
    fun buckets(result: QuoteResult, categories: List<BudgetCategoryEntity>): List<QuoteBudgetBucket> {
        val ids = categories.map { it.id }.toSet()
        val contract = listOf("b-full", "b-whole").firstOrNull { it in ids }
        fun pick(vararg candidates: String) = candidates.firstOrNull { it in ids }
        val s = result.summary
        return listOf(
            QuoteBudgetBucket("labor", "人工辅材", s.laborAuxCents, contract ?: pick("b-construct")),
            QuoteBudgetBucket("included", "代购主材", s.includedMainCents, contract ?: pick("b-main")),
            QuoteBudgetBucket("self", "自购主材", s.selfMainCents, pick("b-main")),
            QuoteBudgetBucket("house", "全屋工程", s.houseWorkCents, contract ?: pick("b-misc", "b-construct")),
            QuoteBudgetBucket("management", "管理费", s.managementCents, contract ?: pick("b-misc")),
            QuoteBudgetBucket("partial", "局改事项", s.partialItemCents, contract ?: pick("b-construct")),
            QuoteBudgetBucket("fees", "局改费用", s.partialFeeCents, contract ?: pick("b-misc")),
        ).filter { it.cents > 0 }
    }
    fun preview(result: QuoteResult, categories: List<BudgetCategoryEntity>, targets: Map<String, String>): BudgetApplyPreview {
        val buckets = buckets(result, categories)
        require(buckets.isNotEmpty()) { "清单没有可应用的金额" }
        val totals = linkedMapOf<String, Long>()
        buckets.forEach { bucket ->
            val target = requireNotNull(targets[bucket.key]) { "请为${bucket.label}选择分类" }
            require(categories.any { it.id == target }) { "目标分类不存在" }
            totals[target] = Math.addExact(totals[target] ?: 0, bucket.cents)
        }
        require(totals.values.fold(0L, Math::addExact) == result.estimatedTotalCents) { "清单汇总不一致，未写入预算" }
        return BudgetApplyPreview(totals.map { (id, amount) ->
            val cat = categories.first { it.id == id }
            CategoryBudgetChange(id, cat.name, cat.plannedCents, amount)
        }, result.estimatedTotalCents)
    }
}
