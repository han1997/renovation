package han1997.renovation.data.db

import kotlinx.serialization.Serializable

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户任务（仅自建 + 派生；内置模板任务的勾选 / 日期走 [TaskCompletionEntity]）。
 *
 * CHECK 约束保证 `DERIVED_FROM_SPACE` 必须带 `space_id`，`CUSTOM` 不能带。
 */
@Entity(
    tableName = "task",
)
@Serializable
data class TaskEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "stage_id", index = true) val stageId: String,
    @ColumnInfo(name = "source_type") val sourceType: String, // CUSTOM | DERIVED_FROM_SPACE
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "tip") val tip: String?,
    @ColumnInfo(name = "space_id", index = true) val spaceId: String?,
    @ColumnInfo(name = "due_date", index = true) val dueDate: String?,
    @ColumnInfo(name = "done") val done: Boolean = false,
    @ColumnInfo(name = "done_at") val doneAt: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
) {
    companion object {
        const val TYPE_CUSTOM = "CUSTOM"
        const val TYPE_DERIVED = "DERIVED_FROM_SPACE"
    }
}