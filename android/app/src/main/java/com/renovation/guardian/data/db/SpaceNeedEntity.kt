package com.renovation.guardian.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 空间需求。 */
@Entity(tableName = "space_need")
data class SpaceNeedEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "preset_id") val presetId: String?,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "emoji") val emoji: String,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "budget_category_id", index = true) val budgetCategoryId: String?,
    @ColumnInfo(name = "budget_note") val budgetNote: String?,
    @ColumnInfo(name = "is_custom") val isCustom: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String,
)

/** 空间需求 ↔ 阶段 N..M 关联表。 */
@Entity(
    tableName = "space_need_stage",
    primaryKeys = ["space_id", "stage_id"],
    indices = [Index("stage_id")],
)
data class SpaceNeedStageEntity(
    @ColumnInfo(name = "space_id") val spaceId: String,
    @ColumnInfo(name = "stage_id") val stageId: String,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
)