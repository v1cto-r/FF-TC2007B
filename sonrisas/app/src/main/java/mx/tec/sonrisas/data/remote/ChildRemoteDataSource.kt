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
    override suspend fun fetchAll(parentName: String): List<Child> {
        val dtos = auth.authed { api.get<List<ChildDto>>("/children", it) }

        val parentMap = mutableMapOf<String, String>()

        if (parentName.isNotBlank()) {
            dtos.forEach { dto ->
                dto.parentId?.let { parentMap[it] = parentName }
            }
        } else {
            // 1. Intentar consultar todos los padres via /users?role=PADRE
            val usersList = runCatching {
                auth.authed { api.get<List<UserOut>>("/users?role=PADRE", it) }
            }.getOrNull()

            if (!usersList.isNullOrEmpty()) {
                usersList.forEach { u -> parentMap[u.id] = u.fullName }
            } else {
                // 2. Si /users?role=PADRE devuelve 403 para docentes, consultar cada padre individualmente via /users/{id}
                dtos.mapNotNull { it.parentId }.distinct().forEach { pid ->
                    val user = runCatching {
                        auth.authed { api.get<UserOut>("/users/$pid", it) }
                    }.getOrNull()
                    if (user != null && user.fullName.isNotBlank()) {
                        parentMap[pid] = user.fullName
                    }
                }
            }
        }

        return dtos.map { dto ->
            val resolvedParentName = when {
                parentName.isNotBlank() -> parentName
                dto.parentId != null && parentMap.containsKey(dto.parentId) -> parentMap[dto.parentId]!!
                else -> ""
            }

            Child(
                id = dto.id,
                fullName = dto.fullName,
                daycareCenter = "Estancia Mariposa",
                group = GroupEnum.P,
                parentName = resolvedParentName,
                birthdate = dto.birthDate ?: "Sin fecha registrada",
                allergies = if (dto.allergies.isNotEmpty()) dto.allergies.joinToString(", ") else "Sin alergias registradas",
                profileCompletedAt = dto.profileCompletedAt
            )
        }
    }

    override suspend fun submitProfile(childId: String, birthDate: String, allergies: List<String>): ChildDto {
        val body = ChildProfilePatchBody(birthDate = birthDate, allergies = allergies)
        return auth.authed { api.patch<ChildProfilePatchBody, ChildDto>("/children/$childId/profile", body, it) }
    }
}
