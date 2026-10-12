package mx.tec.sonrisas.domain

import java.time.LocalDate
import java.util.UUID

data class PickupAuthorization(
    val id: String = UUID.randomUUID().toString(),
    val childId: String,
    val childName: String,
    val date: LocalDate = LocalDate.now(),
    val authorizedPersonName: String,
    val relationship: String,
    val phone: String,
    val identificationType: String = "INE",
    val notes: String? = null,
    val registeredByParentName: String
)
