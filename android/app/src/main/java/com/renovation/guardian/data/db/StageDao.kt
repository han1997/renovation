package com.renovation.guardian.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun seedAll(stages: List<StageEntity>)

    @Query("SELECT COUNT(*) FROM stage")
    suspend fun count(): Int

    @Query("SELECT * FROM stage ORDER BY order_index ASC")
    fun observeAll(): Flow<List<StageEntity>>

    @Query("SELECT * FROM stage ORDER BY order_index ASC")
    suspend fun listAll(): List<StageEntity>

    @Query("SELECT * FROM stage WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): StageEntity?

    @Query("DELETE FROM stage")
    suspend fun clear()
}