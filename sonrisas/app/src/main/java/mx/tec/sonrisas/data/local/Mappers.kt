package mx.tec.sonrisas.data.local

import java.time.DayOfWeek
import mx.tec.sonrisas.data.local.dao.MessageDao
import mx.tec.sonrisas.data.local.entities.ActivityRecordEntity
import mx.tec.sonrisas.data.local.entities.CheckInEmbed
import mx.tec.sonrisas.data.local.entities.CheckOutEmbed
import mx.tec.sonrisas.data.local.entities.ChildEntity
import mx.tec.sonrisas.data.local.entities.DailyLogEntity
import mx.tec.sonrisas.data.local.entities.FeedingEmbed
import mx.tec.sonrisas.data.local.entities.InjuryEmbed
import mx.tec.sonrisas.data.local.entities.MessageEntity
import mx.tec.sonrisas.data.local.entities.NoticeEntity
import mx.tec.sonrisas.data.local.entities.PickupAuthorizationEntity
import mx.tec.sonrisas.data.local.entities.StayEmbed
import mx.tec.sonrisas.domain.ActivityRecord
import mx.tec.sonrisas.domain.AmountEnum
import mx.tec.sonrisas.domain.CheckInRecord
import mx.tec.sonrisas.domain.CheckOutRecord
import mx.tec.sonrisas.domain.Child
import mx.tec.sonrisas.domain.DailyLog
import mx.tec.sonrisas.domain.Feeding
import mx.tec.sonrisas.domain.GroupEnum
import mx.tec.sonrisas.domain.Injury
import mx.tec.sonrisas.domain.Message
import mx.tec.sonrisas.domain.Notice
import mx.tec.sonrisas.domain.PickupAuthorization
import mx.tec.sonrisas.domain.PickupKind
import mx.tec.sonrisas.domain.Stay

/**
 * Translators between what Room stores and what the domain understands.
 * Boring on purpose: if a column changes name, the tremor stops here.
 */

// --- Child ---

fun ChildEntity.toDomain() = Child(
    id = id,
    fullName = fullName,
    daycareCenter = daycareCenter,
    group = GroupEnum.valueOf(group),
    parentName = parentName,
    birthdate = birthdate,
    gender = gender,
    allergies = allergies,
    bloodType = bloodType,
    emergencyContactPhone = emergencyContactPhone,
    profileCompletedAt = profileCompletedAt
)

fun Child.toEntity() = ChildEntity(
    id = id,
    fullName = fullName,
    daycareCenter = daycareCenter,
    group = group.name,
    parentName = parentName,
    birthdate = birthdate,
    gender = gender,
    allergies = allergies,
    bloodType = bloodType,
    emergencyContactPhone = emergencyContactPhone,
    profileCompletedAt = profileCompletedAt
)

// --- Injury / Feeding ---

private fun InjuryEmbed.toDomain() = Injury(bodyPart = bodyPart, description = description)

private fun Injury.toEmbed() = InjuryEmbed(bodyPart = bodyPart, description = description)

private fun FeedingEmbed.toDomain() = Feeding(
    breakfast = AmountEnum.valueOf(breakfast),
    snack = AmountEnum.valueOf(snack),
    lunch = AmountEnum.valueOf(lunch),
    afternoonSnack = AmountEnum.valueOf(afternoonSnack)
)

private fun Feeding.toEmbed() = FeedingEmbed(
    breakfast = breakfast.name,
    snack = snack.name,
    lunch = lunch.name,
    afternoonSnack = afternoonSnack.name
)

// --- CheckInRecord / Stay / CheckOutRecord ---

private fun CheckInEmbed.toDomain() = CheckInRecord(
    checkInTime = checkInTime,
    droppedOffByName = droppedOffByName,
    receivedByStaffName = receivedByStaffName,
    isSick = isSick,
    medicationLeftWithPrescription = medicationLeftWithPrescription,
    arrivesWithSymptoms = arrivesWithSymptoms,
    symptomsDescription = symptomsDescription,
    arrivedClean = arrivedClean,
    broughtCompleteBackpack = broughtCompleteBackpack,
    goodPhysicalCondition = goodPhysicalCondition,
    injury = injury?.toDomain(),
    parentsNotes = parentsNotes
)

private fun CheckInRecord.toEmbed() = CheckInEmbed(
    checkInTime = checkInTime,
    droppedOffByName = droppedOffByName,
    receivedByStaffName = receivedByStaffName,
    isSick = isSick,
    medicationLeftWithPrescription = medicationLeftWithPrescription,
    arrivesWithSymptoms = arrivesWithSymptoms,
    symptomsDescription = symptomsDescription,
    arrivedClean = arrivedClean,
    broughtCompleteBackpack = broughtCompleteBackpack,
    goodPhysicalCondition = goodPhysicalCondition,
    injury = injury?.toEmbed(),
    parentsNotes = parentsNotes
)

