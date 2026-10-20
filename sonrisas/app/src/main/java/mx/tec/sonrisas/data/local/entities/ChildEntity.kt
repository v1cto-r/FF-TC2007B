package mx.tec.sonrisas.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "children")
data class ChildEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val daycareCenter: String,
    val group: String,
    val parentName: String,
    val birthdate: String = "12 de Marzo, 2021",
    val gender: String = "Femenino",
    val allergies: String? = "Sin alergias conocidas",
    val bloodType: String? = "O+",
    val emergencyContactPhone: String? = "555-123-4567",
    val profileCompletedAt: String? = "2026-01-01T00:00:00Z"
)
