package han1997.renovation.data.repo

import han1997.renovation.data.db.AppDatabase
import han1997.renovation.data.db.StageEntity
import kotlinx.coroutines.flow.Flow

class StageRepository(private val db: AppDatabase) {
    fun observeAll(): Flow<List<StageEntity>> = db.stageDao().observeAll()
    suspend fun listAll(): List<StageEntity> = db.stageDao().listAll()
    suspend fun getById(id: String): StageEntity? = db.stageDao().getById(id)
}