package com.renovation.guardian.ui.home

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.renovation.guardian.data.db.StageTaskView
import com.renovation.guardian.ui.AppViewModel
import com.renovation.guardian.util.DateUtil
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val totalPlannedCents: Long = 0L,
    val totalSpentCents: Long = 0L,
    val totalOverCents: Long = 0L,
    val overspentCount: Int = 0,
    val currentStageName: String? = null,
    val currentStageEmoji: String? = null,
    val overallPct: Int = 0,
    val totalTasks: Int = 0,
    val doneTasks: Int = 0,
)

class HomeViewModel(application: Application) : AppViewModel(application) {

    private val today: String get() = container.todayProvider()

    val overdueTasks = container.taskRepo.observeBetween(
        "1970-01-01",
        DateUtil.minusDays(today, 1),
    )
    val todayTasks = container.taskRepo.observeBetween(today, today)
    val upcomingTasks = container.taskRepo.observeBetween(
        DateUtil.plusDays(today, 1),
        DateUtil.plusDays(today, 7),
    )

    private val profileFlow = container.houseProfileRepo.observe()
    private val categoriesFlow = container.budgetRepo.observeCategoriesWithSpent()
    private val progressFlow = container.taskRepo.observeAllProgress()
    private val stagesFlow = container.stageRepo.observeAll()

    val uiState: StateFlow<HomeUiState> = combine(
        profileFlow,
        categoriesFlow,
        progressFlow,
        stagesFlow,
    ) { profile, cats, progress, stages ->
        val totalPlanned = profile?.totalBudgetCents ?: 0L
        val totalSpent = cats.sumOf { it.spentCents }
        val totalOver = cats.sumOf { it.overCents }
        val overspentCount = cats.count { it.overCents > 0 }
        val totalTasks = progress.sumOf { it.total }
        val doneTasks = progress.sumOf { it.done }
        val overallPct = if (totalTasks == 0) 0 else (doneTasks * 100 / totalTasks)
        val current = progress.firstOrNull { !it.isDone }
        val currentStage = current?.let { st -> stages.firstOrNull { it.id == st.stageId } }
        HomeUiState(
            totalPlannedCents = totalPlanned,
            totalSpentCents = totalSpent,
            totalOverCents = totalOver,
            overspentCount = overspentCount,
            currentStageName = currentStage?.name,
            currentStageEmoji = currentStage?.emoji,
            overallPct = overallPct,
            totalTasks = totalTasks,
            doneTasks = doneTasks,
        )
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(),
    )

    fun toggleDone(v: StageTaskView) {
        viewModelScope.launch {
            val t = today
            if (v.source == "TEMPLATE") {
                container.taskRepo.setTemplateDone(v.id, !v.done, t)
            } else {
                container.taskRepo.setCustomTaskDone(v.id, !v.done, t)
            }
        }
    }
}
