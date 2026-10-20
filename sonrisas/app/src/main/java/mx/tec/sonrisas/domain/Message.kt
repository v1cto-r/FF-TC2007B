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
    // Soft delete shared by both sides (mirrors status = DELETED in the API).
    // The row stays in the database, it is just never shown.
    val deleted: Boolean = false,
    // true = deleted on this device but the server hasn't been told yet.
    val deletePending: Boolean = false,
    val createdAtMs: Long? = null,
    val attachmentFileId: String? = null,
    val sendError: String? = null
)