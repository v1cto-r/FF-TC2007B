package mx.tec.sonrisas.data.remote

import javax.inject.Inject
import mx.tec.sonrisas.data.AuthRepository

interface EstanciaRemoteDataSource {
    suspend fun fetchEstancias(): List<EstanciaOut>
    suspend fun getEstancia(id: String): EstanciaOut
    suspend fun putEstancia(id: String, estancia: EstanciaIn): EstanciaOut
    suspend fun archiveEstancia(id: String): EstanciaOut
    suspend fun unarchiveEstancia(id: String): EstanciaOut
    suspend fun deleteEstancia(id: String)
    suspend fun fetchUsers(role: String? = null, estanciaId: String? = null): List<UserOut>
    suspend fun createUser(user: UserCreate): UserCreated
    suspend fun resetPassword(userId: String): PasswordResetDto
    suspend fun fetchSalones(): List<SalonOut>
    suspend fun fetchChildren(): List<ChildOut>
}

class EstanciaRemoteDataSourceImpl @Inject constructor(
    private val api: ApiClient,
    private val auth: AuthRepository
) : EstanciaRemoteDataSource {
    override suspend fun fetchEstancias(): List<EstanciaOut> =
        auth.authed { api.get<List<EstanciaOut>>("/estancias", it) }

    override suspend fun getEstancia(id: String): EstanciaOut =
        auth.authed { api.get<EstanciaOut>("/estancias/$id", it) }

    override suspend fun putEstancia(id: String, estancia: EstanciaIn): EstanciaOut =
        auth.authed {
            api.put<EstanciaIn, EstanciaOut>("/estancias/$id", estancia, it)
        }

    override suspend fun archiveEstancia(id: String): EstanciaOut =
        auth.authed { api.post<Unit, EstanciaOut>("/estancias/$id/archive", Unit, it) }

    override suspend fun unarchiveEstancia(id: String): EstanciaOut =
        auth.authed { api.post<Unit, EstanciaOut>("/estancias/$id/unarchive", Unit, it) }

    override suspend fun deleteEstancia(id: String): Unit =
        auth.authed { api.delete("/estancias/$id", it) }

    override suspend fun fetchUsers(role: String?, estanciaId: String?): List<UserOut> =
        auth.authed { token ->
            val query = buildString {
                if (role != null) append("role=$role&")
                if (estanciaId != null) append("estancia_id=$estanciaId&")
            }
            val path = if (query.isNotEmpty()) "/users?$query" else "/users"
            api.get<List<UserOut>>(path, token)
        }

    override suspend fun createUser(user: UserCreate): UserCreated =
        auth.authed { api.post<UserCreate, UserCreated>("/users", user, it) }

    override suspend fun resetPassword(userId: String): PasswordResetDto =
        auth.authed { api.post<Unit, PasswordResetDto>("/users/$userId/reset-password", Unit, it) }

    override suspend fun fetchSalones(): List<SalonOut> =
        auth.authed { api.get<List<SalonOut>>("/salones", it) }

    override suspend fun fetchChildren(): List<ChildOut> =
        auth.authed { api.get<List<ChildOut>>("/children", it) }
}
