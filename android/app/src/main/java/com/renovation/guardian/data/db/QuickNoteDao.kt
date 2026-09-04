package com.renovation.guardian.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickNoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: QuickNoteEntity)

    @Query("DELETE FROM quick_note WHERE id = :id")
    suspend fun delete(id: String)

    /** 未完成在前（按更新时间倒序），已完成置底 —— 支撑整理视图分组展示。 */
    @Query("SELECT * FROM quick_note ORDER BY is_done ASC, updated_at DESC")
    fun observeAll(): Flow<List<QuickNoteEntity>>

    /** 按类型过滤（wish / memo），排序规则同 [observeAll]。 */
    @Query("SELECT * FROM quick_note WHERE type = :type ORDER BY is_done ASC, updated_at DESC")
    fun observeByType(type: String): Flow<List<QuickNoteEntity>>

    @Query("SELECT * FROM quick_note WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): QuickNoteEntity?

    @Query("SELECT * FROM quick_note")
    suspend fun exportAll(): List<QuickNoteEntity>

    @Query("DELETE FROM quick_note")
    suspend fun clearAll()
}
