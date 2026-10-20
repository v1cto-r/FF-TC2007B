package mx.tec.sonrisas.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "pickup_authorizations", indices = [Index("childId"), Index("date")])
data class PickupAuthorizationEntity(
    @PrimaryKey val id: String,
    val childId: String,
    val childName: String,
    val date: LocalDate,
    val authorizedPersonName: String,
    val relationship: String,
    val phone: String,
    val identificationType: String,
    val notes: String?,
    val registeredByParentName: String
)
