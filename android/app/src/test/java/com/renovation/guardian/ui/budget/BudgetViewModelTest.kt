package com.renovation.guardian.ui.budget

import com.renovation.guardian.RenovationApp
import com.renovation.guardian.data.db.CategoryWithSpent
import com.renovation.guardian.data.repo.AppContainer
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetViewModelTest {

    @Test
    fun overspend_category_emitted() = runTest {
        val app = mockk<RenovationApp>(relaxed = true)
        val container = mockk<AppContainer>(relaxed = true)
        every { app.container } returns container
        every { container.todayProvider } returns { "2026-09-01" }
        every { container.budgetRepo.observeCategoriesWithSpent() } returns flowOf(
            listOf(
                // overCents 由 spent-planned 计算：1500 - 1000 = 500
                CategoryWithSpent("c1", "主材", "🧱", 1000, 1500, 0, false),
            ),
        )
        every { container.budgetRepo.observeForCsv() } returns flowOf(emptyList())

        val vm = BudgetViewModel(app)
        vm.categories.collect { list ->
            assertTrue(list.any { it.overCents > 0 })
        }
    }
}
