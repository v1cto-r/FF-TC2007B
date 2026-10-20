package mx.tec.sonrisas.data.remote

import javax.inject.Inject
import mx.tec.sonrisas.data.AuthRepository
import mx.tec.sonrisas.domain.PickupAuthorization

interface PickupRemoteDataSource {
    /** Every live authorization of the children the user can see (past weeks left out). */
    suspend fun fetchAll(): List<PickupAuthorization>

    /** Throws ApiException(409) when it overlaps another one and [replace] is false. */
    suspend fun save(authorization: PickupAuthorization, replace: Boolean): PickupAuthorization

    suspend fun cancel(id: String)
}

class PickupRemoteDataSourceImpl @Inject constructor(
    private val api: ApiClient,
    private val auth: AuthRepository
) : PickupRemoteDataSource {

    override suspend fun fetchAll(): List<PickupAuthorization> =
        auth.authed { api.get<List<PickupOutDto>>("/pickup-authorizations", it) }.map { it.toDomain() }

    override suspend fun save(authorization: PickupAuthorization, replace: Boolean): PickupAuthorization =
        auth.authed {
            api.put<PickupInBody, PickupOutDto>(
                "/pickup-authorizations/${authorization.id}?replace=$replace", authorization.toBody(), it
            )
        }.toDomain()

    override suspend fun cancel(id: String) {
        try {
            auth.authed { api.delete("/pickup-authorizations/$id", it) }
        } catch (e: ApiException) {
            if (e.code != 404) throw e // already cancelled
        }
    }
}
