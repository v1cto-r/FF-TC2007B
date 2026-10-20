package mx.tec.sonrisas.data.remote

import java.time.OffsetDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import mx.tec.sonrisas.data.AuthRepository
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class FileRemoteDataSource @Inject constructor(
    private val api: ApiClient,
    private val auth: AuthRepository
) {
    private val urls = mutableMapOf<String, FileDownloadDto>()

    suspend fun uploadPhoto(chatId: String, contentType: String, bytes: ByteArray): String {
        val id = UUID.randomUUID().toString()
        val ticket: UploadTicketDto = auth.authed {
            api.post("/files", FileCreateBody(id, "MESSAGE_ATTACHMENT", chatId, contentType, bytes.size), it)
        }
        val put = Request.Builder().url(ticket.upload.url)
            .method(ticket.upload.method, bytes.toRequestBody())
            .apply { ticket.upload.headers.forEach { (k, v) -> header(k, v) } }
            .build()
        api.http.newCall(put).await().use { if (!it.isSuccessful) throw ApiException(it.code, "No se pudo subir la foto") }
        auth.authed { api.send("POST", "/files/$id/complete", it) }
        return id
    }

    suspend fun downloadUrl(fileId: String): String {
        urls[fileId]?.takeIf { OffsetDateTime.parse(it.expiresAt).isAfter(OffsetDateTime.now().plusMinutes(1)) }
            ?.let { return it.url }
        val fresh = auth.authed { api.get<FileDownloadDto>("/files/$fileId", it) }
        urls[fileId] = fresh
        return fresh.url
    }
}
