package mx.tec.sonrisas.data.remote

import javax.inject.Inject
import mx.tec.sonrisas.data.AuthRepository
import mx.tec.sonrisas.domain.Child
import mx.tec.sonrisas.domain.GroupEnum

interface ChildRemoteDataSource {
    suspend fun fetchAll(parentName: String): List<Child>
    suspend fun submitProfile(childId: String, birthDate: String, allergies: List<String>): ChildDto
}

class ChildRemoteDataSourceImpl @Inject constructor(
    private val api: ApiClient,
    private val auth: AuthRepository
) : ChildRemoteDataSource {
    override suspend fun fetchAll(parentName: String): List<Child> =
        auth.authed { api.get<List<ChildDto>>("/children", it) }.map { dto ->
            Child(
                id = dto.id,
                fullName = dto.fullName,
                daycareCenter = "Estancia Mariposa",
                group = GroupEnum.P,
                // The server sends parent_name: a DOCENTE can't read /users, so it can't look the parent up itself.
                parentName = parentName.ifBlank { dto.parentName.orEmpty() },
                birthdate = dto.birthDate ?: "Sin fecha registrada",
                allergies = if (dto.allergies.isNotEmpty()) dto.allergies.joinToString(", ") else "Sin alergias registradas",
                profileCompletedAt = dto.profileCompletedAt
            )
        }

    override suspend fun submitProfile(childId: String, birthDate: String, allergies: List<String>): ChildDto {
        val body = ChildProfilePatchBody(birthDate = birthDate, allergies = allergies)
        return auth.authed { api.patch<ChildProfilePatchBody, ChildDto>("/children/$childId/profile", body, it) }
    }
}
