package mx.tec.sonrisas.domain

import java.util.UUID

data class Child(
    val id: String = UUID.randomUUID().toString(),
    val fullName: String,
    val daycareCenter: String,
    val group: GroupEnum,
    val parentName: String,
    val birthdate: String = "12 de Marzo, 2021",
    val gender: String = "Femenino",
    val allergies: String? = "Sin alergias conocidas",
    val bloodType: String? = "O+",
    val emergencyContactPhone: String? = "555-123-4567",
    val profileCompletedAt: String? = "2026-01-01T00:00:00Z"
) {
    val profileCompleted: Boolean
        get() = !profileCompletedAt.isNullOrBlank()
}
