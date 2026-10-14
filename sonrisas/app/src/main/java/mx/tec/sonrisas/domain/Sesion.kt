package mx.tec.sonrisas.domain

enum class Rol {
    MAESTRA,
    PADRE
}

data class Sesion(
    val id: String,
    val usuario: String,
    val nombre: String,
    val rol: Rol,
    val pendiente: Boolean = false
)
