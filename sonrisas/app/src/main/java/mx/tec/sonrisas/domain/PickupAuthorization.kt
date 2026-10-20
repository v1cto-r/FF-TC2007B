package mx.tec.sonrisas.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

enum class PickupKind {
    /** Some days of one week only; startDate is that week's Monday. Ends with the week. */
    THIS_WEEK,

    /** Some weekdays of every week from startDate on, until the parent cancels it. */
    RECURRING
}

/**
 * Someone other than the parent who may pick the child up. The server sets startDate; the rules
 * below are the same as the backend's (app/pickups.py), so the teacher sees the same answer offline.
 */
data class PickupAuthorization(
    val id: String = UUID.randomUUID().toString(),
    val childId: String,
    val kind: PickupKind,
    val weekdays: Set<DayOfWeek>,
    val startDate: LocalDate = LocalDate.now(),
    val authorizedPersonName: String,
    val relationship: String,
    val phone: String,
    val identificationType: String = "INE",
    val notes: String? = null
) {
    /** Still in force: recurring ones until cancelled, THIS_WEEK ones until their week ends. */
    fun isCurrent(today: LocalDate = LocalDate.now()): Boolean =
        kind == PickupKind.RECURRING || !startDate.isBefore(weekStart(today))

    fun appliesOn(date: LocalDate): Boolean {
        if (date.dayOfWeek !in weekdays) return false
        return when (kind) {
            PickupKind.THIS_WEEK -> !date.isBefore(startDate) && date.isBefore(startDate.plusDays(7))
            PickupKind.RECURRING -> !date.isBefore(startDate)
        }
    }
}

/** The one that applies that day: a THIS_WEEK authorization wins over a RECURRING one. */
fun List<PickupAuthorization>.resolveFor(date: LocalDate): PickupAuthorization? {
    val applying = filter { it.appliesOn(date) }
    return applying.firstOrNull { it.kind == PickupKind.THIS_WEEK } ?: applying.firstOrNull()
}

fun weekStart(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - 1).toLong())

data class WeekDay(val date: LocalDate, val isPast: Boolean)

/** Monday to Sunday of the week of [today], with the days already gone marked, for the THIS_WEEK form. */
fun currentWeek(today: LocalDate = LocalDate.now()): List<WeekDay> =
    (0L..6L).map { weekStart(today).plusDays(it) }.map { WeekDay(it, it.isBefore(today)) }
