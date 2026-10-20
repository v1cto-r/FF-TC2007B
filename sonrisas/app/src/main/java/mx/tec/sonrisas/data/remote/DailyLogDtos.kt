package mx.tec.sonrisas.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mx.tec.sonrisas.domain.ActivityRecord
import mx.tec.sonrisas.domain.CheckInRecord
import mx.tec.sonrisas.domain.CheckOutRecord
import mx.tec.sonrisas.domain.DailyLog
import mx.tec.sonrisas.domain.Stay

// Request bodies of PUT /daily-logs/{id} and PUT /activities/{id} (backend app/schemas/logs.py)

@Serializable
data class DailyLogInBody(
    @SerialName("child_id") val childId: String,
    val date: String,
    @SerialName("caregiver_name") val caregiverName: String,
    @SerialName("check_in") val checkIn: CheckInBody,
    val stay: StayBody? = null,
    @SerialName("check_out") val checkOut: CheckOutBody? = null
)

@Serializable
data class CheckInBody(
    @SerialName("check_in_time") val checkInTime: String,
    @SerialName("dropped_off_by_name") val droppedOffByName: String,
    @SerialName("received_by_staff_name") val receivedByStaffName: String,
    @SerialName("is_sick") val isSick: Boolean,
    @SerialName("medication_left_with_prescription") val medicationLeftWithPrescription: Boolean? = null,
    @SerialName("arrives_with_symptoms") val arrivesWithSymptoms: Boolean,
    @SerialName("symptoms_description") val symptomsDescription: String? = null,
    @SerialName("arrived_clean") val arrivedClean: Boolean,
    @SerialName("brought_complete_backpack") val broughtCompleteBackpack: Boolean,
    @SerialName("good_physical_condition") val goodPhysicalCondition: Boolean,
    @SerialName("injury_body_part") val injuryBodyPart: String? = null,
    @SerialName("injury_description") val injuryDescription: String? = null,
    @SerialName("parents_notes") val parentsNotes: String? = null
)

@Serializable
data class StayBody(
    @SerialName("feeding_breakfast") val feedingBreakfast: String,
    @SerialName("feeding_snack") val feedingSnack: String,
    @SerialName("feeding_lunch") val feedingLunch: String,
    @SerialName("feeding_notes") val feedingNotes: String? = null,
    val slept: Boolean,
    @SerialName("nap_minutes") val napMinutes: Int? = null,
    @SerialName("rest_comments") val restComments: String? = null,
    @SerialName("toileting_notice") val toiletingNotice: Boolean? = null,
    @SerialName("pee_count") val peeCount: Int,
    @SerialName("poop_count") val poopCount: Int,
    @SerialName("toileting_notes") val toiletingNotes: String? = null,
    val mood: String,
    val cried: Boolean,
    val fought: Boolean,
    val participated: Boolean,
    @SerialName("mood_notes") val moodNotes: String? = null,
    @SerialName("had_accident") val hadAccident: Boolean,
    @SerialName("accident_description") val accidentDescription: String? = null,
    @SerialName("accident_report_folio") val accidentReportFolio: String? = null,
    @SerialName("health_issue") val healthIssue: Boolean,
    @SerialName("health_issue_description") val healthIssueDescription: String? = null,
    @SerialName("care_provided") val careProvided: String? = null,
    @SerialName("general_health_notes") val generalHealthNotes: String? = null
)

@Serializable
data class CheckOutBody(
    @SerialName("check_out_time") val checkOutTime: String,
    @SerialName("released_in_good_physical_condition") val releasedInGoodPhysicalCondition: Boolean,
    @SerialName("injury_body_part") val injuryBodyPart: String? = null,
    @SerialName("injury_description") val injuryDescription: String? = null,
    @SerialName("released_clean") val releasedClean: Boolean,
    @SerialName("belongings_complete") val belongingsComplete: Boolean,
    val notes: String? = null,
    @SerialName("picked_up_by_name") val pickedUpByName: String,
    @SerialName("released_by_staff_name") val releasedByStaffName: String
)

@Serializable
data class ActivityInBody(
    @SerialName("daily_log_id") val dailyLogId: String,
    val name: String,
    @SerialName("image_file_ids") val imageFileIds: List<String> = emptyList()
)

fun DailyLog.toBody() = DailyLogInBody(
    childId = childId,
    date = date.toString(),
    caregiverName = caregiverName,
    checkIn = checkIn.toBody(),
    stay = stay?.toBody(),
    checkOut = checkOut?.toBody()
)

private fun CheckInRecord.toBody() = CheckInBody(
    checkInTime = checkInTime.toString(),
    droppedOffByName = droppedOffByName,
    receivedByStaffName = receivedByStaffName,
    isSick = isSick,
    medicationLeftWithPrescription = medicationLeftWithPrescription,
    arrivesWithSymptoms = arrivesWithSymptoms,
    symptomsDescription = symptomsDescription,
    arrivedClean = arrivedClean,
    broughtCompleteBackpack = broughtCompleteBackpack,
    goodPhysicalCondition = goodPhysicalCondition,
    injuryBodyPart = injury?.bodyPart,
    injuryDescription = injury?.description,
    parentsNotes = parentsNotes
)

private fun Stay.toBody() = StayBody(
    feedingBreakfast = feeding.breakfast.name,
    feedingSnack = feeding.snack.name,
    feedingLunch = feeding.lunch.name,
    feedingNotes = feedingNotes,
    slept = slept,
    napMinutes = napMinutes,
    restComments = restComments,
    toiletingNotice = toiletingNotice,
    peeCount = peeCount,
    poopCount = poopCount,
    toiletingNotes = toiletingNotes,
    mood = mood,
    cried = cried,
    fought = fought,
    participated = participated,
    moodNotes = moodNotes,
    hadAccident = hadAccident,
    accidentDescription = accidentDescription,
    accidentReportFolio = accidentReportFolio,
    healthIssue = healthIssue,
    healthIssueDescription = healthIssueDescription,
    careProvided = careProvided,
    generalHealthNotes = generalHealthNotes
)

private fun CheckOutRecord.toBody() = CheckOutBody(
    checkOutTime = checkOutTime.toString(),
    releasedInGoodPhysicalCondition = releasedInGoodPhysicalCondition,
    injuryBodyPart = injury?.bodyPart,
    injuryDescription = injury?.description,
    releasedClean = releasedClean,
    belongingsComplete = belongingsComplete,
    notes = notes,
    pickedUpByName = pickedUpByName,
    releasedByStaffName = releasedByStaffName
)

fun ActivityRecord.toBody() = ActivityInBody(dailyLogId = dailyLogId, name = name, imageFileIds = images)
