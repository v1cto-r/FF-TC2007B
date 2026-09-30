package mx.tec.sonrisas.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mx.tec.sonrisas.data.local.dao.MessageDao
import mx.tec.sonrisas.data.local.toDomain
import mx.tec.sonrisas.data.local.toEntity
import mx.tec.sonrisas.data.remote.MessageRemoteDataSource
import mx.tec.sonrisas.domain.Message

/**
 * Stores both historical messages and the ones sent without internet: they
 * all live in the same table, told apart by `sent` and `synced`. The future
 * sync worker uploads whatever is still unsynced.
 */
interface ChatRepository {
    fun observeMessages(chatId: String): Flow<List<Message>>
    suspend fun send(message: Message)
}

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val dao: MessageDao,
    private val remote: MessageRemoteDataSource,
    private val networkMonitor: NetworkMonitor
) : ChatRepository {

    override fun observeMessages(chatId: String): Flow<List<Message>> =
        dao.observeByChat(chatId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun send(message: Message) {
        if (networkMonitor.isOnline()) {
            remote.send(message)
            dao.save(message.copy(sent = true, synced = true).toEntity())
        } else {
            // Stays in Room marked as pending; the worker uploads it later.
            dao.save(message.toEntity())
        }
    }
}
