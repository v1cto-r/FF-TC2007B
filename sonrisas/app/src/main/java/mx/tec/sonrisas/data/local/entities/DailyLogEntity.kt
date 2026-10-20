package mx.tec.sonrisas.data.local.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

/**
 * A single table for the whole daily log: check-in, stay and check-out
 */
@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey val id: String,
    val childId: String,
    val date: LocalDate,
    val caregiverName: String,
    @Embedded(prefix = "check_in_") val checkIn: CheckInEmbed,
    @Embedded(prefix = "stay_") val stay: StayEmbed?,
    @Embedded(prefix = "check_out_") val checkOut: CheckOutEmbed?,
    val synced: Boolean,
    // Deleted locally, waiting for the server to confirm before the row is removed
    val deleted: Boolean = false,
    // Time of the last local change; lets the sync worker tell if the row changed during an upload
    val modifiedAt: Long = 0
)

data class InjuryEmbed(
    val bodyPart: String,
    val description: String
)

data class FeedingEmbed(
    val breakfast: String,
    val snack: String,
    val lunch: String,
    val afternoonSnack: String
)

data class CheckInEmbed(
    val checkInTime: LocalTime,
    val droppedOffByName: String,
    val receivedByStaffName: String,
    val isSick: Boolean,
    val medicationLeftWithPrescription: Boolean?,
    val arrivesWithSymptoms: Boolean,
    val symptomsDescription: String?,
    val arrivedClean: Boolean,
    val broughtCompleteBackpack: Boolean,
    val goodPhysicalCondition: Boolean,
    @Embedded(prefix = "injury_") val injury: InjuryEmbed?,
    val parentsNotes: String?
)

data class StayEmbed(
    @Embedded(prefix = "feeding_") val feeding: FeedingEmbed,
    val feedingNotes: String?,
    val slept: Boolean,
    val napMinutes: Int?,
    val restComments: String?,
    val toiletingNotice: Boolean?,
    val peeCount: Int,
    val poopCount: Int,
    val toiletingNotes: String?,
    val mood: String,
    val cried: Boolean,
    val fought: Boolean,
    val participated: Boolean,
    val moodNotes: String?,
    val hadAccident: Boolean,
    val accidentDescription: String?,
    val accidentReportFolio: String?,
    val healthIssue: Boolean,
    val healthIssueDescription: String?,
    val careProvided: String?,
    val generalHealthNotes: String?
)

data class CheckOutEmbed(
    val checkOutTime: LocalTime,
    val releasedInGoodPhysicalCondition: Boolean,
    @Embedded(prefix = "injury_") val injury: InjuryEmbed?,
    val releasedClean: Boolean,
    val belongingsComplete: Boolean,
    val notes: String?,
    val pickedUpByName: String,
    val releasedByStaffName: String
)
