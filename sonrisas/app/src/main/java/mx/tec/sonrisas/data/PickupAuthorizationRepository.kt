package mx.tec.sonrisas.data

import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mx.tec.sonrisas.data.local.dao.PickupAuthorizationDao
import mx.tec.sonrisas.data.local.toDomain
import mx.tec.sonrisas.data.local.toEntity
import mx.tec.sonrisas.data.remote.ApiException
import mx.tec.sonrisas.data.remote.PickupRemoteDataSource
import mx.tec.sonrisas.domain.PickupAuthorization
import mx.tec.sonrisas.domain.resolveFor

sealed interface PickupSaveResult {
    data class Saved(val authorization: PickupAuthorization) : PickupSaveResult

    /** Another authorization already covers one of those days; saving again with replace = true cancels it. */
    data class Conflict(val conflictingIds: List<String>) : PickupSaveResult

    data object Offline : PickupSaveResult
}

/**
 * Unlike daily logs, a parent's writes go straight to the API instead of waiting in Room: the parent
 * has to know the school actually got it, and an overlap needs their answer. Room is only a copy
 * of the server, so the teacher still sees today's authorization offline.
 */
interface PickupAuthorizationRepository {
    fun observeAllByChild(childId: String): Flow<List<PickupAuthorization>>
    fun observeForChildOn(childId: String, date: LocalDate = LocalDate.now()): Flow<PickupAuthorization?>
    suspend fun save(authorization: PickupAuthorization, replace: Boolean = false): PickupSaveResult
    suspend fun cancel(authorization: PickupAuthorization)
    suspend fun refreshFromServer()
}

@Singleton
class PickupAuthorizationRepositoryImpl @Inject constructor(
    private val dao: PickupAuthorizationDao,
    private val remote: PickupRemoteDataSource
) : PickupAuthorizationRepository {

    override fun observeAllByChild(childId: String): Flow<List<PickupAuthorization>> =
        dao.observeAllByChild(childId).map { rows -> rows.map { it.toDomain() } }

    override fun observeForChildOn(childId: String, date: LocalDate): Flow<PickupAuthorization?> =
        observeAllByChild(childId).map { it.resolveFor(date) }

    override suspend fun save(authorization: PickupAuthorization, replace: Boolean): PickupSaveResult {
        val saved = try {
            remote.save(authorization, replace)
        } catch (e: IOException) {
            return PickupSaveResult.Offline
        } catch (e: ApiException) {
            if (e.code != 409) throw e
            return PickupSaveResult.Conflict(UUID_PATTERN.findAll(e.detail.orEmpty()).map { it.value }.toList())
        }
        // replace = true cancelled the overlapping ones on the server
        if (replace) refreshFromServer() else dao.save(saved.toEntity())
        return PickupSaveResult.Saved(saved)
    }

    override suspend fun cancel(authorization: PickupAuthorization) {
        remote.cancel(authorization.id)
        dao.deleteById(authorization.id)
    }

    override suspend fun refreshFromServer() {
        dao.replaceAll(remote.fetchAll().map { it.toEntity() })
    }

    private companion object {
        val UUID_PATTERN = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    }
}
