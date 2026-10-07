package mx.tec.sonrisas.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "messages", indices = [Index("chatId")])
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val authorId: String,
    val content: String,
    val sentAt: LocalDateTime,
    val sent: Boolean,
    val synced: Boolean,
    val isAviso: Boolean,
    val hiddenForParent: Boolean = false
)
