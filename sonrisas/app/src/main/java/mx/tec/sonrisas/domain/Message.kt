package mx.tec.sonrisas.domain

import java.time.LocalDateTime
import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val chatId: String,
    val authorId: String,
    val content: String,
    val sentAt: LocalDateTime,
    // false = still only on this device, pending to be sent.
    val sent: Boolean = false,
    val synced: Boolean = false,
    val isAviso: Boolean = false,
    val hiddenForParent: Boolean = false
)
