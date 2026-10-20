package mx.tec.sonrisas.data.chat

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.decodeFromJsonElement
import mx.tec.sonrisas.BuildConfig
import mx.tec.sonrisas.data.AuthRepository
import mx.tec.sonrisas.data.local.dao.ChildDao
import mx.tec.sonrisas.data.local.dao.MessageDao
import mx.tec.sonrisas.data.local.saveFromServer
import mx.tec.sonrisas.data.local.toDomain
import mx.tec.sonrisas.data.remote.ApiClient
import mx.tec.sonrisas.data.remote.MessageDto
import mx.tec.sonrisas.data.remote.MessageRemoteDataSource
import mx.tec.sonrisas.data.remote.toDomain
import mx.tec.sonrisas.data.remote.toUtcIso
import mx.tec.sonrisas.domain.Message
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

@Singleton
class ChatSocket @Inject constructor(
    private val api: ApiClient,
    private val auth: AuthRepository,
    private val json: Json,
    private val messages: MessageDao,
    private val children: ChildDao,
    private val remote: MessageRemoteDataSource
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loop: Job? = null
    private var socket: WebSocket? = null
    @Volatile private var ready = false

    fun connect() {
        if (loop?.isActive == true) return
        loop = scope.launch {
            var failures = 0
            while (isActive) {
                try {
                    session()
                    failures = 0
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    failures++
                }
                delay(minOf(1_000L shl minOf(failures, 5), 30_000L))
            }
        }
    }

    fun reconnectNow() {
        disconnect()
        connect()
    }

    fun disconnect() {
        loop?.cancel()
        loop = null
    }

    fun send(message: Message) {
        if (!ready) return
        socket?.send(
            buildJsonObject {
                put("type", "message")
                put("id", message.id)
                put("chat_id", message.chatId)
                put("content", message.content)
                put("sent_at", message.sentAt.toUtcIso())
                put("is_aviso", message.isAviso)
                put("attachment_file_id", message.attachmentFileId?.let(::JsonPrimitive) ?: JsonNull)
            }.toString()
        )
    }

    private suspend fun session() = coroutineScope {
        var token = auth.validSessionToken()
        val frames = Channel<String>(Channel.UNLIMITED)
        val closed = CompletableDeferred<Int>()
        val request = Request.Builder()
            .url(BuildConfig.API_BASE_URL.replaceFirst("http", "ws") + "/ws/chat")
            .header("Authorization", "Bearer $token")
            .build()
        val ws = api.http.newBuilder().readTimeout(0, TimeUnit.SECONDS).build().newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) { frames.trySend(text) }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                closed.complete(code)
                webSocket.close(code, null)
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { closed.complete(code) }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { closed.completeExceptionally(t) }
        })
        socket = ws

        val keepAlive = launch {
            while (true) {
                delay(30_000)
                ws.send("""{"type":"ping"}""")
                val fresh = auth.validSessionToken()
                if (fresh != token) {
                    token = fresh
                    ws.send(buildJsonObject { put("type", "auth"); put("token", fresh) }.toString())
                }
            }
        }
        val reader = launch { for (frame in frames) handle(frame) }
        try {
            // 4401: renew the token before reconnecting
            if (closed.await() == 4401) auth.validSessionToken()
        } finally {
            ready = false
            keepAlive.cancel()
            reader.cancel()
            ws.cancel()
            socket = null
        }
    }

    private suspend fun handle(text: String) {
        val frame = json.parseToJsonElement(text).jsonObject
        when (frame["type"]?.jsonPrimitive?.content) {
            "ready" -> {
                ready = true
                catchUp()
            }
            "message", "ack" -> messages.saveFromServer(listOf(frame.message().toDomain()))
            "deleted" -> messages.markDeleted(frame.getValue("id").jsonPrimitive.content)
            "error" -> {
                val id = frame["id"]?.jsonPrimitive?.contentOrNull ?: return
                // 429 is a rate limit, so the message stays queued
                if (frame.getValue("status").jsonPrimitive.int != 429) {
                    messages.setError(id, frame["detail"]?.jsonPrimitive?.contentOrNull ?: "No se pudo enviar")
                }
            }
        }
    }

    private fun JsonObject.message(): MessageDto = json.decodeFromJsonElement(getValue("message"))

    private suspend fun catchUp() {
        children.observeAll().first().forEach { child ->
            messages.saveFromServer(remote.list(child.id, messages.newestCreatedAt(child.id)))
        }
        messages.getPendingDeletes().forEach {
            remote.delete(it.id)
            messages.save(it.copy(deletePending = false))
        }
        messages.getUnsent().forEach { send(it.toDomain()) }
    }
}
