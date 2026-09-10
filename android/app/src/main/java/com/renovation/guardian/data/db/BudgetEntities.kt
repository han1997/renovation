package com.renovation.guardian.data.db

import kotlinx.serialization.Serializable

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** 预算分类。`planned_cents` 用分（cents）。 */
@Entity(tableName = "budget_category")
@Serializable
data class BudgetCategoryEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "emoji") val emoji: String,
    @ColumnInfo(name = "planned_cents") val plannedCents: Long = 0L,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    @ColumnInfo(name = "is_from_template") val isFromTemplate: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String,
)

/** 支出条目。`amount_cents` 用分（cents）。 */
@Entity(tableName = "expense")
@Serializable
data class ExpenseEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "category_id", index = true) val categoryId: String,
    @ColumnInfo(name = "date", index = true) val date: String,
    @ColumnInfo(name = "note") val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
)