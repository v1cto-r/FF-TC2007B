package mx.tec.sonrisas.data

import kotlinx.coroutines.delay
import mx.tec.sonrisas.model.Actividad
import mx.tec.sonrisas.model.Evento
import mx.tec.sonrisas.model.Mensaje
import mx.tec.sonrisas.model.Mocks
import mx.tec.sonrisas.model.Nino

class SonrisasRepository {

    private val mensajesMaestraList = Mocks.mensajesMaestra.toMutableList()
    private val mensajesPadreList = Mocks.mensajesPadre.toMutableList()

    suspend fun obtenerEventos(): List<Evento> {
        delay(100)
        return Mocks.eventos
    }

    suspend fun obtenerNinos(): List<Nino> {
        delay(100)
        return Mocks.ninos
    }

    suspend fun obtenerNino(nombre: String): Nino? {
        delay(100)
        return Mocks.ninos.firstOrNull { it.nombre.equals(nombre, ignoreCase = true) }
            ?: Mocks.ninos.firstOrNull()
    }

    suspend fun obtenerMensajesMaestra(): List<Mensaje> {
        delay(100)
        return mensajesMaestraList.toList()
    }

    suspend fun obtenerActividadesPadre(): List<Actividad> {
        delay(100)
        return Mocks.actividades
    }

    suspend fun obtenerMensajesPadre(): List<Mensaje> {
        delay(100)
        return mensajesPadreList.toList()
    }

    suspend fun enviarMensajeMaestra(texto: String): List<Mensaje> {
        mensajesMaestraList.add(
            Mensaje(
                texto = texto,
                hora = "Ahora",
                esMio = true,
                esAviso = false
            )
        )
        return mensajesMaestraList.toList()
    }

    suspend fun enviarMensajePadre(texto: String): List<Mensaje> {
        mensajesPadreList.add(
            Mensaje(
                texto = texto,
                hora = "Ahora",
                esMio = true,
                esAviso = false
            )
        )
        return mensajesPadreList.toList()
    }
}
