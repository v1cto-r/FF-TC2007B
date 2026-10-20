package mx.tec.sonrisas.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mx.tec.sonrisas.data.local.dao.NoticeDao
import mx.tec.sonrisas.data.local.toDomain
import mx.tec.sonrisas.data.local.toEntity
import mx.tec.sonrisas.data.remote.NoticeRemoteDataSource
import mx.tec.sonrisas.domain.Notice

/**
 * Notices are posted from the admin / org screens straight to the API; here they are only read.
 * Room keeps a copy so the list shows offline.
 */
interface NoticeRepository {
    fun observeAll(): Flow<List<Notice>>
    fun observeById(id: String): Flow<Notice?>
    suspend fun refreshFromServer()
}

@Singleton
class NoticeRepositoryImpl @Inject constructor(
    private val dao: NoticeDao,
    private val remote: NoticeRemoteDataSource
) : NoticeRepository {

    override fun observeAll(): Flow<List<Notice>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeById(id: String): Flow<Notice?> =
        dao.observeById(id).map { row -> row?.toDomain() }

    override suspend fun refreshFromServer() {
        dao.replaceAll(remote.fetchAll().map { it.toEntity() })
    }
}
