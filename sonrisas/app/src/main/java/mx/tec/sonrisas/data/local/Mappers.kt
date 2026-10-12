package mx.tec.sonrisas.data.local

import mx.tec.sonrisas.data.local.entities.ActivityRecordEntity
import mx.tec.sonrisas.data.local.entities.CheckInEmbed
import mx.tec.sonrisas.data.local.entities.CheckOutEmbed
import mx.tec.sonrisas.data.local.entities.ChildEntity
import mx.tec.sonrisas.data.local.entities.DailyLogEntity
import mx.tec.sonrisas.data.local.entities.FeedingEmbed
import mx.tec.sonrisas.data.local.entities.InjuryEmbed
import mx.tec.sonrisas.data.local.entities.MessageEntity
import mx.tec.sonrisas.data.local.entities.NoticeEntity
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
    parentName = parentName
)

fun Child.toEntity() = ChildEntity(
    id = id,
    fullName = fullName,
    daycareCenter = daycareCenter,
    group = group.name,
    parentName = parentName
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
    releasedByStaffName = releasedByStaffName
)

private fun CheckOutRecord.toEmbed() = CheckOutEmbed(
    checkOutTime = checkOutTime,
    releasedInGoodPhysicalCondition = releasedInGoodPhysicalCondition,
    injury = injury?.toEmbed(),
    releasedClean = releasedClean,
    belongingsComplete = belongingsComplete,
    notes = notes,
    pickedUpByName = pickedUpByName,
    releasedByStaffName = releasedByStaffName
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
    synced = synced
)

// --- ActivityRecord ---

fun ActivityRecordEntity.toDomain() = ActivityRecord(
    id = id,
    dailyLogId = dailyLogId,
    name = name,
    images = images,
    synced = synced
)

fun ActivityRecord.toEntity() = ActivityRecordEntity(
    id = id,
    dailyLogId = dailyLogId,
    name = name,
    images = images,
    synced = synced
)

// --- Notice ---

fun NoticeEntity.toDomain() = Notice(id = id, name = name, description = description, date = date)

fun Notice.toEntity() = NoticeEntity(id = id, name = name, description = description, date = date)

// --- Message ---

fun MessageEntity.toDomain() = Message(
    id = id,
    chatId = chatId,
    authorId = authorId,
    content = content,
    sentAt = sentAt,
    sent = sent,
    synced = synced,
    isAviso = isAviso,
    hiddenForParent = hiddenForParent,
    hiddenForTeacher = hiddenForTeacher
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
    hiddenForParent = hiddenForParent,
    hiddenForTeacher = hiddenForTeacher
)

// --- PickupAuthorization ---

fun mx.tec.sonrisas.data.local.entities.PickupAuthorizationEntity.toDomain() = mx.tec.sonrisas.domain.PickupAuthorization(
    id = id,
    childId = childId,
    childName = childName,
    date = date,
    authorizedPersonName = authorizedPersonName,
    relationship = relationship,
    phone = phone,
    identificationType = identificationType,
    notes = notes,
    registeredByParentName = registeredByParentName
)

fun mx.tec.sonrisas.domain.PickupAuthorization.toEntity() = mx.tec.sonrisas.data.local.entities.PickupAuthorizationEntity(
    id = id,
    childId = childId,
    childName = childName,
    date = date,
    authorizedPersonName = authorizedPersonName,
    relationship = relationship,
    phone = phone,
    identificationType = identificationType,
    notes = notes,
    registeredByParentName = registeredByParentName
)
