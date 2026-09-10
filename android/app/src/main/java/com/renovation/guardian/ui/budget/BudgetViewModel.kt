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

    fun observeExpense(id: String) = container.budgetRepo.observeExpense(id)
    val profile = container.houseProfileRepo.observe()
    fun today(): String = today
    fun addCategoryCents(name: String, emoji: String, cents: Long, onSaved: () -> Unit = {}) =
        perform(onSuccess = onSaved, singleFlight = true) { container.budgetRepo.addCategoryCents(name, emoji, cents, today) }
    fun updateCategoryCents(id: String, name: String, cents: Long, onSaved: () -> Unit = {}) =
        perform(onSuccess = onSaved, singleFlight = true) { container.budgetRepo.updateCategoryCents(id, name, cents) }
    fun addExpenseCents(name: String, cents: Long, categoryId: String, date: String, note: String?, onSaved: () -> Unit = {}) =
        perform(onSuccess = onSaved, singleFlight = true) { container.budgetRepo.addExpenseCents(name, cents, categoryId, date, note, today) }
    fun updateExpenseCents(id: String, name: String, cents: Long, categoryId: String, date: String, note: String?, onSaved: () -> Unit = {}) =
        perform(onSuccess = onSaved, singleFlight = true) { container.budgetRepo.updateExpenseCents(id, name, cents, categoryId, date, note) }
    fun addCategory(name: String, emoji: String, plannedYuan: Double) = addCategoryCents(name, emoji, com.renovation.guardian.util.MoneyUtil.fromYuan(plannedYuan))
    fun updateCategory(id: String, name: String, plannedYuan: Double) = updateCategoryCents(id, name, com.renovation.guardian.util.MoneyUtil.fromYuan(plannedYuan))
    fun addExpense(name: String, amountYuan: Double, categoryId: String, date: String, note: String?) = addExpenseCents(name, com.renovation.guardian.util.MoneyUtil.fromYuan(amountYuan), categoryId, date, note)
    fun updateExpense(id: String, name: String, amountYuan: Double, categoryId: String, date: String, note: String?) = updateExpenseCents(id, name, com.renovation.guardian.util.MoneyUtil.fromYuan(amountYuan), categoryId, date, note)
    fun deleteCategory(id: String, onResult: (Boolean) -> Unit) = perform(success = null) {
        val ok = container.budgetRepo.deleteCategoryIfEmpty(id)
        if (ok) notify("已删除")
        onResult(ok)
    }
    fun deleteExpense(id: String) = perform("已删除") { container.budgetRepo.deleteExpense(id) }
}
