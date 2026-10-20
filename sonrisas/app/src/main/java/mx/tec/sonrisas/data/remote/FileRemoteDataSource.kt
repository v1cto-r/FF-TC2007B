package mx.tec.sonrisas.data.remote

import android.content.Context
import java.io.File
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

    suspend fun uploadChildDocument(childId: String, context: Context): String {
        val pdfFile = File(context.cacheDir, "acta_nacimiento.pdf")
        if (!pdfFile.exists() || pdfFile.length() == 0L) {
            pdfFile.writeBytes("%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj 2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj 3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R>>endobj\nxref\n0 4\n0000000000 65535 f \n0000000009 00000 n \n0000000057 00000 n \n0000000114 00000 n \ntrailer<</Size 4/Root 1 0 R>>\nstartxref\n190\n%%EOF".toByteArray())
        }
        val bytes = pdfFile.readBytes()
        val fileId = UUID.randomUUID().toString()

        val ticket: UploadTicketDto = auth.authed {
            api.post("/files", FileCreateBody(fileId, "CHILD_DOCUMENT", childId, "application/pdf", bytes.size, "acta_nacimiento.pdf"), it)
        }
        val put = Request.Builder().url(ticket.upload.url)
            .method(ticket.upload.method, bytes.toRequestBody())
            .apply { ticket.upload.headers.forEach { (k, v) -> header(k, v) } }
            .build()
        api.http.newCall(put).await().use { if (!it.isSuccessful) throw ApiException(it.code, "No se pudo subir el documento PDF") }
        auth.authed { api.send("POST", "/files/$fileId/complete", it) }

        val documentId = UUID.randomUUID().toString()
        auth.authed {
            api.put<DocumentInBody, DocumentOutDto>(
                "/documents/$documentId",
                DocumentInBody(childId = childId, fileId = fileId, title = "Acta de nacimiento"),
                it
            )
        }
        return fileId
    }

    suspend fun downloadUrl(fileId: String): String {
        urls[fileId]?.takeIf { OffsetDateTime.parse(it.expiresAt).isAfter(OffsetDateTime.now().plusMinutes(1)) }
            ?.let { return it.url }
        val fresh = auth.authed { api.get<FileDownloadDto>("/files/$fileId", it) }
        urls[fileId] = fresh
        return fresh.url
    }
}
