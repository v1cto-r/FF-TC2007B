package mx.tec.sonrisas.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import mx.tec.sonrisas.domain.Rol
import mx.tec.sonrisas.domain.Sesion

class SesionRepository {

    private val _sesion = MutableStateFlow<Sesion?>(null)
    val sesion: Flow<Sesion?> = _sesion.asStateFlow()

    fun entrar(correo: String, contrasena: String) {
        val correoLimpio = correo.trim().lowercase()
        val rol = if (correoLimpio.contains("padre")) {
            Rol.PADRE
        } else {
            Rol.MAESTRA
        }
        val nombre = if (rol == Rol.MAESTRA) "Maestra Laura" else "María López"

        _sesion.value = Sesion(
            usuario = correoLimpio.ifEmpty { "demo@sonrisas.edu.mx" },
            nombre = nombre,
            rol = rol
        )
    }

    fun salir() {
        _sesion.value = null
    }
}
