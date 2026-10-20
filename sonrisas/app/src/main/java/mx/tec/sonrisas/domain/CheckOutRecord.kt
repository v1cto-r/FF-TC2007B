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
    val releasedByStaffName: String,
    // Released to someone the parent authorized: then a photo of their ID is required
    val pickupAuthorizationId: String? = null,
    // Server id of that photo, chosen when it is taken so a retried upload keeps it
    val idPhotoFileId: String? = null,
    // The photo on this phone until it is uploaded
    val idPhotoLocalPath: String? = null
)
