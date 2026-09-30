package mx.tec.sonrisas.domain

import java.time.LocalTime

data class CheckOutRecord(
    val checkOutTime: LocalTime,
    val releasedInGoodPhysicalCondition: Boolean,
    val injury: Injury? = null,
    val releasedClean: Boolean,
    val belongingsComplete: Boolean,
    val notes: String? = null,
    val pickedUpByName: String,
    val releasedByStaffName: String
)
