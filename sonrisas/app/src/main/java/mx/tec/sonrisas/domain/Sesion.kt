package mx.tec.sonrisas.domain

enum class Rol {
    MAESTRA,
    PADRE,
    ADMIN,
    ORG
}

data class Sesion(
    val usuario: String,
    val nombre: String,
    val rol: Rol
)