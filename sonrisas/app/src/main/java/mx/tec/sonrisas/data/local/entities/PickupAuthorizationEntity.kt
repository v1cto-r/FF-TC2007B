package mx.tec.sonrisas.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** A copy of what the server has: parents write straight to the API, so there is nothing pending here. */
@Entity(tableName = "pickup_authorizations", indices = [Index("childId")])
data class PickupAuthorizationEntity(
    @PrimaryKey val id: String,
    val childId: String,
    val kind: String,
    val startDate: LocalDate,
    // Bitmask, Monday = 1 ... Sunday = 64 (same as the server)
    val weekdays: Int,
    val authorizedPersonName: String,
    val relationship: String,
    val phone: String,
    val identificationType: String,
    val notes: String?
)
