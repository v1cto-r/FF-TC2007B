package mx.tec.sonrisas.domain

import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Same cases as the backend's test_pickups_api.py: the phone and the server must agree on who picks up. */
class PickupAuthorizationTest {

    private val today = LocalDate.of(2026, 10, 21)  // a Wednesday; its week starts on Monday the 19th

    private fun auth(kind: PickupKind, start: LocalDate, vararg days: java.time.DayOfWeek) = PickupAuthorization(
        childId = "c1", kind = kind, weekdays = days.toSet(), startDate = start,
        authorizedPersonName = "Abuela", relationship = "Abuela", phone = "55"
    )

    private val weekly = auth(PickupKind.RECURRING, today, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
    private val thisFriday = auth(PickupKind.THIS_WEEK, LocalDate.of(2026, 10, 19), FRIDAY)

    @Test
    fun `this week wins over recurring on its days only`() {
        val both = listOf(weekly, thisFriday)
        assertSame(weekly, both.resolveFor(LocalDate.of(2026, 10, 22)))      // Thursday
        assertSame(thisFriday, both.resolveFor(LocalDate.of(2026, 10, 23)))  // this Friday
        assertNull(both.resolveFor(LocalDate.of(2026, 10, 24)))              // Saturday: nobody
        assertSame(weekly, both.resolveFor(LocalDate.of(2026, 10, 30)))      // next Friday: the week is over
        assertNull(both.resolveFor(LocalDate.of(2026, 10, 20)))              // before the recurring one started
    }

    @Test
    fun `a this week authorization stops being current when its week ends`() {
        assertTrue(thisFriday.isCurrent(LocalDate.of(2026, 10, 25)))   // Sunday, same week
        assertFalse(thisFriday.isCurrent(LocalDate.of(2026, 10, 26)))  // next Monday
        assertTrue(weekly.isCurrent(LocalDate.of(2027, 1, 1)))         // recurring: until cancelled
    }

    @Test
    fun `current week runs monday to sunday with past days marked`() {
        val week = currentWeek(today)
        assertEquals(LocalDate.of(2026, 10, 19), week.first().date)
        assertEquals(LocalDate.of(2026, 10, 25), week.last().date)
        assertEquals(listOf(true, true, false, false, false, false, false), week.map { it.isPast })
    }
}
