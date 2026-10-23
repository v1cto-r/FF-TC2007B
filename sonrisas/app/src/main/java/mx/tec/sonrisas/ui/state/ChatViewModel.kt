package mx.tec.sonrisas.ui.state

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.ChatRepository
import mx.tec.sonrisas.data.SesionRepository
import mx.tec.sonrisas.domain.Message

/**
 * Peer chats, which have no child: teacher↔admin (chat id = the teacher's user id) and admin↔ORG
 * (chat id = the admin's user id). Same flow as the parent's chat, without weekly hiding or deleting.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sesionRepository: SesionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    var estadoMensajes by mutableStateOf<UiState<List<Message>>>(UiState.Cargando)
        private set

    /** Last message of each chat passed to [observarUltimosMensajes], for the chat lists. */
    var ultimosMensajes by mutableStateOf<Map<String, Message>>(emptyMap())
        private set

    private var ultimosJob: Job? = null

    fun observarUltimosMensajes(chatIds: List<String>) {
        ultimosJob?.cancel()
        if (chatIds.isEmpty()) {
            ultimosMensajes = emptyMap()
            return
        }
        ultimosJob = viewModelScope.launch {
            combine(chatIds.map { id -> chatRepository.observeMessages(id).map { id to it.lastOrNull() } }) { pares ->
                pares.mapNotNull { (id, ultimo) -> ultimo?.let { id to it } }.toMap()
            }.collect { ultimosMensajes = it }
        }
    }

    fun cargarMensajes(chatId: String) {
        viewModelScope.launch {
            estadoMensajes = UiState.Cargando
            try {
                launch {
                    try {
                        chatRepository.refresh(chatId)
                    } catch (e: Exception) {
                        if (estadoMensajes !is UiState.Exito) estadoMensajes = UiState.Error("No pudimos cargar la conversación")
                    }
                }
                chatRepository.observeMessages(chatId).collect { mensajes ->
                    estadoMensajes = if (mensajes.isEmpty()) UiState.Vacio else UiState.Exito(mensajes)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                estadoMensajes = UiState.Error("Error al cargar chat: ${e.message}")
            }
        }
    }

    fun enviarMensaje(chatId: String, texto: String, esAviso: Boolean) {
        viewModelScope.launch {
            try {
                val autor = sesionRepository.sesion.first()?.id.orEmpty()
                chatRepository.send(
                    Message(chatId = chatId, authorId = autor, content = texto, sentAt = LocalDateTime.now(), isAviso = esAviso)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun enviarFoto(chatId: String, uri: Uri) {
        viewModelScope.launch {
            val foto = context.leerYBorrarFoto(uri) ?: return@launch
            val autor = sesionRepository.sesion.first()?.id.orEmpty()
            try {
                chatRepository.sendPhoto(chatId, autor, foto.first, foto.second)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
