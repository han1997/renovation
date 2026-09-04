package com.renovation.guardian.ui.budget

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.renovation.guardian.ui.AppViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class BudgetViewModel(application: Application) : AppViewModel(application) {

    private val today: String get() = container.todayProvider()

    val categories: Flow<List<com.renovation.guardian.data.db.CategoryWithSpent>> =
        container.budgetRepo.observeCategoriesWithSpent()

    val csvRows: Flow<List<com.renovation.guardian.data.db.ExpenseExportRow>> =
        container.budgetRepo.observeForCsv()

    fun observeExpenses(categoryId: String): Flow<List<com.renovation.guardian.data.db.ExpenseEntity>> =
        container.budgetRepo.observeExpenses(categoryId)

    fun addCategory(name: String, emoji: String, plannedYuan: Double) {
        if (name.isBlank()) return
        viewModelScope.launch { container.budgetRepo.addCategory(name, emoji, plannedYuan, today) }
    }

    fun updateCategory(id: String, name: String, plannedYuan: Double) {
        viewModelScope.launch { container.budgetRepo.updateCategory(id, name, plannedYuan) }
    }

    fun deleteCategory(id: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = container.budgetRepo.deleteCategoryIfEmpty(id)
            onResult(ok)
        }
    }

    fun addExpense(name: String, amountYuan: Double, categoryId: String, date: String, note: String?) {
        if (name.isBlank()) return
        viewModelScope.launch { container.budgetRepo.addExpense(name, amountYuan, categoryId, date, note, today) }
    }

    fun updateExpense(id: String, name: String, amountYuan: Double, categoryId: String, date: String, note: String?) {
        viewModelScope.launch { container.budgetRepo.updateExpense(id, name, amountYuan, categoryId, date, note) }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch { container.budgetRepo.deleteExpense(id) }
    }
}
