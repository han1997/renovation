package com.renovation.guardian.data.repo

import com.renovation.guardian.data.db.AppDatabase
import com.renovation.guardian.data.db.ChecklistItemCheckEntity
import com.renovation.guardian.data.db.ChecklistItemRow
import kotlinx.coroutines.flow.Flow

class ChecklistRepository(private val db: AppDatabase) {

    fun observeItems(checklistId: String): Flow<List<ChecklistItemRow>> =
        db.checklistDao().observeItems(checklistId)

    suspend fun listAll(): List<com.renovation.guardian.data.db.ChecklistEntity> =
        db.checklistDao().listAll()

    suspend fun setChecked(itemId: String, done: Boolean, today: String) {
        db.checklistDao().upsertCheck(ChecklistItemCheckEntity(itemId, done, if (done) today else null))
    }

    suspend fun clearChecksFor(checklistId: String) =
        db.checklistDao().clearChecksFor(checklistId)
}