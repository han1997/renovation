package com.renovation.guardian.data.repo

import com.renovation.guardian.data.db.AppDatabase
import com.renovation.guardian.data.db.ContactEntity
import com.renovation.guardian.data.db.NoteEntity
import com.renovation.guardian.util.IdGen
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val db: AppDatabase) {
    fun observeAll(): Flow<List<NoteEntity>> = db.noteDao().observeAll()
    suspend fun getById(id: String): NoteEntity? = db.noteDao().getById(id)
    suspend fun upsert(title: String, body: String, today: String, existingId: String?) {
        val id = existingId ?: IdGen.new("nt")
        val createdAt = existingId?.let { db.noteDao().getById(it)?.createdAt } ?: today
        db.noteDao().upsert(
            NoteEntity(
                id = id,
                title = title,
                body = body,
                updatedAt = today,
                createdAt = createdAt,
            ),
        )
    }
    suspend fun delete(id: String) = db.noteDao().delete(id)
}

class ContactRepository(private val db: AppDatabase) {
    fun observeAll(): Flow<List<ContactEntity>> = db.contactDao().observeAll()
    suspend fun getById(id: String): ContactEntity? = db.contactDao().getById(id)
    suspend fun upsert(name: String, role: String?, phone: String?, note: String?, existingId: String?) {
        val id = existingId ?: IdGen.new("ct")
        db.contactDao().upsert(
            ContactEntity(
                id = id,
                name = name,
                role = role?.takeIf { it.isNotBlank() },
                phone = phone?.takeIf { it.isNotBlank() },
                note = note?.takeIf { it.isNotBlank() },
                createdAt = existingId?.let { db.contactDao().getById(it)?.createdAt } ?: java.time.LocalDate.now().toString(),
            ),
        )
    }
    suspend fun delete(id: String) = db.contactDao().delete(id)
}