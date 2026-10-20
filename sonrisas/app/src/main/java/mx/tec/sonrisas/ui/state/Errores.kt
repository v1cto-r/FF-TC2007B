package mx.tec.sonrisas.ui.state

import java.io.IOException
import mx.tec.sonrisas.data.RolNoSoportadoException
import mx.tec.sonrisas.data.remote.ApiException

fun mensajeDeError(e: Exception, ante401: String = "Correo o contraseña incorrectos."): String = when {
    e is ApiException && e.code == 401 -> ante401
    e is ApiException && e.code == 429 -> "Demasiados intentos. Espera un minuto e inténtalo de nuevo."
    e is ApiException && e.code == 422 -> e.detail ?: "Revisa los datos e inténtalo de nuevo."
    e is RolNoSoportadoException -> e.message.orEmpty()
    e is IOException -> "Sin conexión. Revisa tu internet e inténtalo de nuevo."
    else -> "No pudimos completar la solicitud. Inténtalo de nuevo."
}
