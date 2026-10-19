package mx.tec.sonrisas.ui.screens.admin

data class MaestroUi(val id: String, val nombre: String, val correo: String)

data class AlumnoUi(val id: String, val nombre: String, val tutor: String, val salon: String? = null)

data class SalonUi(
    val id: String,
    val nombre: String,
    val maestro: MaestroUi?,
    val alumnos: List<AlumnoUi>
)

data class PadreUi(
    val id: String,
    val nombre: String,
    val correo: String,
    val hijos: List<AlumnoUi>
)

data class AvisoUi(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val fecha: String,
    val destino: String
)

/** Cuenta recién creada: la contraseña temporal solo se puede ver una vez. */
data class CredencialUi(val nombre: String, val correo: String, val contrasena: String)

data class AdminDatos(
    val salones: List<SalonUi>,
    val maestros: List<MaestroUi>,
    val padres: List<PadreUi>,
    val alumnosSinSalon: List<AlumnoUi>,
    val avisos: List<AvisoUi>
)

/** Datos falsos, solo para los @Preview. */
object AdminMuestra {
    val maestros = listOf(
        MaestroUi("m1", "Laura Hernández", "laura@sonrisas.mx"),
        MaestroUi("m2", "Carmen Salinas", "carmen@sonrisas.mx"),
        MaestroUi("m3", "Patricia Ortiz", "patricia@sonrisas.mx")
    )

    private val a1 = AlumnoUi("a1", "Sofía Martínez", "María López", "Mariposas")
    private val a2 = AlumnoUi("a2", "Mateo García", "Jorge García", "Mariposas")
    private val a3 = AlumnoUi("a3", "Valentina Ruiz", "Carla Ruiz", "Mariposas")
    private val a4 = AlumnoUi("a4", "Santiago Díaz", "Ana Díaz", "Estrellitas")
    private val a5 = AlumnoUi("a5", "Emma Torres", "Luis Torres", "Estrellitas")
    private val a6 = AlumnoUi("a6", "Leonardo Vega", "Rosa Vega", "Arcoíris")

    val alumnosSinSalon = listOf(
        AlumnoUi("a7", "Camila Reyes", "Elena Reyes"),
        AlumnoUi("a8", "Diego Castillo", "Marco Castillo"),
        AlumnoUi("a9", "Isabella Mora", "Sandra Mora"),
        AlumnoUi("a10", "Emiliano Cruz", "Pablo Cruz")
    )

    val salones = listOf(
        SalonUi("s1", "Mariposas", maestros[0], listOf(a1, a2, a3)),
        SalonUi("s2", "Estrellitas", maestros[1], listOf(a4, a5)),
        SalonUi("s3", "Arcoíris", null, listOf(a6))
    )

    val padres = listOf(
        PadreUi("p1", "María López", "maria@correo.com", listOf(a1)),
        PadreUi("p2", "Jorge García", "jorge@correo.com", listOf(a2)),
        PadreUi("p3", "Elena Reyes", "elena@correo.com", alumnosSinSalon.take(1)),
        PadreUi("p4", "Sandra Mora", "sandra@correo.com", emptyList())
    )

    val avisos = listOf(
        AvisoUi("n1", "Junta de padres", "El viernes a las 5 pm en el patio principal.", "2026-10-23", "Toda la estancia"),
        AvisoUi("n2", "Traer ropa de cambio", "Favor de enviar una muda extra en la mochila.", "2026-10-20", "Salón Mariposas")
    )
}