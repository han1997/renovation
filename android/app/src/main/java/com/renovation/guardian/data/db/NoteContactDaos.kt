package com.renovation.guardian.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity)

    @Query("DELETE FROM note WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM note ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NoteEntity?

    @Query("SELECT * FROM note")
    suspend fun exportAll(): List<NoteEntity>

    @Query("DELETE FROM note")
    suspend fun clearAll()
}

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contact: ContactEntity)

    @Query("DELETE FROM contact WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM contact ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contact WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ContactEntity?

    @Query("SELECT * FROM contact")
    suspend fun exportAll(): List<ContactEntity>

    @Query("DELETE FROM contact")
    suspend fun clearAll()
}