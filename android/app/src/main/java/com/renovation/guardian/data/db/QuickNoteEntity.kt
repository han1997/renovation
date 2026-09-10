package com.renovation.guardian.data.db

import kotlinx.serialization.Serializable

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 随手记：快速捕捉购物愿望与阶段备忘。
 *
 * - `type` 二选一：[TYPE_WISH]（购物愿望，带品类）或 [TYPE_MEMO]（阶段备忘，关联阶段）；
 * - `category` 仅 wish 有值（furniture / appliance / daily）；
 * - `stage_id` 仅 memo 有值（关联 14 阶段之一的 `stage.id`）；
 * - 日期为 `yyyy-MM-dd` 字符串，与全库约定一致。
 */
@Entity(tableName = "quick_note")
@Serializable
data class QuickNoteEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "category") val category: String? = null,
    @ColumnInfo(name = "stage_id") val stageId: String? = null,
    @ColumnInfo(name = "is_done") val isDone: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
) {
    companion object {
        /** 类型：购物愿望。 */
        const val TYPE_WISH = "wish"
        /** 类型：阶段备忘。 */
        const val TYPE_MEMO = "memo"
        /** 品类（仅 wish）：家具。 */
        const val CATEGORY_FURNITURE = "furniture"
        /** 品类（仅 wish）：家电。 */
        const val CATEGORY_APPLIANCE = "appliance"
        /** 品类（仅 wish）：生活用品。 */
        const val CATEGORY_DAILY = "daily"
    }
}
