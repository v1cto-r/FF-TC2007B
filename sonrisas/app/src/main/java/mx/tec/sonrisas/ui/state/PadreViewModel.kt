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
import java.time.OffsetDateTime
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
import mx.tec.sonrisas.data.remote.ChildRemoteDataSource
import mx.tec.sonrisas.data.remote.FileRemoteDataSource
import mx.tec.sonrisas.domain.Child
import mx.tec.sonrisas.domain.Message
import mx.tec.sonrisas.domain.PickupAuthorization
import mx.tec.sonrisas.domain.WeeklySummary
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
    private val sesionRepository: SesionRepository,
    private val childRemoteDataSource: ChildRemoteDataSource,
    private val fileRemoteDataSource: FileRemoteDataSource,
    @ApplicationContext private val context: Context
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

    var resumenSemanal by mutableStateOf<WeeklySummary?>(null)
        private set

    val ninoActualId: String?
        get() = ninos.getOrNull(ninoSeleccionadoIndex)?.id

    val childActual: Child?
        get() = ninos.getOrNull(ninoSeleccionadoIndex)

    private var chatIdActual: String = ""

    fun cargarResumenSemanal(childId: String) {
        viewModelScope.launch {
            try {
                val child = ninos.firstOrNull { it.id == childId }
                val childName = child?.fullName ?: "Alumno/a"

                dailyLogRepository.observeByChild(childId).collect { logs ->
                    if (logs.isEmpty()) {
                        resumenSemanal = WeeklySummary(
                            childId = childId,
                            childName = childName,
                            weekLabel = "Semana Actual",
                            daysAttended = 0,
                            totalDays = 5,
                            totalActivitiesCompleted = 0,
                            topActivities = emptyList(),
                            mealsEaten = 0,
                            favoriteMealNote = "Sin registros de alimentación esta semana",
                            averageNapMinutes = 0,
                            predominantMood = "Sin registro de ánimo",
                            teacherComment = "Aún no hay observaciones registradas esta semana."
                        )
                    } else {
                        val diasAsistidos = logs.count { it.checkIn != null }
                        val notasComida = logs.mapNotNull { it.stay?.feedingNotes }.filter { it.isNotBlank() }
                        val resumenComida = if (notasComida.isNotEmpty()) {
                            notasComida.joinToString(separator = " · ")
                        } else {
                            "Alimentación registrada por la maestra"
                        }

                        val siestas = logs.mapNotNull { it.stay?.napMinutes }.filter { it > 0 }
                        val promedioSiesta = if (siestas.isNotEmpty()) siestas.average().toInt() else 0

                        val animos = logs.mapNotNull { it.stay?.mood }.filter { it.isNotBlank() }
                        val animoPredominante = if (animos.isNotEmpty()) animos.last() else "Tranquilo y participativo"

                        val notasMaestra = logs.mapNotNull {
                            it.stay?.generalHealthNotes ?: it.checkOut?.notes
                        }.filter { it.isNotBlank() }
                        val comentarioMaestra = if (notasMaestra.isNotEmpty()) {
                            notasMaestra.last()
                        } else {
                            "El alumno ha mostrado un excelente desempeño durante las jornadas escolares."
                        }

                        val listaActividadesReales = mutableListOf<String>()
                        logs.forEach { log ->
                            val actividades = dailyLogRepository.observeActivities(log.id).first()
                            actividades.forEach { act ->
                                if (act.name.isNotBlank()) listaActividadesReales.add(act.name)
                            }
                        }

                        resumenSemanal = WeeklySummary(
                            childId = childId,
                            childName = childName,
                            weekLabel = "Semana Actual",
                            daysAttended = diasAsistidos.coerceAtLeast(1),
                            totalDays = 5,
                            totalActivitiesCompleted = listaActividadesReales.size,
                            topActivities = if (listaActividadesReales.isNotEmpty()) listaActividadesReales.distinct() else listOf("Actividades pedagógicas en el aula"),
                            mealsEaten = logs.count { it.stay != null } * 2,
                            favoriteMealNote = resumenComida,
                            averageNapMinutes = promedioSiesta,
                            predominantMood = animoPredominante,
                            teacherComment = comentarioMaestra
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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

    fun actualizarDatosHijo(childId: String, birthdate: String, allergies: String, actaUri: Uri?, onListo: () -> Unit) {
        viewModelScope.launch {
            try {
                if (actaUri != null) {
                    fileRemoteDataSource.uploadChildDocument(childId, context)
                }

                val alergiasList = if (allergies.isNotBlank() && allergies.lowercase() != "ninguna" && allergies.lowercase() != "ninguno") {
                    allergies.split(",").map { it.trim() }.filter { it.isNotBlank() }
                } else {
                    emptyList()
                }

                val updatedDto = childRemoteDataSource.submitProfile(childId, birthdate, alergiasList)

                val ninoExistente = ninos.firstOrNull { it.id == childId }
                if (ninoExistente != null) {
                    val ninoActualizado = ninoExistente.copy(
                        birthdate = birthdate,
                        allergies = allergies,
                        profileCompletedAt = updatedDto.profileCompletedAt ?: OffsetDateTime.now().toString()
                    )
                    childRepository.save(ninoActualizado)
                }

                onListo()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cargarMensajes(chatId: String) {
        chatIdActual = chatId
        viewModelScope.launch {
            estadoMensajes = UiState.Cargando
            try {
                launch { refrescarMensajes(chatId) }
                chatRepository.observeMessagesForParent(chatId).collect { mensajes ->
                    estadoMensajes = if (mensajes.isEmpty()) UiState.Vacio else UiState.Exito(mensajes)
                }
            } catch (e: Exception) {
                estadoMensajes = UiState.Error("Error al cargar chat")
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

    fun enviarFoto(uri: Uri) {
        viewModelScope.launch {
            val foto = context.leerFoto(uri) ?: return@launch
            val autor = sesionRepository.sesion.first()?.id.orEmpty()
            try {
                chatRepository.sendPhoto(chatIdActual, autor, foto.first, foto.second)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                chatRepository.deleteMessage(messageId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun enviarMensaje(texto: String, esAviso: Boolean = false) {
        viewModelScope.launch {
            try {
                val autor = sesionRepository.sesion.first()?.id ?: "padre"
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
