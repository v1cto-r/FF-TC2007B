package mx.tec.sonrisas.domain

import java.time.LocalTime

data class CheckInRecord(
    val checkInTime: LocalTime,
    val droppedOffByName: String,
    val receivedByStaffName: String,
    val isSick: Boolean,
    val medicationLeftWithPrescription: Boolean? = null,
    val arrivesWithSymptoms: Boolean,
    val symptomsDescription: String? = null,
    val arrivedClean: Boolean,
    val broughtCompleteBackpack: Boolean,
    val goodPhysicalCondition: Boolean,
    val injury: Injury? = null,
    val parentsNotes: String? = null
)
