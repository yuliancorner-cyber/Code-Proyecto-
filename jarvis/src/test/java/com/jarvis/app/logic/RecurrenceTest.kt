package com.jarvis.app.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/** Siguiente fecha de una tarea que se repite. */
class RecurrenceTest {

    private val monday = LocalDate.of(2026, 11, 16)

    @Test
    fun `no repeat gives no next date`() {
        assertNull(Recurrence.next(monday, Repeat.NONE, 0, monday))
    }

    @Test
    fun `daily goes to the next day`() {
        assertEquals(monday.plusDays(1), Recurrence.next(monday, Repeat.DAILY, 0, monday))
    }

    @Test
    fun `daily task done late does not come back in the past`() {
        // Tocaba el lunes, se completa el jueves -> la siguiente es el viernes.
        val thursday = monday.plusDays(3)
        assertEquals(thursday.plusDays(1), Recurrence.next(monday, Repeat.DAILY, 0, thursday))
    }

    @Test
    fun `daily task done early keeps its schedule`() {
        // Tocaba el miercoles y se adelanta el lunes -> la siguiente es el jueves.
        val wednesday = monday.plusDays(2)
        assertEquals(wednesday.plusDays(1), Recurrence.next(wednesday, Repeat.DAILY, 0, monday))
    }

    @Test
    fun `weekdays skip the weekend`() {
        val friday = monday.plusDays(4)
        assertEquals(monday.plusDays(7), Recurrence.next(friday, Repeat.WEEKDAYS, 0, friday))
        assertEquals(monday.plusDays(1), Recurrence.next(monday, Repeat.WEEKDAYS, 0, monday))
    }

    @Test
    fun `weekly without days repeats the same weekday`() {
        assertEquals(monday.plusDays(7), Recurrence.next(monday, Repeat.WEEKLY, 0, monday))
    }

    @Test
    fun `weekly with chosen days goes to the next chosen day`() {
        val monWedFri = Days.bit(DayOfWeek.MONDAY) or Days.bit(DayOfWeek.WEDNESDAY) or Days.bit(DayOfWeek.FRIDAY)
        assertEquals(monday.plusDays(2), Recurrence.next(monday, Repeat.WEEKLY, monWedFri, monday))
        val friday = monday.plusDays(4)
        assertEquals(monday.plusDays(7), Recurrence.next(friday, Repeat.WEEKLY, monWedFri, friday))
    }

    @Test
    fun `monthly keeps the day of the month`() {
        assertEquals(LocalDate.of(2026, 12, 16), Recurrence.next(monday, Repeat.MONTHLY, 0, monday))
    }

    @Test
    fun `monthly clamps to short months`() {
        val jan31 = LocalDate.of(2026, 1, 31)
        assertEquals(LocalDate.of(2026, 2, 28), Recurrence.next(jan31, Repeat.MONTHLY, 0, jan31))
    }

    @Test
    fun `monthly done very late jumps past today`() {
        val done = LocalDate.of(2027, 1, 20)
        assertEquals(LocalDate.of(2027, 2, 16), Recurrence.next(monday, Repeat.MONTHLY, 0, done))
    }

    @Test
    fun `unknown repeat name falls back to none`() {
        assertEquals(Repeat.NONE, Repeat.of("WHATEVER"))
        assertEquals(Repeat.DAILY, Repeat.of("DAILY"))
    }
}
