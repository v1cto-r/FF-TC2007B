package mx.tec.sonrisas.ui.screens.admin

data class MaestroUi(val id: String, val nombre: String, val correo: String)

data class AlumnoUi(val id: String, val nombre: String, val tutor: String)

data class SalonUi(
    val id: String,
    val nombre: String,
    val maestro: MaestroUi?,
    val alumnos: List<AlumnoUi>
)

/** Datos falsos, se borran cuando se conecte la BD. */
object AdminMuestra {
    val maestros = listOf(
        MaestroUi("m1", "Laura Hernández", "laura@sonrisas.mx"),
        MaestroUi("m2", "Carmen Salinas", "carmen@sonrisas.mx"),
        MaestroUi("m3", "Patricia Ortiz", "patricia@sonrisas.mx")
    )

    private val a1 = AlumnoUi("a1", "Sofía Martínez", "María López")
    private val a2 = AlumnoUi("a2", "Mateo García", "Jorge García")
    private val a3 = AlumnoUi("a3", "Valentina Ruiz", "Carla Ruiz")
    private val a4 = AlumnoUi("a4", "Santiago Díaz", "Ana Díaz")
    private val a5 = AlumnoUi("a5", "Emma Torres", "Luis Torres")
    private val a6 = AlumnoUi("a6", "Leonardo Vega", "Rosa Vega")

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
}