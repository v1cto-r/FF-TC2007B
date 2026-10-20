package mx.tec.sonrisas.data.remote

import android.net.Uri
import java.time.Instant
import javax.inject.Inject
import mx.tec.sonrisas.data.AuthRepository
import mx.tec.sonrisas.domain.Message

interface MessageRemoteDataSource {
    suspend fun list(chatId: String, sinceMs: Long?): List<Message>

    /**
     * API: DELETE /messages/{message_id} -> 204.
     * Soft delete (status = DELETED), hidden for both sides.
     * 403 if the caller is not the child's parent, 404 if it doesn't exist.
     */
    suspend fun delete(messageId: String)
}

class MessageRemoteDataSourceImpl @Inject constructor(
    private val api: ApiClient,
    private val auth: AuthRepository
) : MessageRemoteDataSource {

    override suspend fun list(chatId: String, sinceMs: Long?): List<Message> {
        val since = sinceMs?.let { "&since=" + Uri.encode(Instant.ofEpochMilli(it).toString()) }.orEmpty()
        return auth.authed { api.get<List<MessageDto>>("/messages?chat_id=$chatId$since", it) }.map { it.toDomain() }
    }

    override suspend fun delete(messageId: String) {
        auth.authed { api.delete("/messages/$messageId", it) }
    }
}
