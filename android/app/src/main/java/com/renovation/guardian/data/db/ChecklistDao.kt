package com.renovation.guardian.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class ChecklistWithItems(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "emoji") val emoji: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "note") val note: String?,
    @ColumnInfo(name = "items") val items: List<ChecklistItemRow>,
)

data class ChecklistItemRow(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "done") val done: Boolean,
)

@Dao
interface ChecklistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun seedChecklists(rows: List<ChecklistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun seedItems(rows: List<ChecklistItemEntity>)

    @Query("SELECT COUNT(*) FROM checklist")
    suspend fun count(): Int

    @Query("SELECT * FROM checklist")
    suspend fun listAll(): List<ChecklistEntity>

    @Query(
        """
        SELECT ci.id AS id,
               ci.order_index AS order_index,
               ci.text AS text,
               COALESCE(c.done, 0) AS done
          FROM checklist_item ci
          LEFT JOIN checklist_item_check c ON c.item_id = ci.id
         WHERE ci.checklist_id = :checklistId
         ORDER BY ci.order_index ASC
        """,
    )
    fun observeItems(checklistId: String): Flow<List<ChecklistItemRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCheck(check: ChecklistItemCheckEntity)

    @Query("DELETE FROM checklist_item_check WHERE item_id IN (SELECT id FROM checklist_item WHERE checklist_id = :checklistId)")
    suspend fun clearChecksFor(checklistId: String)

    @Query("DELETE FROM checklist")
    suspend fun clearChecklists()

    @Query("DELETE FROM checklist_item")
    suspend fun clearItems()

    @Query("DELETE FROM checklist_item_check")
    suspend fun clearChecks()

    @Query("SELECT * FROM checklist_item_check")
    suspend fun exportChecks(): List<ChecklistItemCheckEntity>
}