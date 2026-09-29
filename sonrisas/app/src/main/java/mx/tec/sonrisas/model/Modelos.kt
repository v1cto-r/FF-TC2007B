package mx.tec.sonrisas.model

data class Mensaje(
    val texto: String,
    val hora: String,
    val esMio: Boolean,
    val esAviso: Boolean = false,
    val imagenRes: Int? = null
)

enum class EstadoAsistencia { Presente, Ausente }

data class Nino(
    val nombre: String,
    val sala: String,
    val asistencia: EstadoAsistencia,
    val fotoRes: Int? = null
)

enum class TipoActividad { Comida, Juegos, Siesta, Aviso }

data class Actividad(
    val tipo: TipoActividad,
    val titulo: String,
    val detalle: String,
    val hora: String = ""
)

data class Evento(
    val dia: String,
    val titulo: String,
    val descripcion: String,
    val esCoral: Boolean
)