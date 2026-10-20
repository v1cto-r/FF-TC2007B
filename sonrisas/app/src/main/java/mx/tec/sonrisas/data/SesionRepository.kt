package mx.tec.sonrisas.data

import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mx.tec.sonrisas.data.chat.ChatSocket
import mx.tec.sonrisas.data.local.dao.ChildDao
import mx.tec.sonrisas.data.local.dao.MessageDao
import mx.tec.sonrisas.data.local.dao.NoticeDao
import mx.tec.sonrisas.data.local.dao.PickupAuthorizationDao
import mx.tec.sonrisas.data.push.PushRepository
import mx.tec.sonrisas.data.remote.ApiClient
import mx.tec.sonrisas.data.remote.MeDto
import mx.tec.sonrisas.data.remote.EstanciaRemoteDataSource
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sesion

class RolNoSoportadoException : Exception("Esta cuenta no puede usar la app. Inicia sesión desde la plataforma web.")

@Singleton
class SesionRepository @Inject constructor(
    private val auth: AuthRepository,
    private val api: ApiClient,
    private val childRepository: ChildRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val pickupAuthorizationRepository: PickupAuthorizationRepository,
    private val pickupAuthorizationDao: PickupAuthorizationDao,
    private val noticeRepository: NoticeRepository,
    private val noticeDao: NoticeDao,
    private val socket: ChatSocket,
    private val messageDao: MessageDao,
    private val childDao: ChildDao,
    private val push: PushRepository,
    private val admin: AdminRepository,
    private val estancias: EstanciaRemoteDataSource
) {
    // Background refreshes that outlive any screen (coming back to the foreground)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _sesion = MutableStateFlow<Sesion?>(null)
    val sesion: StateFlow<Sesion?> = _sesion.asStateFlow()

    val haySesionGuardada: Boolean get() = auth.hasStoredSession

    init {
        // Deleted or archived by an admin: the server closes the chat socket and refuses the refresh token
        scope.launch {
            auth.revocada.collect {
                if (_sesion.value == null) return@collect
                terminarSesion()
                borrarDatosLocales()   // the server already dropped this device's push registration
            }
        }
    }

    suspend fun entrar(correo: String, contrasena: String) {
        auth.login(correo, contrasena)
        abrirSesion()
    }

    suspend fun restaurar(): Boolean {
        if (!auth.hasStoredSession) return false
        return try {
            abrirSesion()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun cambiarContrasena(actual: String, nueva: String) {
        auth.changePassword(actual, nueva)
        abrirSesion()
    }

    fun alPrimerPlano() {
        val actual = _sesion.value ?: return
        if (actual.pendiente) return
        socket.reconnectNow()
        scope.launch { refrescarDatos(actual.rol, RECENT_DAYS) }
    }

    /**
     * What the teacher and the parent share: daily logs, pickup authorizations and notices. Each one
     * on its own and never throwing: an unreachable server (or one missing an endpoint) only means the
     * copy in Room stays as it was.
     */
    suspend fun refrescarDatos(rol: Rol? = _sesion.value?.rol, dias: Long = RECENT_DAYS) {
        if (rol != Rol.PADRE && rol != Rol.MAESTRA) return
        runCatching { dailyLogRepository.refreshFromServer(dias) }
        runCatching { pickupAuthorizationRepository.refreshFromServer() }
        runCatching { noticeRepository.refreshFromServer() }
    }

    /** Records on this phone not uploaded yet; logging out throws them away. */
    suspend fun registrosSinEnviar(): Int = dailyLogRepository.countPending()

    suspend fun salir() {
        terminarSesion()
        push.unregister()
        auth.logout()
        borrarDatosLocales()
    }

    /** Leaves the UI at Login right away. */
    private fun terminarSesion() {
        socket.disconnect()
        socket.peerChatIds = { emptyList() }
        _sesion.value = null
    }

    private suspend fun borrarDatosLocales() {
        messageDao.clear()
        childDao.clear()
        pickupAuthorizationDao.clear()
        noticeDao.clear()
        // The next account on this phone must neither see these nor upload them under its own name
        dailyLogRepository.clearLocal()
    }

    private suspend fun abrirSesion() {
        val me = auth.authed { api.get<MeDto>("/auth/me", it) }
        val rol = when (me.role.uppercase()) {
            "DOCENTE", "MAESTRA" -> Rol.MAESTRA
            "PADRE" -> Rol.PADRE
            "ADMIN", "ADMINISTRADOR" -> Rol.ADMIN
            "ORG" -> Rol.ORG
            else -> {
                auth.logout()
                throw RolNoSoportadoException()
            }
        }
        val sesion = Sesion(me.sub, me.email, me.fullName, rol, me.pending)
        if (!me.pending) {
            if (rol == Rol.PADRE || rol == Rol.MAESTRA) {
                try {
                    childRepository.refreshFromServer(parentName = if (rol == Rol.PADRE) me.fullName else "")
                } catch (e: IOException) {
                }
                refrescarDatos(rol, LOGIN_DAYS)
            } else {
                // ADMIN / ORG have no child chats: stale children would make the socket fetch chats it can't open.
                childDao.clear()
            }
            socket.peerChatIds = when (rol) {
                Rol.MAESTRA -> { { listOf(me.sub) } }
                Rol.ADMIN -> { { admin.teacherIds() + me.sub } }
                Rol.ORG -> { { estancias.fetchEstancias().mapNotNull { it.adminUserId } } }
                else -> { { emptyList() } }
            }
            socket.connect()
            push.register()
        }
        _sesion.value = sesion
    }

    private companion object {
        // On login the history the parent's weekly summary uses; afterwards only what may have changed
        const val LOGIN_DAYS = 90L
        const val RECENT_DAYS = 7L
    }
}
