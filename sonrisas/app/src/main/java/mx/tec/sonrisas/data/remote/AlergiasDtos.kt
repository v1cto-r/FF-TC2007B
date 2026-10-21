package mx.tec.sonrisas.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Una entrada del catálogo de alérgenos (GET /allergens). `name` es lo que se guarda en el niño. */
@Serializable
data class AllergenDto(
    val id: String,
    val name: String,
    val keywords: String = ""
)

/** Un platillo de la semana y, opcional, ingredientes o cosas que podrían causar reacción. */
@Serializable
data class MealBody(
    val name: String,
    val ingredients: List<String> = emptyList()
)

@Serializable
data class MenuCheckBody(val meals: List<MealBody>)

@Serializable
data class DetectedAllergenDto(
    val name: String,
    val probability: Double,
    val meals: List<String> = emptyList()
)

@Serializable
data class AllergenConflictDto(
    val allergen: String,
    val meals: List<String> = emptyList()
)

@Serializable
data class ChildConflictDto(
    @SerialName("child_id") val childId: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("salon_name") val salonName: String? = null,
    val conflicts: List<AllergenConflictDto> = emptyList()
)

/** Respuesta de POST /estancias/{id}/menu-check. */
@Serializable
data class MenuCheckDto(
    val detected: List<DetectedAllergenDto> = emptyList(),
    val conflicts: List<ChildConflictDto> = emptyList()
)
