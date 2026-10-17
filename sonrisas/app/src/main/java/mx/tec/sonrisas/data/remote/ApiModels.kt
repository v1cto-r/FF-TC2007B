package mx.tec.sonrisas.data.remote

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mx.tec.sonrisas.domain.Message

@Serializable
data class TokenPairDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    val pending: Boolean
)

@Serializable
data class DeviceBody(val token: String, val platform: String = "ANDROID")

@Serializable
data class RefreshBody(@SerialName("refresh_token") val refreshToken: String)

@Serializable
data class ChangePasswordBody(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String
)

@Serializable
data class MeDto(
    val sub: String,
    val email: String,
    @SerialName("full_name") val fullName: String,
    val role: String,
    val pending: Boolean
)

@Serializable
data class ChildDto(
    val id: String,
    @SerialName("full_name") val fullName: String
)

@Serializable
data class MessageDto(
    val id: String,
    @SerialName("child_id") val childId: String,
    @SerialName("author_id") val authorId: String? = null,
    val content: String,
    @SerialName("sent_at") val sentAt: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("is_aviso") val isAviso: Boolean = false,
    @SerialName("attachment_file_id") val attachmentFileId: String? = null
)

@Serializable
data class FileCreateBody(
    val id: String,
    val purpose: String,
    @SerialName("target_id") val targetId: String,
    @SerialName("content_type") val contentType: String,
    val size: Int
)

@Serializable
data class SignedUploadDto(val url: String, val method: String = "PUT", val headers: Map<String, String> = emptyMap())

@Serializable
data class UploadTicketDto(val upload: SignedUploadDto)

@Serializable
data class FileDownloadDto(val url: String, @SerialName("expires_at") val expiresAt: String)

private val imageLine = Regex("""(?:^|\n)::Image /([0-9a-fA-F-]{36})(?:\.\w+)?\s*$""")

fun MessageDto.toDomain(): Message {
    val match = imageLine.find(content)
    return Message(
        id = id,
        chatId = childId,
        authorId = authorId.orEmpty(),
        content = if (match != null) content.removeRange(match.range) else content,
        sentAt = OffsetDateTime.parse(sentAt).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(),
        sent = true,
        synced = true,
        isAviso = isAviso,
        createdAtMs = OffsetDateTime.parse(createdAt).toInstant().toEpochMilli(),
        attachmentFileId = attachmentFileId ?: match?.groupValues?.get(1)
    )
}

fun LocalDateTime.toUtcIso(): String = atZone(ZoneId.systemDefault()).toInstant().toString()
