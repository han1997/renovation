package com.renovation.guardian.ui.onboarding

import android.app.Application
import com.renovation.guardian.data.knowledge.GradeJson
import com.renovation.guardian.data.knowledge.ModeJson
import com.renovation.guardian.data.knowledge.TierJson
import com.renovation.guardian.ui.AppViewModel

/**
 * 首次设置向导（3 步）：
 * 面积 / 城市 / 开工日期 → 装修方式 → 档次 + 总预算。
 */
class OnboardingViewModel(application: Application) : AppViewModel(application) {

    private val knowledge = container.knowledge

    val modes: List<ModeJson> get() = knowledge.knowledge?.modes ?: emptyList()
    val tiers: List<TierJson> get() = knowledge.prices?.tiers ?: emptyList()
    val grades: List<GradeJson> get() = knowledge.prices?.grades ?: emptyList()

    /** 根据面积 / 档次 / 方式估算推荐总预算（元）。 */
    fun suggestTotalYuan(areaM2: Double, tierId: String, modeId: String, gradeId: String): Double {
        val today = container.todayProvider()
        val cats = container.seeder.buildBudgetTemplate(
            com.renovation.guardian.data.knowledge.ProfileSnapshot(areaM2, tierId, modeId, gradeId),
            today,
        )
        return cats.sumOf { it.plannedCents } / 100.0
    }

    suspend fun finish(
        areaM2: Double,
        tierId: String,
        modeId: String,
        gradeId: String,
        startDate: String?,
        totalBudgetYuan: Double,
    ) {
        container.houseProfileRepo.finishOnboarding(
            areaM2 = areaM2,
            tierId = tierId,
            modeId = modeId,
            gradeId = gradeId,
            startDate = startDate,
            totalBudgetYuan = totalBudgetYuan,
            selectedPresetIds = emptyList(),
            customSpaces = emptyList(),
        )
    }
    suspend fun finishCents(area: Double, tier: String, mode: String, grade: String, date: String?, budget: Long) {
        container.houseProfileRepo.finishOnboardingCents(area, tier, mode, grade, date, budget, emptyList(), emptyList())
    }

}
