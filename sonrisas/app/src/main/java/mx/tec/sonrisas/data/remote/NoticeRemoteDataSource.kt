package mx.tec.sonrisas.data.remote

import javax.inject.Inject
import mx.tec.sonrisas.domain.Notice

interface NoticeRemoteDataSource {
    suspend fun save(notice: Notice)
    suspend fun delete(notice: Notice)
}

// TODO: implement once the API exists. Never called today because
// NetworkMonitor.isOnline() always returns false.
class NoticeRemoteDataSourceImpl @Inject constructor() : NoticeRemoteDataSource {
    override suspend fun save(notice: Notice): Unit = TODO("Pending: API integration")
    override suspend fun delete(notice: Notice): Unit = TODO("Pending: API integration")
}
