package com.renovation.guardian.data.db

import kotlinx.serialization.Serializable

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 内置模板任务的勾选 / 日期。
 * 对应 Web `state.tasksDone` / `state.taskDates` 中 key 与 `task_template.id`
 * 重叠的部分。
 */
@Entity(tableName = "task_completion")
@Serializable
data class TaskCompletionEntity(
    @PrimaryKey @ColumnInfo(name = "template_id") val templateId: String,
    @ColumnInfo(name = "due_date") val dueDate: String?,
    @ColumnInfo(name = "done") val done: Boolean = false,
    @ColumnInfo(name = "done_at") val doneAt: String?,
)