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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.ChatRepository
import mx.tec.sonrisas.data.ChildRepository
import mx.tec.sonrisas.data.DailyLogRepository
import mx.tec.sonrisas.data.NoticeRepository
import mx.tec.sonrisas.data.SesionRepository
import mx.tec.sonrisas.domain.ActivityRecord
import mx.tec.sonrisas.domain.Child
import mx.tec.sonrisas.domain.DailyLog
import mx.tec.sonrisas.domain.GroupEnum
import mx.tec.sonrisas.domain.Message
import mx.tec.sonrisas.domain.Notice

data class NinoConEstado(
    val child: Child,
    val presente: Boolean,
    val dailyLog: DailyLog? = null,
    val activities: List<ActivityRecord> = emptyList()
)

data class ConversacionUi(val child: Child, val ultimoMensaje: Message?)

@HiltViewModel
class MaestraViewModel @Inject constructor(
    private val childRepository: ChildRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val noticeRepository: NoticeRepository,
    private val chatRepository: ChatRepository,
    private val sesionRepository: SesionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    var estadoNinos by mutableStateOf<UiState<List<NinoConEstado>>>(UiState.Cargando)
        private set

    var estadoEventos by mutableStateOf<UiState<List<Notice>>>(UiState.Cargando)
        private set

    var estadoPerfilNino by mutableStateOf<UiState<NinoConEstado>>(UiState.Cargando)
        private set

    var estadoMensajes by mutableStateOf<UiState<List<Message>>>(UiState.Cargando)
        private set

    var estadoConversaciones by mutableStateOf<UiState<List<ConversacionUi>>>(UiState.Cargando)
        private set

    var presentes by mutableStateOf(0)
        private set
    var total by mutableStateOf(0)
        private set
    var grupo by mutableStateOf<GroupEnum?>(null)
        private set

    private fun observarEstadoDeHoy(child: Child): Flow<NinoConEstado> =
        dailyLogRepository.observeToday(child.id).flatMapLatest { log ->
            if (log == null) {
                flowOf(NinoConEstado(child, presente = false, dailyLog = null, activities = emptyList()))
            } else {
                dailyLogRepository.observeActivities(log.id).map { actividades ->
                    NinoConEstado(child, presente = true, dailyLog = log, activities = actividades)
                }
            }
        }

    private val misNinos: Flow<List<NinoConEstado>> =
        childRepository.observeAll().flatMapLatest { children ->
            if (children.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(children.map { child -> observarEstadoDeHoy(child) }) { it.toList() }
            }
        }

    fun cargarNinos() {
        viewModelScope.launch {
            estadoNinos = UiState.Cargando
            try {
                misNinos.collect { lista ->
                    estadoNinos = if (lista.isEmpty()) UiState.Vacio else UiState.Exito(lista)
                    total = lista.size
                    presentes = lista.count { it.presente }
                    grupo = lista.firstOrNull()?.child?.group
                }
            } catch (e: Exception) {
                e.printStackTrace()
                estadoNinos = UiState.Error("Error al cargar niños: ${e.message}")
            }
        }
    }

    fun cargarInicio() {
        viewModelScope.launch {
            launch {
                try {
                    misNinos.collect { lista ->
                        total = lista.size
                        presentes = lista.count { it.presente }
                        grupo = lista.firstOrNull()?.child?.group
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            estadoEventos = UiState.Cargando
            try {
                noticeRepository.observeAll().collect { avisos ->
                    estadoEventos = if (avisos.isEmpty()) UiState.Vacio else UiState.Exito(avisos)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                estadoEventos = UiState.Error("Error al cargar avisos: ${e.message}")
            }
        }
    }

    fun cargarPerfilNino(ninoId: String) {
        viewModelScope.launch {
            estadoPerfilNino = UiState.Cargando
            try {
                childRepository.observeById(ninoId)
                    .flatMapLatest { child ->
                        if (child == null) {
                            flowOf(null)
                        } else {
                            observarEstadoDeHoy(child)
                        }
                    }
                    .collect { ninoConEstado ->
                        estadoPerfilNino = if (ninoConEstado != null) {
                            UiState.Exito(ninoConEstado)
                        } else {
                            UiState.Error("Niño no encontrado")
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                estadoPerfilNino = UiState.Error("Error al cargar perfil: ${e.message}")
            }
        }
    }

    fun cargarMensajes(chatId: String) {
        viewModelScope.launch {
            estadoMensajes = UiState.Cargando
            try {
                val yo = sesionRepository.sesion.first()?.id.orEmpty()
                launch { refrescarMensajes(chatId) }
                chatRepository.observeMessagesForTeacher(chatId, yo).collect { mensajes ->
                    estadoMensajes = if (mensajes.isEmpty()) UiState.Vacio else UiState.Exito(mensajes)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                estadoMensajes = UiState.Error("Error al cargar chat: ${e.message}")
            }
        }
    }

    private suspend fun refrescarMensajes(chatId: String) {
        try {
            chatRepository.refresh(chatId)
        } catch (e: Exception) {
            if (estadoMensajes !is UiState.Exito) estadoMensajes = UiState.Error("No pudimos cargar la conversación")
        }
    }

    fun enviarFoto(chatId: String, uri: Uri) {
        viewModelScope.launch {
            val foto = context.leerFoto(uri) ?: return@launch
            val autor = sesionRepository.sesion.first()?.id.orEmpty()
            try {
                chatRepository.sendPhoto(chatId, autor, foto.first, foto.second)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cargarConversaciones() {
        viewModelScope.launch {
            estadoConversaciones = UiState.Cargando
            try {
                childRepository.observeAll()
                    .flatMapLatest { children ->
                        if (children.isEmpty()) {
                            flowOf(emptyList())
                        } else {
                            combine(
                                children.map { child ->
                                    chatRepository.observeMessages(child.id).map { mensajes ->
                                        ConversacionUi(child, mensajes.lastOrNull())
                                    }
                                }
                            ) { it.toList() }
                        }
                    }
                    .collect { lista ->
                        val ordenada = lista.sortedByDescending { it.ultimoMensaje?.sentAt }
                        estadoConversaciones = if (ordenada.isEmpty()) UiState.Vacio else UiState.Exito(ordenada)
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                estadoConversaciones = UiState.Error("Error al cargar chats: ${e.message}")
            }
        }
    }

    fun enviarMensaje(chatId: String, texto: String, esAviso: Boolean = false) {
        viewModelScope.launch {
            try {
                val autor = sesionRepository.sesion.first()?.id ?: "maestra"
                chatRepository.send(
                    Message(
                        chatId = chatId,
                        authorId = autor,
                        content = texto,
                        sentAt = LocalDateTime.now(),
                        isAviso = esAviso
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cancelarSalida(ninoId: String, onListo: () -> Unit) {
        viewModelScope.launch {
            try {
                val log = dailyLogRepository.observeToday(ninoId).first()
                if (log != null) {
                    dailyLogRepository.save(log.copy(checkOut = null))
                }
                onListo()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun enviarActividadATodos(titulo: String, mensaje: String, onListo: () -> Unit) {
        viewModelScope.launch {
            val autor = sesionRepository.sesion.first()?.id ?: "maestra"
            val presentes = misNinos.first().filter { it.presente }
            presentes.forEach { nino ->
                chatRepository.send(
                    Message(
                        chatId = nino.child.id,
                        authorId = autor,
                        content = "$titulo: $mensaje",
                        sentAt = LocalDateTime.now()
                    )
                )
            }
            onListo()
        }
    }

    fun enviarAvisoATodos(titulo: String, mensaje: String, onListo: () -> Unit) {
        viewModelScope.launch {
            val autor = sesionRepository.sesion.first()?.id ?: "maestra"
            val todos = childRepository.observeAll().first()
            todos.forEach { child ->
                chatRepository.send(
                    Message(
                        chatId = child.id,
                        authorId = autor,
                        content = "$titulo: $mensaje",
                        sentAt = LocalDateTime.now(),
                        isAviso = true
                    )
                )
            }
            onListo()
        }
    }
}
