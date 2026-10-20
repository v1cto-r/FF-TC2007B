package mx.tec.sonrisas.ui.screens.organizacion

import java.time.LocalDate
import mx.tec.sonrisas.domain.Notice

data class AdministradorUi(val id: String, val nombre: String, val correo: String)

data class InstanciaUi(
    val id: String,
    val nombre: String,
    val ciudad: String,
    val direccion: String,
    val telefono: String,
    val capacidad: Int,
    val activa: Boolean,
    val ninos: Int,
    val maestros: Int,
    val salones: Int,
    val admin: AdministradorUi?   // null = sin administrador asignado
)

/** Datos falsos, se borran cuando se conecte la BD. */
object OrgMuestra {
    val instancias = listOf(
        InstanciaUi(
            id = "i1", nombre = "Sol Naciente", ciudad = "Pachuca",
            direccion = "Av. Juárez 120, Centro", telefono = "771 123 4567", capacidad = 50,
            activa = true, ninos = 42, maestros = 6, salones = 3,
            admin = AdministradorUi("u1", "María López", "maria.lopez@email.com")
        ),
        InstanciaUi(
            id = "i2", nombre = "Casita Arcoíris", ciudad = "Monterrey",
            direccion = "Calle Hidalgo 45, Centro", telefono = "81 2345 6789", capacidad = 35,
            activa = true, ninos = 30, maestros = 5, salones = 2,
            admin = null
        ),
        InstanciaUi(
            id = "i3", nombre = "Pequeños Pasos", ciudad = "Tulancingo",
            direccion = "Blvd. Las Torres 300", telefono = "775 987 6543", capacidad = 30,
            activa = false, ninos = 25, maestros = 4, salones = 2,
            admin = AdministradorUi("u3", "Ana Ruiz", "ana.ruiz@email.com")
        ),
        InstanciaUi(
            id = "i4", nombre = "Mariposa Centro", ciudad = "CDMX",
            direccion = "Calle Madero 88, Centro Histórico", telefono = "55 4321 8765", capacidad = 60,
            activa = true, ninos = 55, maestros = 8, salones = 4,
            admin = AdministradorUi("u4", "Carla Gómez", "carla.gomez@email.com")
        )
    )

    val avisos = listOf(
        Notice(name = "Reportes mensuales", description = "Recordatorio de entrega antes del día 30", date = LocalDate.now()),
        Notice(name = "Cambio de plataforma", description = "Mantenimiento programado el sábado", date = LocalDate.now().minusDays(2)),
        Notice(name = "Capacitación nuevos admins", description = "Sesión en línea el lunes a las 5:00 PM", date = LocalDate.now().minusDays(5))
    )
}
