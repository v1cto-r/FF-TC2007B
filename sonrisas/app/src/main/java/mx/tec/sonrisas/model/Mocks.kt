package mx.tec.sonrisas.model

object Mocks {
    private const val SALA = "Sala 3 · Prescolares B"

    val ninos = listOf(
        Nino("Sofía Martínez", SALA, EstadoAsistencia.Presente),
        Nino("Regina López", SALA, EstadoAsistencia.Presente),
        Nino("Valentina Ruiz", SALA, EstadoAsistencia.Ausente),
        Nino("Mateo García", SALA, EstadoAsistencia.Presente),
        Nino("Camila Fernández", SALA, EstadoAsistencia.Presente)
    )

    val actividades = listOf(
        Actividad(TipoActividad.Comida, "Comió su comida", "Sopa de fideo y tacos de pollo", "1:00 PM"),
        Actividad(TipoActividad.Juegos, "Jugó con un rompecabezas", "Desarrollo motriz e interacción", "11:00 AM"),
        Actividad(TipoActividad.Siesta, "Durmió 30min", "Descansó poco tiempo", "2:00 PM"),
        Actividad(TipoActividad.Aviso, "Mañana traer cambio de ropa", "Se programó juego con agua al aire libre")
    )

    val eventos = listOf(
        Evento("VIE", "Reunión de Padres", "Taller mensual de entrega de reportes - 4:00 PM", true),
        Evento("LUN", "Vacunación programada", "Campaña nacional de salud infantil - 9:00 AM", false)
    )

    // Vista de la maestra: sus mensajes son "esMio = true"
    val mensajesMaestra = listOf(
        Mensaje("Este viernes habrá taller de pintura. Favor de enviar delantal protector.", "8:30 AM", esMio = true, esAviso = true),
        Mensaje("Hola Maestra Laura, muchas gracias por el aviso.", "11:15 AM", esMio = false),
        Mensaje("Sofía se divirtió mucho hoy con los bloques.", "2:15 PM", esMio = true),
        Mensaje("¡Muchas gracias por el reporte, maestra!", "2:30 PM", esMio = false)
    )

    // Vista del padre: la misma conversación con los roles invertidos
    val mensajesPadre = mensajesMaestra.map { it.copy(esMio = !it.esMio) }
}