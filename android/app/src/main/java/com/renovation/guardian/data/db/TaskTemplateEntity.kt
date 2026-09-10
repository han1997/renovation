package com.renovation.guardian.data.db

import kotlinx.serialization.Serializable

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 内置模板任务（来自 `assets/knowledge.json` 的 `stages[].tasks[]`）。
 * 启动时一次性写入；后续 `assets` 更新时按需重建。
 */
@Entity(
    tableName = "task_template",
    primaryKeys = ["id"],
    indices = [Index("stage_id")],
)
@Serializable
data class TaskTemplateEntity(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "stage_id") val stageId: String,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "tip") val tip: String?,
)