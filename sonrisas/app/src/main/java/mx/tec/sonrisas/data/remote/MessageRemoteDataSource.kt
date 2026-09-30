package mx.tec.sonrisas.data.remote

import javax.inject.Inject
import mx.tec.sonrisas.domain.Message

interface MessageRemoteDataSource {
    suspend fun send(message: Message)
}

// TODO: implement once the API exists. Never called today because
// NetworkMonitor.isOnline() always returns false.
class MessageRemoteDataSourceImpl @Inject constructor() : MessageRemoteDataSource {
    override suspend fun send(message: Message): Unit = TODO("Pending: API integration")
}
