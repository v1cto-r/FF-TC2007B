package mx.tec.sonrisas.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    @SerialName("full_name") val fullName: String,
    val role: String,
    @SerialName("estancia_id") val estanciaId: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    val status: String? = null
)

@Serializable
data class UserCreateBody(
    val email: String,
    @SerialName("full_name") val fullName: String,
    val role: String
)

@Serializable
data class UserCreatedDto(
    val id: String,
    val email: String,
    val role: String,
    @SerialName("temporary_password") val temporaryPassword: String
)

@Serializable
data class SalonDto(
    val id: String,
    @SerialName("estancia_id") val estanciaId: String,
    val name: String,
    @SerialName("group_type") val groupType: String,
    @SerialName("docente_id") val docenteId: String? = null,
    val status: String? = null
)

@Serializable
data class SalonInBody(
    @SerialName("estancia_id") val estanciaId: String,
    val name: String,
    @SerialName("group_type") val groupType: String,
    @SerialName("docente_id") val docenteId: String?
)

@Serializable
data class AdminChildDto(
    val id: String,
    @SerialName("estancia_id") val estanciaId: String,
    @SerialName("salon_id") val salonId: String? = null,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("full_name") val fullName: String,
    val status: String? = null
)

@Serializable
data class ChildInBody(
    @SerialName("estancia_id") val estanciaId: String,
    @SerialName("salon_id") val salonId: String?,
    @SerialName("parent_id") val parentId: String?,
    @SerialName("full_name") val fullName: String
)

@Serializable
data class NoticeDto(
    val id: String,
    @SerialName("estancia_id") val estanciaId: String,
    @SerialName("salon_id") val salonId: String? = null,
    @SerialName("author_id") val authorId: String? = null,
    val name: String,
    val description: String,
    val date: String,
    val status: String? = null
)

@Serializable
data class NoticeInBody(
    @SerialName("estancia_id") val estanciaId: String,
    @SerialName("salon_id") val salonId: String?,
    val name: String,
    val description: String,
    val date: String
)