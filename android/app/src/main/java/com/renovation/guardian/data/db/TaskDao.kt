package com.renovation.guardian.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** 单阶段内的"任务列表（含勾选 + 日期）"合并视图。 */
data class StageTaskView(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "stage_id") val stageId: String,
    @ColumnInfo(name = "source") val source: String, // TEMPLATE | CUSTOM | DERIVED
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "tip") val tip: String?,
    @ColumnInfo(name = "space_id") val spaceId: String?,
    @ColumnInfo(name = "due_date") val dueDate: String?,
    @ColumnInfo(name = "done") val done: Boolean,
)

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletion(c: TaskCompletionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletions(c: List<TaskCompletionEntity>)

    @Query("SELECT * FROM task WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT * FROM task WHERE stage_id = :stageId ORDER BY created_at ASC")
    suspend fun listAllForStage(stageId: String): List<TaskEntity>

    @Query("SELECT * FROM task WHERE stage_id = :stageId ORDER BY created_at ASC")
    fun observeByStage(stageId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM task_completion WHERE template_id = :templateId LIMIT 1")
    fun observeCompletion(templateId: String): Flow<TaskCompletionEntity?>

    @Query("SELECT * FROM task_completion WHERE template_id = :templateId LIMIT 1")
    suspend fun getCompletion(templateId: String): TaskCompletionEntity?

    @Query("DELETE FROM task WHERE id = :id")
    suspend fun deleteTask(id: String)

    @Query("DELETE FROM task WHERE space_id = :spaceId AND done = 0")
    suspend fun deleteUndoneDerivedForSpace(spaceId: String)

    @Query("UPDATE task SET space_id = NULL WHERE space_id = :spaceId AND done = 1")
    suspend fun unmarkDerivedDoneForSpace(spaceId: String)

    /** 把所有"未来 7 天内 / 今日 / 已逾期"且未完成的任务一次性拉出。 */
    @Query(
        """
        SELECT t.id AS id,
               t.stage_id AS stage_id,
               'CUSTOM_OR_DERIVED' AS source,
               t.text AS text,
               t.tip AS tip,
               t.space_id AS space_id,
               t.due_date AS due_date,
               t.done AS done
          FROM task t
         WHERE t.due_date IS NOT NULL AND t.done = 0
           AND t.due_date BETWEEN :startDate AND :endDate
        UNION ALL
        SELECT tt.id AS id,
               tt.stage_id AS stage_id,
               'TEMPLATE' AS source,
               tt.text AS text,
               tt.tip AS tip,
               NULL AS space_id,
               tc.due_date AS due_date,
               COALESCE(tc.done, 0) AS done
          FROM task_template tt
          LEFT JOIN task_completion tc ON tc.template_id = tt.id
         WHERE tc.due_date IS NOT NULL AND COALESCE(tc.done, 0) = 0
           AND tc.due_date BETWEEN :startDate AND :endDate
        ORDER BY due_date ASC, source DESC
        """,
    )
    fun observeBetween(startDate: String, endDate: String): Flow<List<StageTaskView>>

    /** 任务完成度（按阶段聚合）。 */
    @Query(
        """
        SELECT st.id AS stageId,
               (SELECT COUNT(*) FROM task_template tt WHERE tt.stage_id = st.id) +
               (SELECT COUNT(*) FROM task t WHERE t.stage_id = st.id) AS total,
               (SELECT COUNT(*) FROM task_template tt
                  LEFT JOIN task_completion tc ON tc.template_id = tt.id
                WHERE tt.stage_id = st.id AND COALESCE(tc.done, 0) = 1) +
               (SELECT COUNT(*) FROM task t WHERE t.stage_id = st.id AND t.done = 1) AS done
          FROM stage st
         ORDER BY st.order_index ASC
        """,
    )
    fun observeAllProgress(): Flow<List<StageProgressRow>>

    @Transaction
    suspend fun setTemplateDone(templateId: String, done: Boolean, today: String) {
        val existing = getCompletion(templateId)
        upsertCompletion(
            TaskCompletionEntity(
                templateId = templateId,
                dueDate = existing?.dueDate,
                done = done,
                doneAt = if (done) today else null,
            ),
        )
    }

    @Transaction
    suspend fun setTemplateDueDate(templateId: String, date: String?) {
        val existing = getCompletion(templateId)
        upsertCompletion(
            TaskCompletionEntity(
                templateId = templateId,
                dueDate = date,
                done = existing?.done ?: false,
                doneAt = existing?.doneAt,
            ),
        )
    }

    @Query("SELECT * FROM task_completion")
    suspend fun exportCompletions(): List<TaskCompletionEntity>

    @Query("SELECT * FROM task")
    suspend fun exportCustomTasks(): List<TaskEntity>

    @Query("SELECT * FROM stage_override")
    suspend fun exportOverrides(): List<StageOverrideEntity>

    @Query("DELETE FROM task")
    suspend fun clearTasks()

    @Query("DELETE FROM task_completion")
    suspend fun clearCompletions()

    @Query("DELETE FROM stage_override")
    suspend fun clearOverrides()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverride(row: StageOverrideEntity)
}

data class StageProgressRow(
    @ColumnInfo(name = "stageId") val stageId: String,
    @ColumnInfo(name = "total") val total: Int,
    @ColumnInfo(name = "done") val done: Int,
) {
    val pct: Int get() = if (total == 0) 0 else (done * 100 / total)
    val isDone: Boolean get() = total > 0 && done == total
}