package mx.tec.sonrisas.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import mx.tec.sonrisas.data.local.entities.MessageEntity

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY sentAt ASC")
    fun observeByChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE sent = 0 AND deleted = 0 AND sendError IS NULL ORDER BY sentAt ASC")
    suspend fun getUnsent(): List<MessageEntity>

    @Query("SELECT MAX(createdAtMs) FROM messages WHERE chatId = :chatId")
    suspend fun newestCreatedAt(chatId: String): Long?

    @Query("UPDATE messages SET sendError = :error WHERE id = :id")
    suspend fun setError(id: String, error: String)

    @Query("UPDATE messages SET deleted = 1 WHERE id = :id")
    suspend fun markDeleted(id: String)

    @Query("UPDATE messages SET deleted = 1 WHERE chatId = :chatId AND sent = 1 AND id NOT IN (:keep)")
    suspend fun markDeletedExcept(chatId: String, keep: List<String>)

    @Query("DELETE FROM messages")
    suspend fun clear()

    @Query("SELECT * FROM messages WHERE deletePending = 1")
    suspend fun getPendingDeletes(): List<MessageEntity>

    @Upsert
    suspend fun save(message: MessageEntity)

    @Delete
    suspend fun delete(message: MessageEntity)
}