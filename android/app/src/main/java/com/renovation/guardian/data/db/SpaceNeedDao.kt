package com.renovation.guardian.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class SpaceNeedWithStages(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "preset_id") val presetId: String?,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "emoji") val emoji: String,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "budget_category_id") val budgetCategoryId: String?,
    @ColumnInfo(name = "budget_note") val budgetNote: String?,
    @ColumnInfo(name = "is_custom") val isCustom: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "stage_ids") val stageIds: String,
)

data class SpaceExportTuple(
    @ColumnInfo(name = "space") val space: SpaceNeedEntity,
    @ColumnInfo(name = "stage_ids") val stageIds: List<String>,
)

@Dao
interface SpaceNeedDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(space: SpaceNeedEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStages(rows: List<SpaceNeedStageEntity>)

    @Query("SELECT * FROM space_need WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SpaceNeedEntity?

    @Query("DELETE FROM space_need_stage WHERE space_id = :spaceId")
    suspend fun clearStagesForSpace(spaceId: String)

    @Query("DELETE FROM space_need WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT stage_id FROM space_need_stage WHERE space_id = :spaceId ORDER BY order_index ASC")
    suspend fun stageIdsFor(spaceId: String): List<String>

    @Query(
        """
        SELECT s.id AS id,
               s.preset_id AS preset_id,
               s.name AS name,
               s.emoji AS emoji,
               s.description AS description,
               s.budget_category_id AS budget_category_id,
               s.budget_note AS budget_note,
               s.is_custom AS is_custom,
               s.created_at AS created_at,
               (SELECT GROUP_CONCAT(sns.stage_id, ',') FROM space_need_stage sns WHERE sns.space_id = s.id ORDER BY sns.order_index ASC) AS stage_ids
          FROM space_need s
         ORDER BY s.created_at ASC
        """,
    )
    fun observeAll(): Flow<List<SpaceNeedWithStages>>

    @Transaction
    suspend fun add(space: SpaceNeedEntity, stageIds: List<String>) {
        upsert(space)
        clearStagesForSpace(space.id)
        upsertStages(stageIds.mapIndexed { idx, sid -> SpaceNeedStageEntity(space.id, sid, idx) })
    }

    @Transaction
    suspend fun delete(id: String) {
        clearStagesForSpace(id)
        deleteById(id)
    }

    @Query("DELETE FROM space_need_stage")
    suspend fun clearStages()

    @Query("DELETE FROM space_need")
    suspend fun clearSpaces()

    @Transaction
    suspend fun clearAll() {
        clearStages()
        clearSpaces()
    }

    @Query(
        """
        SELECT s.* FROM space_need s ORDER BY s.created_at ASC
        """,
    )
    suspend fun listAll(): List<SpaceNeedEntity>
}

suspend fun SpaceNeedDao.exportSpaces(): List<SpaceExportTuple> {
    val spaces = listAll()
    return spaces.map { sp ->
        SpaceExportTuple(sp, stageIdsFor(sp.id))
    }
}