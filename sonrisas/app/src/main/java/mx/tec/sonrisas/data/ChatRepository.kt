package mx.tec.sonrisas.data

import android.os.Build
import androidx.annotation.RequiresApi
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mx.tec.sonrisas.data.chat.ChatSocket
import mx.tec.sonrisas.data.local.dao.MessageDao
import mx.tec.sonrisas.data.local.saveFromServer
import mx.tec.sonrisas.data.local.toDomain
import mx.tec.sonrisas.data.local.toEntity
import mx.tec.sonrisas.data.remote.FileRemoteDataSource
import mx.tec.sonrisas.data.remote.MessageRemoteDataSource
import mx.tec.sonrisas.domain.Message

/**
 * Almacena y gestiona los mensajes de chat.
 *
 * Reglas de visibilidad:
 * 1. Para la Maestra: Los mensajes enviados por la maestra que NO fueron marcados como 'isAviso'
 *    se vuelven invisibles en el lado de la maestra al finalizar la semana actual (permanecen en BD).
 * 2. Para el Padre: Los mensajes de las maestras NO se borran solos (disponibles 24/7).
 *    Solo aplica a chats de niño: las pantallas maestra↔admin deben usar observeMessagesForParent
 *    (sin filtro) y solo el chat de un niño usa observeMessagesForTeacher.
 * 3. 'isAviso': se decide al enviar (el servidor nunca edita mensajes) y jamás se oculta en el
 *    lado de la maestra por el paso de la semana.
 * 4. Eliminar mensaje: solo el padre (o el admin de la estancia en chats de niño) puede hacerlo. Es un soft delete
 *    compartido: el mensaje se marca como 'deleted' y desaparece para AMBOS lados, pero
 *    permanece en BD. Si no hay conexión queda 'deletePending' hasta avisar al servidor.
 */
interface ChatRepository {
    fun observeMessagesForTeacher(chatId: String, currentUserId: String): Flow<List<Message>>
    fun observeMessagesForParent(chatId: String): Flow<List<Message>>
    fun observeMessages(chatId: String): Flow<List<Message>>
    suspend fun refresh(chatId: String)
    suspend fun send(message: Message)
    suspend fun sendPhoto(chatId: String, authorId: String, contentType: String, bytes: ByteArray)
    suspend fun photoUrl(fileId: String): String
    suspend fun deleteMessage(messageId: String)
}

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val dao: MessageDao,
    private val remote: MessageRemoteDataSource,
    private val files: FileRemoteDataSource,
    private val socket: ChatSocket
) : ChatRepository {

    override fun observeMessages(chatId: String): Flow<List<Message>> = observeMessagesForParent(chatId)

    override fun observeMessagesForTeacher(chatId: String, currentUserId: String): Flow<List<Message>> =
        dao.observeByChat(chatId).map { rows ->
            val domainMessages = rows.map { it.toDomain() }.filter { !it.deleted }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                filtrarMensajesInvisiblesMaestra(currentUserId, domainMessages)
            } else {
                domainMessages
            }
        }

    override fun observeMessagesForParent(chatId: String): Flow<List<Message>> =
        dao.observeByChat(chatId).map { rows ->
            rows.map { it.toDomain() }.filter { !it.deleted }
        }

    override suspend fun refresh(chatId: String) {
        val all = remote.list(chatId, null)
        dao.saveFromServer(all)
        dao.markDeletedExcept(chatId, all.map { it.id })
    }

    override suspend fun send(message: Message) {
        dao.save(message.toEntity())
        socket.send(message)
    }

    override suspend fun sendPhoto(chatId: String, authorId: String, contentType: String, bytes: ByteArray) {
        val fileId = files.uploadPhoto(chatId, contentType, bytes)
        send(
            Message(
                chatId = chatId,
                authorId = authorId,
                content = "",
                sentAt = LocalDateTime.now(),
                attachmentFileId = fileId
            )
        )
    }

    override suspend fun photoUrl(fileId: String): String = files.downloadUrl(fileId)

    override suspend fun deleteMessage(messageId: String) {
        val entity = dao.getById(messageId) ?: return
        val told = try {
            remote.delete(messageId)
            true
        } catch (e: IOException) {
            false
        }
        dao.save(entity.copy(deleted = true, deletePending = !told))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun filtrarMensajesInvisiblesMaestra(currentUserId: String, mensajes: List<Message>): List<Message> {
        // Lunes a las 00:00:00 de la semana actual
        val inicioSemanaActual = LocalDateTime.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .withHour(0).withMinute(0).withSecond(0).withNano(0)

        return mensajes.filter { mensaje ->
            val esDeMaestra = mensaje.authorId == currentUserId
            val esAvisoOImportante = mensaje.isAviso

            if (esDeMaestra && !esAvisoOImportante) {
                // En el lado de la maestra, los mensajes normales de semanas pasadas se vuelven invisibles
                !mensaje.sentAt.isBefore(inicioSemanaActual)
            } else {
                // Mensajes de Padres, Admin o Importantes permanecen visibles
                true
            }
        }
    }
}