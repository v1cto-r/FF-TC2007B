package mx.tec.sonrisas.domain

data class Stay(
    val feeding: Feeding,
    val feedingNotes: String? = null,
    val slept: Boolean,
    val napMinutes: Int? = null,
    val restComments: String? = null,
    val toiletingNotice: Boolean? = null,
    val peeCount: Int,
    val poopCount: Int,
    val toiletingNotes: String? = null,
    val mood: String,
    val cried: Boolean,
    val fought: Boolean,
    val participated: Boolean,
    val moodNotes: String? = null,
    val hadAccident: Boolean,
    val accidentDescription: String? = null,
    val accidentReportFolio: String? = null,
    val healthIssue: Boolean,
    val healthIssueDescription: String? = null,
    val careProvided: String? = null,
    val generalHealthNotes: String? = null
)
