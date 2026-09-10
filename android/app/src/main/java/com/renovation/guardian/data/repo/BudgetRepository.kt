package com.renovation.guardian.data.repo

import com.renovation.guardian.data.db.AppDatabase
import com.renovation.guardian.data.db.BudgetCategoryEntity
import com.renovation.guardian.data.db.CategoryWithSpent
import com.renovation.guardian.data.db.ExpenseEntity
import com.renovation.guardian.data.db.ExpenseExportRow
import com.renovation.guardian.util.IdGen
import com.renovation.guardian.util.MoneyUtil
import androidx.room.withTransaction
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

    fun observeExpense(id: String): Flow<ExpenseEntity?> = db.expenseDao().observeById(id)

    fun observeForCsv(): Flow<List<ExpenseExportRow>> =
        db.expenseDao().observeForCsv()

    fun observeTotalSpent(): Flow<Long> =
        db.budgetCategoryDao().observeTotalSpent()

    suspend fun addCategoryCents(name: String, emoji: String, plannedCents: Long, today: String) = db.withTransaction {
        require(name.isNotBlank() && plannedCents >= 0) { "名称或分类计划金额无效" }
        val existing = db.budgetCategoryDao().listAll()
        Math.addExact(existing.fold(0L) { sum, c -> Math.addExact(sum, c.plannedCents) }, plannedCents)
        val order = (existing.maxOfOrNull { it.orderIndex } ?: -1) + 1
        db.budgetCategoryDao().upsert(
            BudgetCategoryEntity(
                id = IdGen.new("bc"),
                name = name,
                emoji = emoji,
                plannedCents = plannedCents,
                orderIndex = order,
                isFromTemplate = false,
                createdAt = today,
            ),
        )
    }

    suspend fun updateCategoryCents(id: String, name: String, plannedCents: Long) = db.withTransaction {
        require(name.isNotBlank() && plannedCents >= 0) { "名称或分类计划金额无效" }
        Math.addExact(db.budgetCategoryDao().listAll().filterNot { it.id == id }.fold(0L) { sum, c -> Math.addExact(sum, c.plannedCents) }, plannedCents)
        val cat = requireNotNull(db.budgetCategoryDao().getById(id)) { "分类已不存在" }
        db.budgetCategoryDao().upsert(
            cat.copy(
                name = name,
                plannedCents = plannedCents,
            ),
        )
    }

    suspend fun deleteCategoryIfEmpty(id: String): Boolean =
        db.budgetCategoryDao().deleteIfEmpty(id)

    suspend fun addExpenseCents(
        name: String,
        amountCents: Long,
        categoryId: String,
        date: String,
        note: String?,
        today: String,
    ) = db.withTransaction {
        require(name.isNotBlank() && amountCents > 0) { "项目名称或支出金额无效" }
        Math.addExact(db.expenseDao().totalExcept(""), amountCents)
        kotlinx.datetime.LocalDate.parse(date)
        require(db.budgetCategoryDao().getById(categoryId) != null) { "分类已不存在，请重新选择" }

        db.expenseDao().upsert(
            ExpenseEntity(
                id = IdGen.new("ex"),
                name = name,
                amountCents = amountCents,
                categoryId = categoryId,
                date = date,
                note = note,
                createdAt = today,
            ),
        )
    }

    suspend fun updateExpenseCents(
        id: String,
        name: String,
        amountCents: Long,
        categoryId: String,
        date: String,
        note: String?,
    ) = db.withTransaction {
        require(name.isNotBlank() && amountCents > 0) { "项目名称或支出金额无效" }
        Math.addExact(db.expenseDao().totalExcept(id), amountCents)
        kotlinx.datetime.LocalDate.parse(date)
        require(db.budgetCategoryDao().getById(categoryId) != null) { "分类已不存在，请重新选择" }

        val ex = requireNotNull(db.expenseDao().getById(id)) { "支出已不存在" }
        db.expenseDao().upsert(
            ex.copy(
                name = name,
                amountCents = amountCents,
                categoryId = categoryId,
                date = date,
                note = note,
            ),
        )
    }

    suspend fun deleteExpense(id: String) {
        db.expenseDao().deleteById(id)
    }

    // 旧调用兼容入口；交互输入统一走上面的整数分接口。
    suspend fun addCategory(name: String, emoji: String, plannedYuan: Double, today: String) =
        addCategoryCents(name, emoji, MoneyUtil.fromYuan(plannedYuan), today)
    suspend fun updateCategory(id: String, name: String, plannedYuan: Double) =
        updateCategoryCents(id, name, MoneyUtil.fromYuan(plannedYuan))
    suspend fun addExpense(name: String, amountYuan: Double, categoryId: String, date: String, note: String?, today: String) =
        addExpenseCents(name, MoneyUtil.fromYuan(amountYuan), categoryId, date, note, today)
    suspend fun updateExpense(id: String, name: String, amountYuan: Double, categoryId: String, date: String, note: String?) =
        updateExpenseCents(id, name, MoneyUtil.fromYuan(amountYuan), categoryId, date, note)

    /** 分类超支检测：返回该分类当前 spentCents - plannedCents（>0 即超支）。 */
    suspend fun overspendCents(categoryId: String): Long {
        val cat = db.budgetCategoryDao().getById(categoryId) ?: return 0L
        val total = db.expenseDao().sumForCategory(categoryId)
        return (total - cat.plannedCents).coerceAtLeast(0L)
    }
}