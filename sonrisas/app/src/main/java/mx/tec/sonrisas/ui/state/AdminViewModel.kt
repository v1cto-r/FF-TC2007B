package mx.tec.sonrisas.ui.state

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.AdminRepository
import mx.tec.sonrisas.data.AdminSnapshot
import mx.tec.sonrisas.data.ChatRepository
import mx.tec.sonrisas.data.GROUP_TYPE_POR_DEFECTO
import mx.tec.sonrisas.data.RolApi
import mx.tec.sonrisas.data.SesionRepository
import mx.tec.sonrisas.data.remote.AdminChildDto
import mx.tec.sonrisas.data.remote.ApiException
import mx.tec.sonrisas.data.remote.UserDto
import mx.tec.sonrisas.domain.Message
import mx.tec.sonrisas.ui.screens.admin.AdminDatos
import mx.tec.sonrisas.ui.screens.admin.AlumnoUi
import mx.tec.sonrisas.ui.screens.admin.AvisoUi
import mx.tec.sonrisas.ui.screens.admin.CredencialUi
import mx.tec.sonrisas.ui.screens.admin.MaestroUi
import mx.tec.sonrisas.ui.screens.admin.PadreUi
import mx.tec.sonrisas.ui.screens.admin.SalonUi

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val repo: AdminRepository,
    private val chatRepository: ChatRepository,
    private val sesionRepository: dagger.Lazy<SesionRepository>
) : ViewModel() {

    var estado by mutableStateOf<UiState<AdminDatos>>(UiState.Cargando)
        private set

    var guardando by mutableStateOf(false)
        private set

    var errorMensaje by mutableStateOf<String?>(null)
        private set

    var credencial by mutableStateOf<CredencialUi?>(null)
        private set

    var estadoMensajes by mutableStateOf<UiState<List<Message>>>(UiState.Cargando)
        private set

    private var snapshot: AdminSnapshot? = null

    fun cargar() {
        viewModelScope.launch {
            if (estado !is UiState.Exito) estado = UiState.Cargando
            try {
                refrescar()
            } catch (e: Exception) {
                if (estado !is UiState.Exito) estado = UiState.Error(mensajeAdmin(e))
            }
        }
    }

    fun limpiarError() {
        errorMensaje = null
    }

    fun descartarCredencial() {
        credencial = null
    }

    // --- Grupos ---

    fun crearGrupo(nombre: String, maestroId: String?, alumnoIds: Set<String>, onListo: () -> Unit) =
        ejecutar(onListo) {
            val snap = requerir()
            val salonId = UUID.randomUUID().toString()
            repo.guardarSalon(salonId, snap.estanciaId, nombre, GROUP_TYPE_POR_DEFECTO, maestroId)
            moverNinos(snap, alumnoIds, salonId)
        }

    fun asignarMaestra(salonId: String, maestroId: String?, onListo: () -> Unit) =
        ejecutar(onListo) {
            val snap = requerir()
            val salon = snap.salones.firstOrNull { it.id == salonId }
                ?: throw IllegalStateException("Salón no encontrado")
            repo.guardarSalon(salon.id, snap.estanciaId, salon.name, salon.groupType, maestroId)
        }

    fun agregarAlumnos(salonId: String, alumnoIds: Set<String>, onListo: () -> Unit) =
        ejecutar(onListo) {
            moverNinos(requerir(), alumnoIds, salonId)
        }

    // --- Cuentas y niños ---

    fun crearUsuario(nombre: String, apellido: String, correo: String, rol: String) = ejecutar {
        val completo = nombreCompleto(nombre, apellido)
        val creado = repo.crearUsuario(correo, completo, rol)
        credencial = CredencialUi(completo, creado.email, creado.temporaryPassword)
    }

    fun inscribir(nombreTutor: String, apellidoTutor: String, correo: String, nombreAlumno: String) = ejecutar {
        val snap = requerir()
        val completo = nombreCompleto(nombreTutor, apellidoTutor)
        val creado = repo.crearUsuario(correo, completo, RolApi.PADRE)
        credencial = CredencialUi(completo, creado.email, creado.temporaryPassword)
        repo.guardarNino(UUID.randomUUID().toString(), snap.estanciaId, nombreAlumno, null, creado.id)
    }

    fun agregarNino(padreId: String, nombreNino: String, salonId: String?, onListo: () -> Unit) =
        ejecutar(onListo) {
            val snap = requerir()
            repo.guardarNino(UUID.randomUUID().toString(), snap.estanciaId, nombreNino, salonId, padreId)
        }

    // --- Chat ---

    fun cargarMensajes(chatId: String) {
        viewModelScope.launch {
            estadoMensajes = UiState.Cargando
            try {
                val yo = sesionRepository.get().sesion.first()?.id.orEmpty()
                launch {
                    try { chatRepository.refresh(chatId) } catch (e: Exception) {}
                }
                chatRepository.observeMessagesForTeacher(chatId, yo).collect { mensajes ->
                    estadoMensajes = if (mensajes.isEmpty()) UiState.Vacio else UiState.Exito(mensajes)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                estadoMensajes = UiState.Error("Error al cargar chat: ${e.message}")
            }
        }
    }

    fun enviarMensaje(chatId: String, texto: String) {
        viewModelScope.launch {
            try {
                val autor = sesionRepository.get().sesion.first()?.id ?: "admin"
                chatRepository.send(
                    Message(
                        chatId = chatId,
                        authorId = autor,
                        content = texto,
                        sentAt = LocalDateTime.now(),
                        isAviso = false
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun enviarFoto(chatId: String, uri: Uri, context: Context) {
        viewModelScope.launch {
            val foto = context.leerFoto(uri) ?: return@launch
            val autor = sesionRepository.get().sesion.first()?.id.orEmpty()
            try {
                chatRepository.sendPhoto(chatId, autor, foto.first, foto.second)
            } catch (e: Exception) {
                e.printStackTrace()
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

    fun eliminarMensaje(message: Message) {
        viewModelScope.launch {
            try {
                chatRepository.deleteMessage(message.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Internos ---

    private suspend fun moverNinos(snap: AdminSnapshot, ids: Set<String>, salonId: String) {
        snap.ninos.filter { it.id in ids }.forEach { nino ->
            repo.guardarNino(nino.id, snap.estanciaId, nino.fullName, salonId, nino.parentId)
        }
    }

    private suspend fun refrescar() {
        val nuevo = repo.cargar()
        snapshot = nuevo
        estado = UiState.Exito(nuevo.toDatos())
    }

    private fun requerir(): AdminSnapshot =
        snapshot ?: throw IllegalStateException("Los datos todavía no se cargan")

    private fun nombreCompleto(nombre: String, apellido: String) = "${nombre.trim()} ${apellido.trim()}".trim()

    private fun ejecutar(onListo: () -> Unit = {}, bloque: suspend () -> Unit) {
        viewModelScope.launch {
            guardando = true
            errorMensaje = null
            var fallo: Exception? = null
            try {
                bloque()
            } catch (e: Exception) {
                fallo = e
            }
            runCatching { refrescar() }
            guardando = false
            if (fallo == null) onListo() else errorMensaje = mensajeAdmin(fallo)
        }
    }

    private fun mensajeAdmin(e: Exception): String = when {
        e is ApiException && e.code == 403 -> e.detail ?: "No tienes permiso para hacer esto."
        e is ApiException && e.code == 404 -> e.detail ?: "No encontramos lo que buscas."
        e is ApiException && e.code == 409 -> e.detail ?: "Ese registro ya existe o entra en conflicto con otro."
        else -> mensajeDeError(e, ante401 = "Tu sesión venció. Cierra sesión e inicia de nuevo.")
    }
}

private fun UserDto.toMaestro() = MaestroUi(id = id, nombre = fullName, correo = email)

private fun AdminSnapshot.toDatos(): AdminDatos {
    val maestrosApi = docentes.filter { it.status == null }
    val tutoresApi = padres.filter { it.status == null }
    val ninosActivos = ninos.filter { it.status == null }
    val salonesActivos = salones.filter { it.status == null }

    val maestroPorId = maestrosApi.associateBy { it.id }
    val tutorPorId = tutoresApi.associateBy { it.id }
    val salonPorId = salonesActivos.associateBy { it.id }

    fun alumno(n: AdminChildDto) = AlumnoUi(
        id = n.id,
        nombre = n.fullName,
        tutor = n.parentId?.let { tutorPorId[it] }?.fullName ?: "Sin tutor asignado",
        salon = n.salonId?.let { salonPorId[it] }?.name
    )

    val salonesUi = salonesActivos
        .map { s ->
            SalonUi(
                id = s.id,
                nombre = s.name,
                maestro = s.docenteId?.let { maestroPorId[it] }?.toMaestro(),
                alumnos = ninosActivos.filter { it.salonId == s.id }.map { alumno(it) }
            )
        }
        .sortedBy { it.nombre }

    val padresUi = tutoresApi
        .map { p ->
            PadreUi(
                id = p.id,
                nombre = p.fullName,
                correo = p.email,
                hijos = ninosActivos.filter { it.parentId == p.id }.map { alumno(it) }
            )
        }
        .sortedBy { it.nombre }

    val avisosUi = avisos
        .filter { it.status == null }
        .map { a ->
            val destino = if (a.salonId == null) {
                "Toda la estancia"
            } else {
                "Salón ${salonPorId[a.salonId]?.name.orEmpty()}".trim()
            }
            AvisoUi(a.id, a.name, a.description, a.date, destino)
        }
        .sortedByDescending { it.fecha }

    return AdminDatos(
        salones = salonesUi,
        maestros = maestrosApi.map { it.toMaestro() }.sortedBy { it.nombre },
        padres = padresUi,
        alumnosSinSalon = ninosActivos.filter { it.salonId == null }.map { alumno(it) },
        avisos = avisosUi
    )
}
