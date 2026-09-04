package com.renovation.guardian.data.repo

import com.renovation.guardian.data.db.AppDatabase
import com.renovation.guardian.data.db.BudgetCategoryEntity
import com.renovation.guardian.data.db.CategoryWithSpent
import com.renovation.guardian.data.db.ExpenseEntity
import com.renovation.guardian.data.db.ExpenseExportRow
import com.renovation.guardian.util.IdGen
import com.renovation.guardian.util.MoneyUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class BudgetRepository(private val db: AppDatabase) {

    fun observeCategoriesWithSpent(): Flow<List<CategoryWithSpent>> =
        db.budgetCategoryDao().observeWithSpent()

    fun observeCategories(): Flow<List<BudgetCategoryEntity>> =
        db.budgetCategoryDao().observeAll()

    suspend fun listCategories(): List<BudgetCategoryEntity> =
        db.budgetCategoryDao().listAll()

    fun observeExpenses(categoryId: String?): Flow<List<ExpenseEntity>> =
        db.expenseDao().observeByCategory(categoryId)

    fun observeForCsv(): Flow<List<ExpenseExportRow>> =
        db.expenseDao().observeForCsv()

    fun observeTotalSpent(): Flow<Long> =
        db.budgetCategoryDao().observeTotalSpent()

    suspend fun addCategory(name: String, emoji: String, plannedYuan: Double, today: String) {
        val existing = db.budgetCategoryDao().listAll()
        val order = (existing.maxOfOrNull { it.orderIndex } ?: -1) + 1
        db.budgetCategoryDao().upsert(
            BudgetCategoryEntity(
                id = IdGen.new("bc"),
                name = name,
                emoji = emoji,
                plannedCents = MoneyUtil.fromYuan(plannedYuan),
                orderIndex = order,
                isFromTemplate = false,
                createdAt = today,
            ),
        )
    }

    suspend fun updateCategory(id: String, name: String, plannedYuan: Double) {
        val cat = db.budgetCategoryDao().getById(id) ?: return
        db.budgetCategoryDao().upsert(
            cat.copy(
                name = name,
                plannedCents = MoneyUtil.fromYuan(plannedYuan),
            ),
        )
    }

    suspend fun deleteCategoryIfEmpty(id: String): Boolean =
        db.budgetCategoryDao().deleteIfEmpty(id)

    suspend fun addExpense(
        name: String,
        amountYuan: Double,
        categoryId: String,
        date: String,
        note: String?,
        today: String,
    ) {
        db.expenseDao().upsert(
            ExpenseEntity(
                id = IdGen.new("ex"),
                name = name,
                amountCents = MoneyUtil.fromYuan(amountYuan),
                categoryId = categoryId,
                date = date,
                note = note,
                createdAt = today,
            ),
        )
    }

    suspend fun updateExpense(
        id: String,
        name: String,
        amountYuan: Double,
        categoryId: String,
        date: String,
        note: String?,
    ) {
        val ex = db.expenseDao().getById(id) ?: return
        db.expenseDao().upsert(
            ex.copy(
                name = name,
                amountCents = MoneyUtil.fromYuan(amountYuan),
                categoryId = categoryId,
                date = date,
                note = note,
            ),
        )
    }

    suspend fun deleteExpense(id: String) {
        db.expenseDao().deleteById(id)
    }

    /** 分类超支检测：返回该分类当前 spentCents - plannedCents（>0 即超支）。 */
    suspend fun overspendCents(categoryId: String): Long {
        val cat = db.budgetCategoryDao().getById(categoryId) ?: return 0L
        val total = db.expenseDao().sumForCategory(categoryId)
        return (total - cat.plannedCents).coerceAtLeast(0L)
    }
}