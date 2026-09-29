package mx.tec.sonrisas.model

data class Mensaje(
    val texto: String,
    val hora: String,
    val esMio: Boolean,
    val esAviso: Boolean = false,
    val imagenRes: Int? = null   // drawable de la foto, si el mensaje trae imagen
)