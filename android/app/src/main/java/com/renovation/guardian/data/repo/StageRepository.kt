package com.renovation.guardian.data.repo

import com.renovation.guardian.data.db.AppDatabase
import com.renovation.guardian.data.db.StageEntity
import kotlinx.coroutines.flow.Flow

class StageRepository(private val db: AppDatabase) {
    fun observeAll(): Flow<List<StageEntity>> = db.stageDao().observeAll()
    suspend fun listAll(): List<StageEntity> = db.stageDao().listAll()
    suspend fun getById(id: String): StageEntity? = db.stageDao().getById(id)
}