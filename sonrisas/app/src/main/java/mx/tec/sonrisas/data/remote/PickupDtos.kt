package mx.tec.sonrisas.data.remote

import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mx.tec.sonrisas.domain.PickupAuthorization
import mx.tec.sonrisas.domain.PickupKind

// Bodies of /pickup-authorizations (backend app/schemas/pickups.py). Weekdays are ISO: 1 = Monday ... 7 = Sunday.

@Serializable
data class PickupInBody(
    @SerialName("child_id") val childId: String,
    val kind: String,
    val weekdays: List<Int>,
    @SerialName("authorized_person_name") val authorizedPersonName: String,
    val relationship: String,
    val phone: String,
    @SerialName("identification_type") val identificationType: String,
    val notes: String? = null
)

@Serializable
data class PickupOutDto(
    val id: String,
    @SerialName("child_id") val childId: String,
    val kind: String,
    @SerialName("start_date") val startDate: String,
    val weekdays: List<Int>,
    @SerialName("authorized_person_name") val authorizedPersonName: String,
    val relationship: String,
    val phone: String,
    @SerialName("identification_type") val identificationType: String,
    val notes: String? = null
)

fun PickupAuthorization.toBody() = PickupInBody(
    childId = childId,
    kind = kind.name,
    weekdays = weekdays.map { it.value }.sorted(),
    authorizedPersonName = authorizedPersonName,
    relationship = relationship,
    phone = phone,
    identificationType = identificationType,
    notes = notes
)

fun PickupOutDto.toDomain() = PickupAuthorization(
    id = id,
    childId = childId,
    kind = PickupKind.valueOf(kind),
    weekdays = weekdays.map(DayOfWeek::of).toSet(),
    startDate = LocalDate.parse(startDate),
    authorizedPersonName = authorizedPersonName,
    relationship = relationship,
    phone = phone,
    identificationType = identificationType,
    notes = notes
)
