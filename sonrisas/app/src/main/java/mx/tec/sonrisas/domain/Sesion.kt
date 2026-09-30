package mx.tec.sonrisas.domain

enum class Rol {
    MAESTRA,
    PADRE
}

data class Sesion(
    val usuario: String,
    val nombre: String,
    val rol: Rol
)
