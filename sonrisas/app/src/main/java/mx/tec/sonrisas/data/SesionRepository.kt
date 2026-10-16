package mx.tec.sonrisas.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sesion

@Singleton
class SesionRepository @Inject constructor() {

    private val _sesion = MutableStateFlow<Sesion?>(null)
    val sesion: Flow<Sesion?> = _sesion.asStateFlow()

    fun entrar(correo: String, contrasena: String): Boolean {
        val correoLimpio = correo.trim().lowercase()
        val passwordLimpia = contrasena.trim()

        val sesionValida = when {
            (correoLimpio == "maestra@sonrisas.edu.mx" || correoLimpio == "maestra") && passwordLimpia.isNotEmpty() -> {
                Sesion(usuario = "maestra@sonrisas.edu.mx", nombre = "Maestra Laura", rol = Rol.MAESTRA)
            }
            (correoLimpio == "padre@sonrisas.edu.mx" || correoLimpio == "padre") && passwordLimpia.isNotEmpty() -> {
                Sesion(usuario = "padre@sonrisas.edu.mx", nombre = "María López", rol = Rol.PADRE)
            }
            (correoLimpio == "admin@sonrisas.edu.mx" || correoLimpio == "admin") && passwordLimpia.isNotEmpty() -> {
                Sesion(usuario = "admin@sonrisas.edu.mx", nombre = "Administrador", rol = Rol.ADMIN)
            }
            (correoLimpio == "org@sonrisas.edu.mx" || correoLimpio == "org" || correoLimpio == "organizador") && passwordLimpia.isNotEmpty() -> {
                Sesion(usuario = "org@sonrisas.edu.mx", nombre = "Sofía", rol = Rol.ORG)
            }
            correoLimpio.contains("maestra") && passwordLimpia.isNotEmpty() -> {
                Sesion(usuario = correoLimpio, nombre = "Maestra Laura", rol = Rol.MAESTRA)
            }
            correoLimpio.contains("padre") && passwordLimpia.isNotEmpty() -> {
                Sesion(usuario = correoLimpio, nombre = "María López", rol = Rol.PADRE)
            }
            else -> null
        }

        if (sesionValida != null) {
            _sesion.value = sesionValida
            return true
        }
        return false
    }

    fun entrarConSesion(usuario: String, nombre: String, rol: Rol) {
        _sesion.value = Sesion(usuario = usuario, nombre = nombre, rol = rol)
    }

    fun salir() {
        _sesion.value = null
    }
}