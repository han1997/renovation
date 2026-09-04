package com.renovation.guardian.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface StageOverrideDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: StageOverrideEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<StageOverrideEntity>)

    @Query("SELECT * FROM stage_override WHERE stage_id = :stageId LIMIT 1")
    suspend fun get(stageId: String): StageOverrideEntity?

    @Query("DELETE FROM stage_override WHERE stage_id = :stageId")
    suspend fun delete(stageId: String)

    @Transaction
    suspend fun setOverride(stageId: String, done: Boolean, today: String) {
        if (done) {
            upsert(StageOverrideEntity(stageId, StageOverrideEntity.STATE_DONE, today))
        } else {
            delete(stageId)
        }
    }
}