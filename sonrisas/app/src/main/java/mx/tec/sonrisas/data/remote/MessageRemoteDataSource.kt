package mx.tec.sonrisas.data.remote

import javax.inject.Inject
import mx.tec.sonrisas.domain.Message

interface MessageRemoteDataSource {
    suspend fun send(message: Message)

    /**
     * API: DELETE /messages/{message_id} -> 204.
     * Soft delete (status = DELETED), hidden for both sides.
     * 403 if the caller is not the child's parent, 404 if it doesn't exist.
     */
    suspend fun delete(messageId: String)
}

// TODO: implement once the API integration exists. Never called today because
// NetworkMonitor.isOnline() always returns false.
class MessageRemoteDataSourceImpl @Inject constructor() : MessageRemoteDataSource {
    override suspend fun send(message: Message): Unit = TODO("Pending: API integration")
    override suspend fun delete(messageId: String): Unit = TODO("Pending: API integration")
}