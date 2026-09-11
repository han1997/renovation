package han1997.renovation.data.db

import kotlinx.serialization.Serializable

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note")
@Serializable
data class NoteEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "body") val body: String = "",
    @ColumnInfo(name = "updated_at") val updatedAt: String,
    @ColumnInfo(name = "created_at") val createdAt: String,
)

@Entity(tableName = "contact")
@Serializable
data class ContactEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "role") val role: String?,
    @ColumnInfo(name = "phone") val phone: String?,
    @ColumnInfo(name = "note") val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
)