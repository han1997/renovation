package com.renovation.guardian.data.db

import kotlinx.serialization.Serializable

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 房屋信息（单行表）。
 * 对应 Web `state.profile` + `state.quiz.styleId / styleQuizAt`。
 *
 * 用 `PRIMARY KEY CHECK(id = 1)` 强制单行；多套房屋是 v2+。
 */
@Entity(
    tableName = "house_profile",
    primaryKeys = ["id"],
)
@Serializable
data class HouseProfileEntity(
    @ColumnInfo(name = "id") val id: Int = SINGLE_ROW_ID,
    @ColumnInfo(name = "area_m2") val areaM2: Double,
    @ColumnInfo(name = "tier_id") val tierId: String,
    @ColumnInfo(name = "mode_id") val modeId: String,
    @ColumnInfo(name = "grade_id") val gradeId: String,
    @ColumnInfo(name = "start_date") val startDate: String?,
    @ColumnInfo(name = "total_budget_cents") val totalBudgetCents: Long,
    @ColumnInfo(name = "style_id") val styleId: String?,
    @ColumnInfo(name = "style_quiz_at") val styleQuizAt: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
) {
    companion object {
        const val SINGLE_ROW_ID = 1
    }
}