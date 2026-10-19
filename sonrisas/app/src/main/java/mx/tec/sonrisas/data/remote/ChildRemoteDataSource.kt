package mx.tec.sonrisas.data.remote

import javax.inject.Inject
import mx.tec.sonrisas.data.AuthRepository
import mx.tec.sonrisas.domain.Child
import mx.tec.sonrisas.domain.GroupEnum

interface ChildRemoteDataSource {
    suspend fun fetchAll(parentName: String): List<Child>
    suspend fun save(child: Child)
    suspend fun delete(child: Child)
}

// NetworkMonitor.isOnline() always returns false.
// save/delete: TODO, never called while NetworkMonitor.isOnline() is false
class ChildRemoteDataSourceImpl @Inject constructor(
    private val api: ApiClient,
    private val auth: AuthRepository
) : ChildRemoteDataSource {
    override suspend fun fetchAll(parentName: String): List<Child> =
        auth.authed { api.get<List<ChildDto>>("/children", it) }.map {
            Child(id = it.id, fullName = it.fullName, daycareCenter = "Estancia Mariposa", group = GroupEnum.P, parentName = parentName)
        }

    override suspend fun save(child: Child): Unit = TODO("Pending: API integration")
    override suspend fun delete(child: Child): Unit = TODO("Pending: API integration")
}
