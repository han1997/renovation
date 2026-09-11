package han1997.renovation.data.repo

import han1997.renovation.data.db.AppDatabase
import han1997.renovation.data.db.ChecklistItemCheckEntity
import han1997.renovation.data.db.ChecklistItemRow
import kotlinx.coroutines.flow.Flow

class ChecklistRepository(private val db: AppDatabase) {

    fun observeItems(checklistId: String): Flow<List<ChecklistItemRow>> =
        db.checklistDao().observeItems(checklistId)

    suspend fun listAll(): List<han1997.renovation.data.db.ChecklistEntity> =
        db.checklistDao().listAll()

    suspend fun setChecked(itemId: String, done: Boolean, today: String) {
        db.checklistDao().upsertCheck(ChecklistItemCheckEntity(itemId, done, if (done) today else null))
    }

    suspend fun clearChecksFor(checklistId: String) =
        db.checklistDao().clearChecksFor(checklistId)
}