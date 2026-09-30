package mx.tec.sonrisas.domain

import java.time.LocalDate
import java.util.UUID

data class DailyLog(
    val id: String = UUID.randomUUID().toString(),
    val childId: String,
    val date: LocalDate,
    val caregiverName: String,
    val checkIn: CheckInRecord,
    val stay: Stay? = null,
    val checkOut: CheckOutRecord? = null,
    // Marks whether this record, created while offline, has been uploaded yet.
    val synced: Boolean = false
)
