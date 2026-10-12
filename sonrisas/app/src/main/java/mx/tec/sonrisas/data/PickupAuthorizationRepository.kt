package mx.tec.sonrisas.data

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mx.tec.sonrisas.data.local.dao.PickupAuthorizationDao
import mx.tec.sonrisas.data.local.toDomain
import mx.tec.sonrisas.data.local.toEntity
import mx.tec.sonrisas.domain.PickupAuthorization

interface PickupAuthorizationRepository {
    fun observeTodayByChild(childId: String, date: LocalDate = LocalDate.now()): Flow<PickupAuthorization?>
    fun observeAllByChild(childId: String): Flow<List<PickupAuthorization>>
    suspend fun save(authorization: PickupAuthorization)
    suspend fun delete(authorization: PickupAuthorization)
}

@Singleton
class PickupAuthorizationRepositoryImpl @Inject constructor(
    private val dao: PickupAuthorizationDao
) : PickupAuthorizationRepository {

    override fun observeTodayByChild(childId: String, date: LocalDate): Flow<PickupAuthorization?> =
        dao.observeTodayByChild(childId, date).map { row -> row?.toDomain() }

    override fun observeAllByChild(childId: String): Flow<List<PickupAuthorization>> =
        dao.observeAllByChild(childId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun save(authorization: PickupAuthorization) {
        dao.save(authorization.toEntity())
    }

    override suspend fun delete(authorization: PickupAuthorization) {
        dao.delete(authorization.toEntity())
    }
}
