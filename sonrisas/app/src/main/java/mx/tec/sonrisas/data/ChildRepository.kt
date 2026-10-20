package mx.tec.sonrisas.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mx.tec.sonrisas.data.local.dao.ChildDao
import mx.tec.sonrisas.data.local.toDomain
import mx.tec.sonrisas.data.local.toEntity
import mx.tec.sonrisas.data.remote.ChildRemoteDataSource
import mx.tec.sonrisas.domain.Child

/**
 * The single door to a child's data. The UI only knows this interface: it
 * doesn't know (or care) whether the data came from Room or the API.
 */
interface ChildRepository {
    fun observeAll(): Flow<List<Child>>
    fun observeById(id: String): Flow<Child?>
    suspend fun save(child: Child)
    suspend fun delete(child: Child)
    suspend fun refreshFromServer(parentName: String)
}

@Singleton
class ChildRepositoryImpl @Inject constructor(
    private val dao: ChildDao,
    private val remote: ChildRemoteDataSource
) : ChildRepository {

    override fun observeAll(): Flow<List<Child>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeById(id: String): Flow<Child?> =
        dao.observeById(id).map { row -> row?.toDomain() }

    // The server is changed through its own calls (the parent's profile goes in submitProfile);
    // this only updates the local copy so the screens show it right away
    override suspend fun save(child: Child) {
        dao.save(child.toEntity())
    }

    override suspend fun delete(child: Child) {
        dao.delete(child.toEntity())
    }

    override suspend fun refreshFromServer(parentName: String) {
        val children = remote.fetchAll(parentName)
        dao.clear()
        children.forEach { dao.save(it.toEntity()) }
    }
}
