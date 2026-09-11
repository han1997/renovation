package han1997.renovation.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class CategoryWithSpent(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "emoji") val emoji: String,
    @ColumnInfo(name = "planned_cents") val plannedCents: Long,
    @ColumnInfo(name = "spent_cents") val spentCents: Long,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    @ColumnInfo(name = "is_from_template") val isFromTemplate: Boolean,
) {
    val pct: Int get() {
        if (plannedCents <= 0) return if (spentCents > 0) 999 else 0
        return ((spentCents.toDouble() / plannedCents.toDouble()) * 100).toInt()
    }
    val overCents: Long get() = (spentCents - plannedCents).coerceAtLeast(0)
}

data class ExpenseExportRow(
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "category_name") val categoryName: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "note") val note: String?,
)

@Dao
interface BudgetCategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<BudgetCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: BudgetCategoryEntity)

    @Query("SELECT * FROM budget_category ORDER BY order_index ASC")
    fun observeAll(): Flow<List<BudgetCategoryEntity>>

    @Query("SELECT * FROM budget_category ORDER BY order_index ASC")
    suspend fun listAll(): List<BudgetCategoryEntity>

    @Query("SELECT * FROM budget_category WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BudgetCategoryEntity?

    @Query("DELETE FROM budget_category WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM expense WHERE category_id = :id")
    suspend fun countExpenses(id: String): Int

    @Transaction
    suspend fun deleteIfEmpty(id: String): Boolean {
        if (countExpenses(id) > 0) return false
        deleteById(id)
        return true
    }

    @Query(
        """
        SELECT c.id AS id,
               c.name AS name,
               c.emoji AS emoji,
               c.planned_cents AS planned_cents,
               COALESCE(SUM(e.amount_cents), 0) AS spent_cents,
               c.order_index AS order_index,
               c.is_from_template AS is_from_template
          FROM budget_category c
          LEFT JOIN expense e ON e.category_id = c.id
         GROUP BY c.id
         ORDER BY c.order_index ASC
        """,
    )
    fun observeWithSpent(): Flow<List<CategoryWithSpent>>

    @Query(
        """
        SELECT COALESCE(SUM(e.amount_cents), 0)
          FROM expense e
        """,
    )
    fun observeTotalSpent(): Flow<Long>

    @Query("DELETE FROM budget_category")
    suspend fun clear()
}

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(expense: ExpenseEntity)

    @Query("SELECT COALESCE(SUM(amount_cents), 0) FROM expense WHERE id != :exceptId")
    suspend fun totalExcept(exceptId: String): Long

    @Query("SELECT * FROM expense WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ExpenseEntity?

    @Query("SELECT * FROM expense WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ExpenseEntity?>

    @Query("DELETE FROM expense WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        """
        SELECT * FROM expense
         WHERE (:categoryId IS NULL OR category_id = :categoryId)
         ORDER BY date DESC, id DESC
        """,
    )
    fun observeByCategory(categoryId: String?): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT e.date AS date,
               COALESCE(c.name, '') AS category_name,
               e.name AS name,
               e.amount_cents AS amount_cents,
               e.note AS note
          FROM expense e
          LEFT JOIN budget_category c ON c.id = e.category_id
         ORDER BY e.date ASC, e.id ASC
        """,
    )
    fun observeForCsv(): Flow<List<ExpenseExportRow>>

    @Query("DELETE FROM expense")
    suspend fun clear()

    @Query("SELECT COALESCE(SUM(amount_cents), 0) FROM expense WHERE category_id = :categoryId")
    suspend fun sumForCategory(categoryId: String): Long

    @Query("SELECT * FROM expense")
    suspend fun exportAll(): List<ExpenseEntity>
}