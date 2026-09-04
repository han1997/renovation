package com.renovation.guardian.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 验收清单目录。 */
@Entity(tableName = "checklist")
data class ChecklistEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "emoji") val emoji: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "note") val note: String?,
)

/** 验收清单条目目录。 */
@Entity(
    tableName = "checklist_item",
    primaryKeys = ["id"],
    indices = [Index("checklist_id")],
)
data class ChecklistItemEntity(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "checklist_id") val checklistId: String,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    @ColumnInfo(name = "text") val text: String,
)

/** 验收条目勾选。 */
@Entity(tableName = "checklist_item_check")
data class ChecklistItemCheckEntity(
    @PrimaryKey @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "done") val done: Boolean = false,
    @ColumnInfo(name = "done_at") val doneAt: String?,
)