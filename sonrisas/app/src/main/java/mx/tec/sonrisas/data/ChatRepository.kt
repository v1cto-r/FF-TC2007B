package mx.tec.sonrisas.data

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
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
 * Almacena y gestiona los mensajes de chat.
 *
 * Reglas de visibilidad:
 * 1. Para la Maestra: Los mensajes enviados por la maestra que NO fueron marcados como 'isAviso'
 *    se vuelven invisibles en el lado de la maestra al finalizar la semana actual (permanecen en BD).
 * 2. Para el Padre: Los mensajes de las maestras NO se borran solos (disponibles 24/7).
 * 3. 'isAviso / Importante': Se puede fijar/marcar mediante pulsación larga (estilo WhatsApp)
 *    y jamás se oculta en el lado de la maestra por el paso de la semana.
 * 4. Eliminar mensaje: solo el padre puede hacerlo (regla de la API). Es un soft delete
 *    compartido: el mensaje se marca como 'deleted' y desaparece para AMBOS lados, pero
 *    permanece en BD. Si no hay conexión queda 'deletePending' hasta avisar al servidor.
 */
interface ChatRepository {
    fun observeMessagesForTeacher(chatId: String): Flow<List<Message>>
    fun observeMessagesForParent(chatId: String): Flow<List<Message>>
    fun observeMessages(chatId: String): Flow<List<Message>>
    suspend fun send(message: Message)
    suspend fun toggleImportant(messageId: String)
    suspend fun deleteMessage(messageId: String)
}

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val dao: MessageDao,
    private val remote: MessageRemoteDataSource,
    private val networkMonitor: NetworkMonitor,
) : ChatRepository {

    override fun observeMessages(chatId: String): Flow<List<Message>> =
        observeMessagesForTeacher(chatId)

    override fun observeMessagesForTeacher(chatId: String): Flow<List<Message>> =
        dao.observeByChat(chatId).map { rows ->
            val domainMessages = rows.map { it.toDomain() }.filter { !it.deleted }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                filtrarMensajesInvisiblesMaestra(chatId, domainMessages)
            } else {
                domainMessages
            }
        }

    override fun observeMessagesForParent(chatId: String): Flow<List<Message>> =
        dao.observeByChat(chatId).map { rows ->
            rows.map { it.toDomain() }.filter { !it.deleted }
        }

    override suspend fun send(message: Message) {
        if (networkMonitor.isOnline()) {
            remote.send(message)
            dao.save(message.copy(sent = true, synced = true).toEntity())
        } else {
            // Stays in Room marked as pending; the worker uploads it later.
            dao.save(message.toEntity())
        }
    }

    override suspend fun toggleImportant(messageId: String) {
        val entity = dao.getById(messageId) ?: return
        dao.save(entity.copy(isAviso = !entity.isAviso))
    }

    override suspend fun deleteMessage(messageId: String) {
        val entity = dao.getById(messageId) ?: return
        if (networkMonitor.isOnline()) {
            remote.delete(messageId)
            dao.save(entity.copy(deleted = true, deletePending = false))
        } else {
            // Hidden right away on this device; the server is told later.
            dao.save(entity.copy(deleted = true, deletePending = true))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun filtrarMensajesInvisiblesMaestra(chatId: String, mensajes: List<Message>): List<Message> {
        val esChatAdmin = chatId.startsWith("admin_", ignoreCase = true)
        if (esChatAdmin) {
            return mensajes
        }

        // Lunes a las 00:00:00 de la semana actual
        val inicioSemanaActual = LocalDateTime.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .withHour(0).withMinute(0).withSecond(0).withNano(0)

        return mensajes.filter { mensaje ->
            val esDeMaestra = mensaje.authorId.contains("maestra", ignoreCase = true)
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