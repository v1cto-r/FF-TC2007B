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
    @SerialName("estancia_id") val estanciaId: String? = null,
    val pending: Boolean
)

@Serializable
data class EstanciaOut(
    val id: String,
    val status: String? = null,
    val name: String,
    val address: String? = null,
    @SerialName("admin_user_id") val adminUserId: String? = null,
    @SerialName("logo_file_id") val logoFileId: String? = null
)

@Serializable
data class EstanciaIn(
    val name: String,
    val address: String? = null,
    @SerialName("admin_user_id") val adminUserId: String? = null,
    @SerialName("logo_file_id") val logoFileId: String? = null
)

@Serializable
data class UserOut(
    val id: String,
    val email: String,
    @SerialName("full_name") val fullName: String,
    val role: String,
    @SerialName("estancia_id") val estanciaId: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class UserCreate(
    val email: String,
    @SerialName("full_name") val fullName: String,
    val role: String
)

@Serializable
data class UserCreated(
    val id: String,
    val email: String,
    val role: String,
    @SerialName("temporary_password") val temporaryPassword: String
)

@Serializable
data class ChildDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("profile_completed_at") val profileCompletedAt: String? = null,
    @SerialName("birth_date") val birthDate: String? = null,
    val allergies: List<String> = emptyList()
)

@Serializable
data class ChildProfilePatchBody(
    @SerialName("birth_date") val birthDate: String,
    val allergies: List<String> = emptyList()
)

@Serializable
data class DocumentInBody(
    @SerialName("child_id") val childId: String,
    @SerialName("file_id") val fileId: String,
    val title: String
)

@Serializable
data class DocumentOutDto(
    val id: String,
    @SerialName("child_id") val childId: String,
    @SerialName("file_id") val fileId: String,
    val title: String
)

@Serializable
data class MessageDto(
    val id: String,
    @SerialName("chat_id") val chatId: String,
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
    val size: Int,
    @SerialName("original_name") val originalName: String? = null
)

@Serializable
data class SignedUploadDto(val url: String, val method: String = "PUT", val headers: Map<String, String> = emptyMap())

@Serializable
data class UploadTicketDto(val upload: SignedUploadDto)

@Serializable
data class FileDownloadDto(val url: String, @SerialName("expires_at") val expiresAt: String)

@Serializable
data class PasswordResetDto(
    val id: String,
    @SerialName("temporary_password") val temporaryPassword: String
)

@Serializable
data class SalonOut(
    val id: String,
    val status: String? = null,
    @SerialName("estancia_id") val estanciaId: String,
    val name: String,
    @SerialName("group_type") val groupType: String,
    @SerialName("docente_id") val docenteId: String? = null
)

@Serializable
data class ChildOut(
    val id: String,
    val status: String? = null,
    @SerialName("estancia_id") val estanciaId: String,
    @SerialName("full_name") val fullName: String
)

private val imageLine = Regex("""(?:^|\n)::Image /([0-9a-fA-F-]{36})(?:\.\w+)?\s*$""")
private val avisoLine = Regex("""(?:^|\n)::Aviso\s*$""")

fun MessageDto.toDomain(): Message {
    // The server appends marker lines in this order: text, ::Aviso, ::Image. Strip them from the end inwards.
    val match = imageLine.find(content)
    val withoutImage = if (match != null) content.removeRange(match.range) else content
    val withoutAviso = avisoLine.find(withoutImage)?.let { withoutImage.removeRange(it.range) } ?: withoutImage
    return Message(
        id = id,
        chatId = chatId,
        authorId = authorId.orEmpty(),
        content = withoutAviso,
        sentAt = OffsetDateTime.parse(sentAt).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(),
        sent = true,
        synced = true,
        isAviso = isAviso,
        createdAtMs = OffsetDateTime.parse(createdAt).toInstant().toEpochMilli(),
        attachmentFileId = attachmentFileId ?: match?.groupValues?.get(1)
    )
}

fun LocalDateTime.toUtcIso(): String = atZone(ZoneId.systemDefault()).toInstant().toString()
