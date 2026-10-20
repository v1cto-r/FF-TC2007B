package mx.tec.sonrisas.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mx.tec.sonrisas.data.ChatRepository
import mx.tec.sonrisas.data.SesionRepository
import mx.tec.sonrisas.data.remote.EstanciaIn
import mx.tec.sonrisas.data.remote.EstanciaRemoteDataSource
import mx.tec.sonrisas.data.remote.UserCreate
import mx.tec.sonrisas.domain.Message
import mx.tec.sonrisas.ui.screens.organizacion.AdministradorUi
import mx.tec.sonrisas.ui.screens.organizacion.InstanciaUi

@HiltViewModel
class OrgViewModel @Inject constructor(
    private val estanciaRemote: EstanciaRemoteDataSource,
    private val chatRepository: ChatRepository,
    private val sesionRepository: SesionRepository
) : ViewModel() {

    var estadoInstancias by mutableStateOf<UiState<List<InstanciaUi>>>(UiState.Cargando)
        private set

    var estadoDetalleInstancia by mutableStateOf<UiState<InstanciaUi>>(UiState.Cargando)
        private set

    var contrasenaTemporalGenerada by mutableStateOf<String?>(null)
        private set

    fun limpiarContrasenaTemporal() {
        contrasenaTemporalGenerada = null
    }

    fun cargarInstancias() {
        viewModelScope.launch {
            estadoInstancias = UiState.Cargando
            try {
                val estanciasDto = runCatching { estanciaRemote.fetchEstancias() }.getOrDefault(emptyList())
                val usersDto = runCatching { estanciaRemote.fetchUsers() }.getOrDefault(emptyList())
                val salonesDto = runCatching { estanciaRemote.fetchSalones() }.getOrDefault(emptyList())
                val childrenDto = runCatching { estanciaRemote.fetchChildren() }.getOrDefault(emptyList())

                val lista = estanciasDto.map { est ->
                    val adminUser = usersDto.firstOrNull { it.id == est.adminUserId || (it.estanciaId == est.id && it.role == "ADMIN") }
                    val adminUi = adminUser?.let { AdministradorUi(id = it.id, nombre = it.fullName, correo = it.email) }

                    val salonesCount = salonesDto.count { it.estanciaId == est.id }
                    val ninosCount = childrenDto.count { it.estanciaId == est.id }
                    val maestrosCount = usersDto.count { it.estanciaId == est.id && (it.role == "DOCENTE" || it.role == "MAESTRA") }

                    InstanciaUi(
                        id = est.id,
                        nombre = est.name,
                        ciudad = est.address?.substringAfter(",")?.trim() ?: "México",
                        direccion = est.address ?: "Sin dirección",
                        telefono = "N/D",
                        capacidad = 50,
                        activa = est.status != "ARCHIVED",
                        ninos = ninosCount,
                        maestros = maestrosCount,
                        salones = salonesCount,
                        admin = adminUi
                    )
                }
                estadoInstancias = if (lista.isEmpty()) UiState.Vacio else UiState.Exito(lista)
            } catch (e: Exception) {
                e.printStackTrace()
                estadoInstancias = UiState.Error("Error al cargar instancias: ${e.message}")
            }
        }
    }

    fun cargarInstanciaDetalle(id: String) {
        viewModelScope.launch {
            estadoDetalleInstancia = UiState.Cargando
            try {
                val est = estanciaRemote.getEstancia(id)
                val users = runCatching { estanciaRemote.fetchUsers() }.getOrDefault(emptyList())
                val salonesDto = runCatching { estanciaRemote.fetchSalones() }.getOrDefault(emptyList())
                val childrenDto = runCatching { estanciaRemote.fetchChildren() }.getOrDefault(emptyList())

                val adminUser = users.firstOrNull { it.id == est.adminUserId || (it.estanciaId == est.id && it.role == "ADMIN") }
                val adminUi = adminUser?.let { AdministradorUi(id = it.id, nombre = it.fullName, correo = it.email) }

                val salonesCount = salonesDto.count { it.estanciaId == est.id }
                val ninosCount = childrenDto.count { it.estanciaId == est.id }
                val maestrosCount = users.count { it.estanciaId == est.id && (it.role == "DOCENTE" || it.role == "MAESTRA") }

                val instancia = InstanciaUi(
                    id = est.id,
                    nombre = est.name,
                    ciudad = est.address?.substringAfter(",")?.trim() ?: "México",
                    direccion = est.address ?: "Sin dirección",
                    telefono = "N/D",
                    capacidad = 50,
                    activa = est.status != "ARCHIVED",
                    ninos = ninosCount,
                    maestros = maestrosCount,
                    salones = salonesCount,
                    admin = adminUi
                )
                estadoDetalleInstancia = UiState.Exito(instancia)
            } catch (e: Exception) {
                e.printStackTrace()
                estadoDetalleInstancia = UiState.Error("Error al cargar detalle: ${e.message}")
            }
        }
    }

    fun guardarInstancia(
        id: String?,
        nombre: String,
        direccion: String,
        telefono: String,
        capacidad: Int,
        adminNombre: String,
        adminCorreo: String,
        onListo: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val estanciaId = id ?: UUID.randomUUID().toString()
                val users = runCatching { estanciaRemote.fetchUsers() }.getOrDefault(emptyList())
                var adminUserId = users.firstOrNull { it.email.equals(adminCorreo.trim(), true) }?.id

                if (adminUserId == null && adminNombre.isNotBlank() && adminCorreo.isNotBlank()) {
                    val created = estanciaRemote.createUser(
                        UserCreate(email = adminCorreo.trim(), fullName = adminNombre.trim(), role = "ADMIN")
                    )
                    adminUserId = created.id
                    contrasenaTemporalGenerada = created.temporaryPassword
                }

                estanciaRemote.putEstancia(
                    id = estanciaId,
                    estancia = EstanciaIn(
                        name = nombre.trim(),
                        address = direccion.trim(),
                        adminUserId = adminUserId
                    )
                )
                cargarInstancias()
                if (contrasenaTemporalGenerada == null) {
                    onListo()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun guardarAviso(
        titulo: String,
        mensaje: String,
        idsInstancias: List<String>?,
        onListo: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val autor = sesionRepository.sesion.first()?.id ?: "org"
                val estanciasDto = runCatching { estanciaRemote.fetchEstancias() }.getOrDefault(emptyList())
                val usersDto = runCatching { estanciaRemote.fetchUsers() }.getOrDefault(emptyList())
                val targetEstancias = if (idsInstancias != null) {
                    estanciasDto.filter { it.id in idsInstancias }
                } else {
                    estanciasDto
                }

                val contenido = if (titulo.isNotBlank()) "$titulo: $mensaje" else mensaje

                targetEstancias.forEach { est ->
                    val adminUser = usersDto.firstOrNull { it.id == est.adminUserId || (it.estanciaId == est.id && it.role == "ADMIN") }
                    adminUser?.id?.let { adminId ->
                        chatRepository.send(
                            Message(
                                chatId = adminId,
                                authorId = autor,
                                content = contenido,
                                sentAt = LocalDateTime.now(),
                                isAviso = true
                            )
                        )
                    }
                }
                onListo()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetarContrasenaAdmin(userId: String) {
        viewModelScope.launch {
            try {
                val res = estanciaRemote.resetPassword(userId)
                contrasenaTemporalGenerada = res.temporaryPassword
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cambiarEstadoInstancia(id: String, activa: Boolean, onListo: () -> Unit) {
        viewModelScope.launch {
            try {
                if (activa) {
                    estanciaRemote.archiveEstancia(id)
                } else {
                    estanciaRemote.unarchiveEstancia(id)
                }
                cargarInstancias()
                onListo()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
