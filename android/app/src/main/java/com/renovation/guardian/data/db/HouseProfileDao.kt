package com.renovation.guardian.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseProfileDao {

    @Query("SELECT * FROM house_profile WHERE id = 1 LIMIT 1")
    fun observe(): Flow<HouseProfileEntity?>

    @Query("SELECT * FROM house_profile WHERE id = 1 LIMIT 1")
    suspend fun get(): HouseProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: HouseProfileEntity)

    @Query("DELETE FROM house_profile")
    suspend fun deleteAll()

    @Transaction
    suspend fun replace(profile: HouseProfileEntity) {
        deleteAll()
        upsert(profile)
    }
}