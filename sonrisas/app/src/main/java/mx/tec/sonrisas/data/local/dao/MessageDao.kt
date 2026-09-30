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

    @Query("SELECT * FROM messages WHERE synced = 0")
    suspend fun getPendingSync(): List<MessageEntity>

    @Upsert
    suspend fun save(message: MessageEntity)

    @Delete
    suspend fun delete(message: MessageEntity)
}
