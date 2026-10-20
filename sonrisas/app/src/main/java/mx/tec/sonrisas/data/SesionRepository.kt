package mx.tec.sonrisas.data

import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mx.tec.sonrisas.data.chat.ChatSocket
import mx.tec.sonrisas.data.local.dao.ChildDao
import mx.tec.sonrisas.data.local.dao.MessageDao
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
    private val socket: ChatSocket,
    private val messageDao: MessageDao,
    private val childDao: ChildDao,
    private val push: PushRepository,
    private val estancias: EstanciaRemoteDataSource
) {
    private val _sesion = MutableStateFlow<Sesion?>(null)
    val sesion: StateFlow<Sesion?> = _sesion.asStateFlow()

    val haySesionGuardada: Boolean get() = auth.hasStoredSession

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
        if (_sesion.value?.pendiente == false) socket.reconnectNow()
    }

    suspend fun salir() {
        socket.disconnect()
        socket.peerChatIds = { emptyList() }
        _sesion.value = null
        push.unregister()
        auth.logout()
        messageDao.clear()
        childDao.clear()
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
            }
            socket.peerChatIds = when (rol) {
                Rol.ADMIN -> { { listOf(me.sub) } }
                Rol.ORG -> { { estancias.fetchEstancias().mapNotNull { it.adminUserId } } }
                else -> { { emptyList() } }
            }
            socket.connect()
            push.register()
        }
        _sesion.value = sesion
    }
}
