package han1997.renovation.ui.home

import android.app.Application
import androidx.lifecycle.viewModelScope
import han1997.renovation.data.db.StageTaskView
import han1997.renovation.ui.AppViewModel
import han1997.renovation.util.DateUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    val currentStageId: String? = null,
    val categoryPlannedCents: Long = 0L,
    val totalTasks: Int = 0,
    val doneTasks: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AppViewModel(application) {

    private val today: String get() = container.todayProvider()

    private val date = MutableStateFlow(today)
    fun refreshDate() { date.value = today }
    val overdueTasks = date.flatMapLatest { container.taskRepo.observeBetween("0001-01-01", DateUtil.minusDays(it, 1)) }
    val todayTasks = date.flatMapLatest { container.taskRepo.observeBetween(it, it) }
    val upcomingTasks = date.flatMapLatest { container.taskRepo.observeBetween(DateUtil.plusDays(it, 1), DateUtil.plusDays(it, 7)) }

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
        val totalOver = (totalSpent - totalPlanned).coerceAtLeast(0L)
        val overspentCount = cats.count { it.overCents > 0 }
        val totalTasks = progress.sumOf { it.total }
        val doneTasks = progress.sumOf { it.done }
        val overallPct = if (totalTasks == 0) 0 else (doneTasks * 100 / totalTasks)
        val current = progress.firstOrNull { it.total > 0 && !it.isDone }
        val currentStage = current?.let { st -> stages.firstOrNull { it.id == st.stageId } }
        HomeUiState(
            totalPlannedCents = totalPlanned,
            currentStageId = currentStage?.id,
            categoryPlannedCents = cats.sumOf { it.plannedCents },
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
        perform(success = null) {
            val t = today
            if (v.source == "TEMPLATE") {
                container.taskRepo.setTemplateDone(v.id, !v.done, t)
            } else {
                container.taskRepo.setCustomTaskDone(v.id, !v.done, t)
            }
        }
    }
}
