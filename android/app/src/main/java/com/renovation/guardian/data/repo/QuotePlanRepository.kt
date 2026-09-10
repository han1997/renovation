package com.renovation.guardian.data.repo

import com.renovation.guardian.data.db.AppDatabase
import com.renovation.guardian.data.db.PlannerStateEntity
import com.renovation.guardian.data.db.QuotePlanEntity
import com.renovation.guardian.data.db.BudgetCategoryEntity
import com.renovation.guardian.util.IdGen
import kotlinx.coroutines.flow.Flow
import androidx.room.withTransaction

/**
 * decobox 逐空间报价方案 + 需求规划状态的持久化(JSON blob 存 Room,见 PRD ADR)。
 */
class QuotePlanRepository(private val db: AppDatabase) {

    fun observePlans(mode: String): Flow<List<QuotePlanEntity>> = db.quotePlanDao().observeByMode(mode)

    fun observeAllPlans(): Flow<List<QuotePlanEntity>> = db.quotePlanDao().observeAll()

    suspend fun getPlan(id: String): QuotePlanEntity? = db.quotePlanDao().getById(id)

    suspend fun savePlan(name: String, mode: String, stateJson: String, today: String): QuotePlanEntity {
        val now = today
        val plan = QuotePlanEntity(
            id = IdGen.new("qp"),
            name = name,
            mode = mode,
            stateJson = stateJson,
            createdAt = now,
            updatedAt = now,
        )
        db.quotePlanDao().upsert(plan)
        return plan
    }

    suspend fun updatePlanState(id: String, stateJson: String, updatedAt: String, name: String? = null, mode: String? = null) {
        val plan = requireNotNull(db.quotePlanDao().getById(id)) { "方案已不存在" }
        db.quotePlanDao().upsert(plan.copy(stateJson = stateJson, updatedAt = updatedAt, name = name ?: plan.name, mode = mode ?: plan.mode))
    }

    suspend fun deletePlan(id: String) {
        db.quotePlanDao().deleteById(id)
    }

    // ---- 需求规划状态(单行) ----

    suspend fun getPlannerState(): PlannerStateEntity? = db.plannerStateDao().get()

    suspend fun savePlannerState(stateJson: String, updatedAt: String) {
        db.plannerStateDao().upsert(
            PlannerStateEntity(
                id = PlannerStateEntity.SINGLE_ROW_ID,
                stateJson = stateJson,
                updatedAt = updatedAt,
            ),
        )
    }

    /**
     * 把报价结果写入预算:
     * - 按大项(人工辅材 / 代购主材 / 管理费 / 全屋工程)累计,
     * - 覆盖 `budget_category.planned_cents`,总预算同步为清单总价;
     * - 已有支出不动。
     */
    suspend fun applyBudgetPreview(preview: BudgetApplyPreview) = db.withTransaction {
        require(preview.changes.isNotEmpty()) { "请先选择目标分类" }
        require(preview.changes.map { it.categoryId }.distinct().size == preview.changes.size)
        val total = preview.changes.fold(0L) { sum, c -> Math.addExact(sum, c.afterCents) }
        require(total == preview.estimatedTotalCents) { "分类金额与清单合计不一致" }
        val updates = preview.changes.map { change ->
            require(change.afterCents >= 0)
            val category = requireNotNull(db.budgetCategoryDao().getById(change.categoryId)) { "分类已被删除，请重新预览" }
            check(category.plannedCents == change.beforeCents || category.plannedCents == change.afterCents) { "预算已变化，请重新预览" }
            category.copy(plannedCents = change.afterCents)
        }
        val updatedIds = updates.map { it.id }.toSet()
        (db.budgetCategoryDao().listAll().filterNot { it.id in updatedIds } + updates).fold(0L) { sum, c -> Math.addExact(sum, c.plannedCents) }
        db.budgetCategoryDao().upsertAll(updates)
        // 房屋上限与历史支出绝不在此修改。
    }

    suspend fun renamePlan(id: String, name: String, today: String) {
        require(name.isNotBlank()) { "请填写方案名称" }
        val plan = requireNotNull(db.quotePlanDao().getById(id)) { "方案已不存在" }
        db.quotePlanDao().upsert(plan.copy(name = name.trim(), updatedAt = today))
    }

    suspend fun listBudgetCategories(): List<BudgetCategoryEntity> = db.budgetCategoryDao().listAll()
}
