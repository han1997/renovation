package com.renovation.guardian.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskTemplateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun seedAll(rows: List<TaskTemplateEntity>)

    @Query("SELECT * FROM task_template WHERE stage_id = :stageId ORDER BY order_index ASC")
    suspend fun listByStage(stageId: String): List<TaskTemplateEntity>

    @Query("SELECT * FROM task_template WHERE stage_id = :stageId ORDER BY order_index ASC")
    fun observeByStage(stageId: String): Flow<List<TaskTemplateEntity>>

    @Query("SELECT * FROM task_template ORDER BY stage_id, order_index ASC")
    suspend fun listAll(): List<TaskTemplateEntity>

    @Query("DELETE FROM task_template")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM task_template")
    suspend fun count(): Int
}