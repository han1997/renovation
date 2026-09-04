package com.renovation.guardian.data.repo

import com.renovation.guardian.data.db.AppDatabase
import com.renovation.guardian.data.db.BudgetCategoryEntity
import com.renovation.guardian.data.db.CategoryWithSpent
import com.renovation.guardian.data.db.ExpenseEntity
import com.renovation.guardian.data.db.HouseProfileEntity
import com.renovation.guardian.data.knowledge.KnowledgeSeeder
import com.renovation.guardian.data.knowledge.ProfileSnapshot
import com.renovation.guardian.util.MoneyUtil
import kotlinx.coroutines.flow.Flow

class HouseProfileRepository(
    private val db: AppDatabase,
    private val seeder: KnowledgeSeeder,
    private val today: () -> String,
) {
    fun observe(): Flow<HouseProfileEntity?> = db.houseProfileDao().observe()
    suspend fun get(): HouseProfileEntity? = db.houseProfileDao().get()

    suspend fun upsert(profile: HouseProfileEntity) {
        db.houseProfileDao().upsert(profile)
    }

    /**
     * 完成向导：写 profile + 应用预算模板（已有分类保留支出 + 自定义；模板分类刷新 planned）。
     */
    suspend fun finishOnboarding(
        areaM2: Double,
        tierId: String,
        modeId: String,
        gradeId: String,
        startDate: String?,
        totalBudgetYuan: Double,
        selectedPresetIds: List<String>,
        customSpaces: List<CustomSpaceInput>,
    ): HouseProfileEntity {
        val existing = db.houseProfileDao().get()
        val now = today()
        val profile = HouseProfileEntity(
            id = HouseProfileEntity.SINGLE_ROW_ID,
            areaM2 = areaM2,
            tierId = tierId,
            modeId = modeId,
            gradeId = gradeId,
            startDate = startDate,
            totalBudgetCents = MoneyUtil.fromYuan(totalBudgetYuan),
            styleId = existing?.styleId,
            styleQuizAt = existing?.styleQuizAt,
            createdAt = existing?.createdAt ?: now,
        )
        db.houseProfileDao().upsert(profile)

        // 应用预算模板
        val tplCats = seeder.buildBudgetTemplate(
            ProfileSnapshot(areaM2, tierId, modeId, gradeId),
            now,
        )
        applyBudgetTemplate(tplCats)

        return profile
    }

    private suspend fun applyBudgetTemplate(tpl: List<BudgetCategoryEntity>) {
        val existing = db.budgetCategoryDao().listAll().associateBy { it.id }
        val merged = tpl.map { tplCat ->
            val ex = existing[tplCat.id]
            if (ex != null) ex.copy(plannedCents = tplCat.plannedCents) else tplCat
        }
        val customCats = existing.values.filter { it.id.startsWith("bc_") && merged.none { it.id == it.id } }
        // 仅追加现有"非模板 / 非内置 id"的自定义分类（保留用户的 bc_xxx 等）
        val extras = existing.values.filter { ex ->
            ex.id.startsWith("bc_") && merged.none { it.id == ex.id }
        }
        db.budgetCategoryDao().upsertAll(merged + extras)
    }

    suspend fun updateStyle(styleId: String) {
        val existing = db.houseProfileDao().get() ?: return
        db.houseProfileDao().upsert(
            existing.copy(
                styleId = styleId,
                styleQuizAt = today(),
            ),
        )
    }

    suspend fun updateProfile(
        areaM2: Double,
        tierId: String,
        modeId: String,
        gradeId: String,
        startDate: String?,
    ) {
        val existing = db.houseProfileDao().get() ?: return
        db.houseProfileDao().upsert(
            existing.copy(
                areaM2 = areaM2,
                tierId = tierId,
                modeId = modeId,
                gradeId = gradeId,
                startDate = startDate,
            ),
        )
    }

    suspend fun alignTotalToCategoriesSum() {
        val cats = db.budgetCategoryDao().listAll()
        val sum = cats.sumOf { it.plannedCents }
        val existing = db.houseProfileDao().get() ?: return
        db.houseProfileDao().upsert(existing.copy(totalBudgetCents = sum))
    }

    suspend fun setTotalBudgetCents(cents: Long) {
        val existing = db.houseProfileDao().get() ?: return
        db.houseProfileDao().upsert(existing.copy(totalBudgetCents = cents))
    }
}

data class CustomSpaceInput(
    val name: String,
    val stageIds: List<String>,
    val budgetCategoryId: String?,
    val budgetNote: String?,
)