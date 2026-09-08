package com.renovation.guardian.data.repo

import com.renovation.guardian.data.db.AppDatabase
import com.renovation.guardian.data.db.PlannerStateEntity
import com.renovation.guardian.data.db.QuotePlanEntity
import com.renovation.guardian.data.db.BudgetCategoryEntity
import com.renovation.guardian.util.IdGen
import kotlinx.coroutines.flow.Flow

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

    suspend fun updatePlanState(id: String, stateJson: String, updatedAt: String) {
        val plan = db.quotePlanDao().getById(id) ?: return
        db.quotePlanDao().upsert(plan.copy(stateJson = stateJson, updatedAt = updatedAt))
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
    suspend fun writeQuoteToBudget(
        categoryCents: Map<String, Long>,
        totalCents: Long,
    ) {
        db.budgetCategoryDao().upsertAll(
            categoryCents.map { (id, cents) ->
                val cat = db.budgetCategoryDao().getById(id)
                if (cat != null) cat.copy(plannedCents = cents) else null
            }.filterNotNull(),
        )
        val profile = db.houseProfileDao().get() ?: return
        db.houseProfileDao().upsert(profile.copy(totalBudgetCents = totalCents))
    }

    suspend fun listBudgetCategories(): List<BudgetCategoryEntity> = db.budgetCategoryDao().listAll()
}
