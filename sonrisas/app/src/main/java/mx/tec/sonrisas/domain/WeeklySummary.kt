package mx.tec.sonrisas.domain

data class WeeklySummary(
    val childId: String,
    val childName: String,
    val weekLabel: String = "Semana Actual",
    val daysAttended: Int = 5,
    val totalDays: Int = 5,
    val totalActivitiesCompleted: Int = 6,
    val topActivities: List<String> = listOf("Taller de Expresión Plástica", "Desarrollo Motriz Fino (Rompecabezas)", "Fomento a la Lectura en Grupo"),
    val mealsEaten: Int = 8,
    val favoriteMealNote: String = "Aceptación favorable en sopas y guisados con pollo",
    val averageNapMinutes: Int = 45,
    val predominantMood: String = "Colaborativo, atento y participativo",
    val teacherComment: String = "Durante la semana, el alumno demostró excelente integración, respeto a las reglas del aula y constancia en sus actividades."
)