private fun StayEmbed.toDomain() = Stay(
    feeding = feeding.toDomain(),
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

private fun Stay.toEmbed() = StayEmbed(
    feeding = feeding.toEmbed(),
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

private fun CheckOutEmbed.toDomain() = CheckOutRecord(
    checkOutTime = checkOutTime,
    releasedInGoodPhysicalCondition = releasedInGoodPhysicalCondition,
    injury = injury?.toDomain(),
    releasedClean = releasedClean,
    belongingsComplete = belongingsComplete,
    notes = notes,
    pickedUpByName = pickedUpByName,
    releasedByStaffName = releasedByStaffName,
    pickupAuthorizationId = pickupAuthorizationId,
    idPhotoFileId = idPhotoFileId,
    idPhotoLocalPath = idPhotoLocalPath
)

private fun CheckOutRecord.toEmbed() = CheckOutEmbed(
    checkOutTime = checkOutTime,
    releasedInGoodPhysicalCondition = releasedInGoodPhysicalCondition,
    injury = injury?.toEmbed(),
    releasedClean = releasedClean,
    belongingsComplete = belongingsComplete,
    notes = notes,
    pickedUpByName = pickedUpByName,
    releasedByStaffName = releasedByStaffName,
    pickupAuthorizationId = pickupAuthorizationId,
    idPhotoFileId = idPhotoFileId,
    idPhotoLocalPath = idPhotoLocalPath
)

// --- DailyLog ---

fun DailyLogEntity.toDomain() = DailyLog(
    id = id,
    childId = childId,
    date = date,
    caregiverName = caregiverName,
    checkIn = checkIn.toDomain(),
    stay = stay?.toDomain(),
    checkOut = checkOut?.toDomain(),
    checkOutCleared = checkOutCleared,
    synced = synced
)

fun DailyLog.toEntity() = DailyLogEntity(
    id = id,
    childId = childId,
    date = date,
    caregiverName = caregiverName,
    checkIn = checkIn.toEmbed(),
    stay = stay?.toEmbed(),
    checkOut = checkOut?.toEmbed(),
    checkOutCleared = checkOutCleared,
    synced = synced
)

// --- ActivityRecord ---

fun ActivityRecordEntity.toDomain() = ActivityRecord(
    id = id,
    dailyLogId = dailyLogId,
    name = name,
    images = images,
    pendingImagePaths = pendingImagePaths,
    synced = synced
)

fun ActivityRecord.toEntity() = ActivityRecordEntity(
    id = id,
    dailyLogId = dailyLogId,
    name = name,
    images = images,
    pendingImagePaths = pendingImagePaths,
    synced = synced
)

// --- Notice ---

fun NoticeEntity.toDomain() = Notice(id = id, name = name, description = description, date = date)

fun Notice.toEntity() = NoticeEntity(id = id, name = name, description = description, date = date)

/// --- Message ---

fun MessageEntity.toDomain() = Message(
    id = id,
    chatId = chatId,
    authorId = authorId,
    content = content,
    sentAt = sentAt,
    sent = sent,
    synced = synced,
    isAviso = isAviso,
    deleted = deleted,
    deletePending = deletePending,
    createdAtMs = createdAtMs,
    attachmentFileId = attachmentFileId,
    sendError = sendError
)

fun Message.toEntity() = MessageEntity(
    id = id,
    chatId = chatId,
    authorId = authorId,
    content = content,
    sentAt = sentAt,
    sent = sent,
    synced = synced,
    isAviso = isAviso,
    deleted = deleted,
    deletePending = deletePending,
    createdAtMs = createdAtMs,
    attachmentFileId = attachmentFileId,
    sendError = sendError
)

// --- PickupAuthorization ---

fun PickupAuthorizationEntity.toDomain() = PickupAuthorization(
    id = id,
    childId = childId,
    kind = PickupKind.valueOf(kind),
    weekdays = weekdaysOf(weekdays),
    startDate = startDate,
    authorizedPersonName = authorizedPersonName,
    relationship = relationship,
    phone = phone,
    identificationType = identificationType,
    notes = notes
)

fun PickupAuthorization.toEntity() = PickupAuthorizationEntity(
    id = id,
    childId = childId,
    kind = kind.name,
    startDate = startDate,
    weekdays = weekdays.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) },
    authorizedPersonName = authorizedPersonName,
    relationship = relationship,
    phone = phone,
    identificationType = identificationType,
    notes = notes
)

private fun weekdaysOf(mask: Int): Set<DayOfWeek> =
    DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()

suspend fun MessageDao.saveFromServer(messages: List<Message>) {
    messages.forEach { incoming ->
        val local = getById(incoming.id)
        save(incoming.copy(deleted = local?.deleted ?: false, deletePending = local?.deletePending ?: false).toEntity())
    }
}
