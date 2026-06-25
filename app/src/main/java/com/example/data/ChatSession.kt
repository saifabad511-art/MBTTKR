package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val messagesJson: String // Serialized List<ChatMessage>
)

@Dao
interface ChatSessionDao {
    @Query("SELECT * FROM chat_sessions ORDER BY createdAt DESC")
    fun getAllSessionsFlow(): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getSessionById(id: Int): ChatSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long

    @Update
    suspend fun updateSession(session: ChatSession)

    @Delete
    suspend fun deleteSession(session: ChatSession)

    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAllSessions()
}
