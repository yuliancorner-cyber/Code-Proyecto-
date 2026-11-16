package com.jarvis.app.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/** Dias de la semana, formato de horas/fechas y calculo de la proxima alarma. */
class TimeAndDaysTest {

    // 16 nov 2026 es lunes.
    private val monday = LocalDate.of(2026, 11, 16)

    @Test
    fun `day masks`() {
        assertTrue(Days.contains(Days.WEEKDAYS, DayOfWeek.FRIDAY))
        assertFalse(Days.contains(Days.WEEKDAYS, DayOfWeek.SATURDAY))
        assertTrue(Days.contains(Days.WEEKEND, DayOfWeek.SUNDAY))
        assertEquals(Days.ALL, Days.WEEKDAYS or Days.WEEKEND)
        assertEquals(0, Days.toggle(Days.bit(DayOfWeek.MONDAY), DayOfWeek.MONDAY))
    }

    @Test
    fun `day labels`() {
        assertEquals("Every day", Days.label(Days.ALL))
        assertEquals("Weekdays", Days.label(Days.WEEKDAYS))
        assertEquals("Weekends", Days.label(Days.WEEKEND))
        assertEquals("Never", Days.label(0))
        val monWedFri = Days.bit(DayOfWeek.MONDAY) or Days.bit(DayOfWeek.WEDNESDAY) or Days.bit(DayOfWeek.FRIDAY)
        assertEquals("Mon, Wed, Fri", Days.label(monWedFri))
    }

    @Test
    fun `time formatting`() {
        assertEquals("5:15 AM", Formats.time(5 * 60 + 15))
        assertEquals("12:00 AM", Formats.time(0))
        assertEquals("12:30 PM", Formats.time(12 * 60 + 30))
        assertEquals("11:59 PM", Formats.time(23 * 60 + 59))
        assertEquals("05:15", Formats.time(5 * 60 + 15, is24Hour = true))
        assertEquals("15:00", Formats.time(15 * 60, is24Hour = true))
    }

    @Test
    fun `relative dates`() {
        assertEquals("Today", Formats.relativeDate(monday, monday))
        assertEquals("Tomorrow", Formats.relativeDate(monday.plusDays(1), monday))
        assertEquals("Yesterday", Formats.relativeDate(monday.minusDays(1), monday))
        assertEquals("Fri, Nov 20", Formats.relativeDate(monday.plusDays(4), monday))
        assertEquals("Monday, November 16", Formats.fullDate(monday))
        assertEquals("November 2026", Formats.monthYear(monday))
    }

    @Test
    fun `degrees round and convert`() {
        assertEquals(14, Formats.degrees(14.4, fahrenheit = false))
        assertEquals(15, Formats.degrees(14.5, fahrenheit = false))
        assertEquals(32, Formats.degrees(0.0, fahrenheit = true))
        assertEquals(68, Formats.degrees(20.0, fahrenheit = true))
    }

    @Test
    fun `lowerFirst keeps acronyms and the pronoun I`() {
        assertEquals("call the bank", Formats.lowerFirst("Call the bank"))
        assertEquals("NASA report", Formats.lowerFirst("NASA report"))
        assertEquals("I need rest", Formats.lowerFirst("I need rest"))
        assertEquals("a walk", Formats.lowerFirst("A walk"))
        assertEquals("", Formats.lowerFirst("  "))
    }

    // ------------------------------------------------------------ Alarma

    @Test
    fun `alarm later today when the time has not passed`() {
        val now = monday.atTime(4, 0)
        assertEquals(monday.atTime(5, 15), AlarmTime.next(now, 5, 15, Days.ALL))
    }

    @Test
    fun `alarm tomorrow when the time already passed`() {
        val now = monday.atTime(5, 15) // justo a la hora: ya no cuenta hoy
        assertEquals(monday.plusDays(1).atTime(5, 15), AlarmTime.next(now, 5, 15, Days.ALL))
    }

    @Test
    fun `alarm skips days that are not selected`() {
        // Viernes 20 a las 6:00, alarma solo entre semana -> lunes 23.
        val friday = LocalDateTime.of(2026, 11, 20, 6, 0)
        assertEquals(LocalDateTime.of(2026, 11, 23, 5, 15), AlarmTime.next(friday, 5, 15, Days.WEEKDAYS))
    }

    @Test
    fun `alarm on a single weekday wraps to next week`() {
        val onlyMonday = Days.bit(DayOfWeek.MONDAY)
        val now = monday.atTime(8, 0)
        assertEquals(monday.plusDays(7).atTime(5, 15), AlarmTime.next(now, 5, 15, onlyMonday))
    }

    @Test
    fun `no alarm when no day is selected`() {
        assertNull(AlarmTime.next(monday.atTime(4, 0), 5, 15, 0))
    }
}
