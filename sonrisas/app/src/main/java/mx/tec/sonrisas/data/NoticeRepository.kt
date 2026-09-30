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

interface NoticeRepository {
    fun observeAll(): Flow<List<Notice>>
    fun observeById(id: String): Flow<Notice?>
    suspend fun save(notice: Notice)
    suspend fun delete(notice: Notice)
}

@Singleton
class NoticeRepositoryImpl @Inject constructor(
    private val dao: NoticeDao,
    private val remote: NoticeRemoteDataSource,
    private val networkMonitor: NetworkMonitor
) : NoticeRepository {

    override fun observeAll(): Flow<List<Notice>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeById(id: String): Flow<Notice?> =
        dao.observeById(id).map { row -> row?.toDomain() }

    override suspend fun save(notice: Notice) {
        if (networkMonitor.isOnline()) remote.save(notice) else dao.save(notice.toEntity())
    }

    override suspend fun delete(notice: Notice) {
        if (networkMonitor.isOnline()) remote.delete(notice) else dao.delete(notice.toEntity())
    }
}
