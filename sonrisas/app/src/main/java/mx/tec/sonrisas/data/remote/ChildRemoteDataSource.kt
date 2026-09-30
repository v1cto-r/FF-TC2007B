package mx.tec.sonrisas.data.remote

import javax.inject.Inject
import mx.tec.sonrisas.domain.Child

interface ChildRemoteDataSource {
    suspend fun save(child: Child)
    suspend fun delete(child: Child)
}

// TODO: implement once the API exists. Never called today because
// NetworkMonitor.isOnline() always returns false.
class ChildRemoteDataSourceImpl @Inject constructor() : ChildRemoteDataSource {
    override suspend fun save(child: Child): Unit = TODO("Pending: API integration")
    override suspend fun delete(child: Child): Unit = TODO("Pending: API integration")
}
