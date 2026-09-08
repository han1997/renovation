package com.renovation.guardian.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuotePlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plan: QuotePlanEntity)

    @Query("SELECT * FROM quote_plan WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): QuotePlanEntity?

    @Query("SELECT * FROM quote_plan WHERE mode = :mode ORDER BY updated_at DESC")
    fun observeByMode(mode: String): Flow<List<QuotePlanEntity>>

    @Query("SELECT * FROM quote_plan ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<QuotePlanEntity>>

    @Query("DELETE FROM quote_plan WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM quote_plan")
    suspend fun clearAll()
}

@Dao
interface PlannerStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: PlannerStateEntity)

    @Query("SELECT * FROM planner_state WHERE id = 1 LIMIT 1")
    suspend fun get(): PlannerStateEntity?

    @Query("DELETE FROM planner_state")
    suspend fun clearAll()
}
