package com.renovation.guardian.ui.home

import com.renovation.guardian.RenovationApp
import com.renovation.guardian.data.db.StageTaskView
import com.renovation.guardian.data.repo.AppContainer
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeViewModelTest {

    private fun makeVm(): HomeViewModel {
        val app = mockk<RenovationApp>(relaxed = true)
        val container = mockk<AppContainer>(relaxed = true)
        every { app.container } returns container
        every { container.todayProvider } returns { "2026-09-01" }
        every { container.houseProfileRepo.observe() } returns flowOf(null)
        every { container.budgetRepo.observeCategoriesWithSpent() } returns flowOf(emptyList())
        every { container.taskRepo.observeAllProgress() } returns flowOf(emptyList())
        every { container.stageRepo.observeAll() } returns flowOf(emptyList())
        every { container.taskRepo.observeBetween("0001-01-01", "2026-08-31") } returns flowOf(
            listOf(StageTaskView("t1", "s1", "TEMPLATE", "逾期任务", null, null, "2020-01-01", false)),
        )
        every { container.taskRepo.observeBetween("2026-09-01", "2026-09-01") } returns flowOf(
            listOf(StageTaskView("t2", "s1", "TEMPLATE", "今天任务", null, null, "2026-09-01", false)),
        )
        every { container.taskRepo.observeBetween("2026-09-02", "2026-09-08") } returns flowOf(emptyList())
        return HomeViewModel(app)
    }

    @Test
    fun overdue_grouping_containsPastDue() = runTest {
        val vm = makeVm()
        vm.overdueTasks.take(1).collect { list ->
            assertEquals(1, list.size)
            assertEquals("t1", list.first().id)
        }
    }

    @Test
    fun today_grouping_containsTodayDue() = runTest {
        val vm = makeVm()
        vm.todayTasks.take(1).collect { list ->
            assertEquals(1, list.size)
            assertEquals("t2", list.first().id)
        }
    }
}
