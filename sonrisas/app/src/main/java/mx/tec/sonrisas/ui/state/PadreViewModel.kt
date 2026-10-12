package mx.tec.sonrisas.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.ChatRepository
import mx.tec.sonrisas.data.ChildRepository
import mx.tec.sonrisas.data.DailyLogRepository
import mx.tec.sonrisas.data.NoticeRepository
import mx.tec.sonrisas.data.PickupAuthorizationRepository
import mx.tec.sonrisas.data.SesionRepository
import mx.tec.sonrisas.domain.Child
import mx.tec.sonrisas.domain.Message
import mx.tec.sonrisas.domain.PickupAuthorization
import mx.tec.sonrisas.domain.etiqueta
import mx.tec.sonrisas.ui.components.ActividadUi
import mx.tec.sonrisas.ui.components.TipoActividadUi

@HiltViewModel
class PadreViewModel @Inject constructor(
    private val childRepository: ChildRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val noticeRepository: NoticeRepository,
    private val chatRepository: ChatRepository,
    private val pickupAuthorizationRepository: PickupAuthorizationRepository,
    private val sesionRepository: SesionRepository
) : ViewModel() {

    var estadoActividades by mutableStateOf<UiState<List<ActividadUi>>>(UiState.Cargando)
        private set

    var estadoMensajes by mutableStateOf<UiState<List<Message>>>(UiState.Cargando)
        private set

    var ninos by mutableStateOf<List<Child>>(emptyList())
        private set

    var ninoSeleccionadoIndex by mutableStateOf(0)
        private set

    var autorizacionHoy by mutableStateOf<PickupAuthorization?>(null)
        private set

    val ninoActualId: String?
        get() = ninos.getOrNull(ninoSeleccionadoIndex)?.id

    val childActual: Child?
        get() = ninos.getOrNull(ninoSeleccionadoIndex)

    private var chatIdActual: String = ""

    fun cargarInicio() {
        viewModelScope.launch {
            estadoActividades = UiState.Cargando
            try {
                val nombrePadre = sesionRepository.sesion.first()?.nombre
                childRepository.observeAll()
                    .map { hijos -> hijos.filter { it.parentName == nombrePadre } }
                    .flatMapLatest { hijos ->
                        ninos = hijos
                        if (ninoSeleccionadoIndex >= hijos.size) ninoSeleccionadoIndex = 0
                        val child = hijos.getOrNull(ninoSeleccionadoIndex)
                        if (child == null) {
                            flowOf(emptyList())
                        } else {
                            launch {
                                pickupAuthorizationRepository.observeTodayByChild(child.id).collect { auth ->
                                    autorizacionHoy = auth
                                }
                            }

                            dailyLogRepository.observeToday(child.id).flatMapLatest { log ->
                                if (log == null) {
                                    flowOf(emptyList())
                                } else {
                                    dailyLogRepository.observeActivities(log.id).map { actividades ->
                                        val tarjetas = mutableListOf<ActividadUi>()
                                        log.stay?.let { stay ->
                                            tarjetas.add(
                                                ActividadUi(
                                                    tipo = TipoActividadUi.Comida,
                                                    titulo = "Alimentación de hoy",
                                                    detalle = stay.feedingNotes ?: "Desayuno: ${stay.feeding.breakfast.etiqueta()}, Comida: ${stay.feeding.lunch.etiqueta()}"
                                                )
                                            )
                                        }
                                        actividades.forEach { actividad ->
                                            tarjetas.add(
                                                ActividadUi(
                                                    tipo = TipoActividadUi.Actividad,
                                                    titulo = actividad.name,
                                                    detalle = "Actividad registrada hoy"
                                                )
                                            )
                                        }
                                        tarjetas
                                    }
                                }
                            }
                        }
                    }
                    .collect { lista ->
                        estadoActividades = if (lista.isEmpty()) UiState.Vacio else UiState.Exito(lista)
                    }
            } catch (e: Exception) {
                estadoActividades = UiState.Error("Error al cargar actividades")
            }
        }
    }

    fun cargarAutorizacionHoy(childId: String) {
        viewModelScope.launch {
            try {
                pickupAuthorizationRepository.observeTodayByChild(childId).collect { auth ->
                    autorizacionHoy = auth
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun guardarAutorizacionTercero(autorizacion: PickupAuthorization, onListo: () -> Unit) {
        viewModelScope.launch {
            try {
                pickupAuthorizationRepository.save(autorizacion)
                autorizacionHoy = autorizacion
                onListo()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun seleccionarNino(indice: Int) {
        ninoSeleccionadoIndex = indice
        cargarInicio()
    }

    fun cargarMensajes(chatId: String) {
        chatIdActual = chatId
        viewModelScope.launch {
            estadoMensajes = UiState.Cargando
            try {
                chatRepository.observeMessagesForParent(chatId).collect { mensajes ->
                    estadoMensajes = if (mensajes.isEmpty()) UiState.Vacio else UiState.Exito(mensajes)
                }
            } catch (e: Exception) {
                estadoMensajes = UiState.Error("Error al cargar chat")
            }
        }
    }

    fun toggleImportant(messageId: String) {
        viewModelScope.launch {
            try {
                chatRepository.toggleImportant(messageId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                chatRepository.hideForParent(messageId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun enviarMensaje(texto: String, esAviso: Boolean = false) {
        viewModelScope.launch {
            try {
                val autor = sesionRepository.sesion.first()?.usuario ?: "padre"
                chatRepository.send(
                    Message(
                        chatId = chatIdActual,
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
}
