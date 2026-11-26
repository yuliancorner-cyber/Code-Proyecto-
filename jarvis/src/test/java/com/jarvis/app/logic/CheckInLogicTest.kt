package com.jarvis.app.logic

import com.jarvis.app.data.Habit
import com.jarvis.app.data.HabitCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/** Que habitos pregunta cada check-in, a que horas y con que palabras. */
class CheckInLogicTest {

    // 16 nov 2026 es lunes.
    private val today = LocalDate.of(2026, 11, 16)
    private val day = today.toEpochDay()

    private val reading = Habit(id = 1, name = "Reading", emoji = "📖", behavior = "read 10 pages", position = 0)
    private val sleep = Habit(id = 2, name = "Go to Sleep early", emoji = "😴", position = 1)
    private val water = Habit(id = 4, name = "Water", emoji = "💧", target = 8, unit = "glasses", position = 3)
    private val gratitude = Habit(id = 10, name = "Night gratitude", position = 9, timeMinutes = 22 * 60)
    private val all = listOf(gratitude, water, sleep, reading) // desordenados a proposito

    private fun pending(
        checks: List<HabitCheck> = emptyList(),
        habits: List<Habit> = all,
        now: Int = 12 * 60,
        everything: Boolean = false,
        skipped: Set<Long> = emptySet(),
        later: Set<Long> = emptySet()
    ) = CheckInLogic.pending(habits, CheckIndex(checks), today, now, everything, skipped, later).map { it.id }

    @Test
    fun `pending habits follow the grid order`() {
        assertEquals(listOf(1L, 2L, 4L), pending())
    }

    @Test
    fun `habits already done are not asked`() {
        assertEquals(listOf(2L, 4L), pending(checks = listOf(HabitCheck(1, day, 1))))
        // Agua a medias (3 de 8) sigue pendiente; completa, no.
        assertEquals(listOf(4L), pending(checks = listOf(HabitCheck(1, day, 1), HabitCheck(2, day, 1), HabitCheck(4, day, 3))))
        assertEquals(emptyList<Long>(), pending(checks = listOf(HabitCheck(1, day, 1), HabitCheck(2, day, 1), HabitCheck(4, day, 8))))
        // Lo de ayer no cuenta para hoy.
        assertEquals(listOf(1L, 2L, 4L), pending(checks = listOf(HabitCheck(1, day - 1, 1))))
    }

    @Test
    fun `not today and later remove a habit from the round`() {
        assertEquals(listOf(4L), pending(skipped = setOf(1L), later = setOf(2L)))
    }

    @Test
    fun `a habit planned for later waits until its time or the last round`() {
        assertFalse(10L in pending(now = 21 * 60))
        assertTrue(10L in pending(now = 22 * 60))
        assertTrue(10L in pending(now = 21 * 60, everything = true))
        // Con el plan "despues de otro habito", la hora guardada no cuenta.
        val stacked = gratitude.copy(cue = Habit.CUE_STACK)
        assertEquals(listOf(10L), pending(habits = listOf(stacked), now = 9 * 60))
    }

    @Test
    fun `switched off, archived and unscheduled habits are never asked`() {
        val habits = listOf(
            reading.copy(askInCheckIns = false),
            sleep.copy(archived = true),
            water.copy(days = Days.WEEKEND) // hoy es lunes
        )
        assertEquals(emptyList<Long>(), pending(habits = habits, everything = true))
    }

    @Test
    fun `check-in times are cleaned up`() {
        assertEquals(listOf(60, 720, 1260), CheckInLogic.normalize(listOf(1260, 720, 720, 60, -5, 1440)))
        assertEquals(CheckInLogic.MAX_TIMES, CheckInLogic.normalize((0 until 20).map { it * 60 }).size)
    }

    @Test
    fun `the last round of the day asks about everything`() {
        val times = listOf(12 * 60, 17 * 60, 21 * 60)
        assertFalse(CheckInLogic.isLastOfDay(times, 12 * 60))
        assertTrue(CheckInLogic.isLastOfDay(times, 21 * 60))
        assertTrue(CheckInLogic.isLastOfDay(emptyList(), 9 * 60))
    }

    @Test
    fun `next round today and next occurrence`() {
        val times = listOf(21 * 60, 12 * 60, 17 * 60)
        assertEquals(17 * 60, CheckInLogic.nextToday(times, 12 * 60))
        assertNull(CheckInLogic.nextToday(times, 21 * 60))
        val noon = today.atTime(12, 0)
        assertEquals(today.atTime(17, 0), CheckInLogic.nextOccurrence(17 * 60, noon))
        assertEquals(today.plusDays(1).atTime(12, 0), CheckInLogic.nextOccurrence(12 * 60, noon))
        assertEquals(today.plusDays(1).atTime(9, 0), CheckInLogic.nextOccurrence(9 * 60, LocalDateTime.of(2026, 11, 16, 23, 59)))
    }

    @Test
    fun `questions read naturally`() {
        assertEquals("Did you read 10 pages today?", CheckInLogic.question(reading, 0))
        assertEquals("Did you keep this habit today?", CheckInLogic.question(sleep, 0))
        assertEquals("3 of 8 glasses so far today.", CheckInLogic.question(water, 3))
        assertEquals("0 of 3 times so far today.", CheckInLogic.question(water.copy(target = 3, unit = ""), 0))
        assertEquals("📖 Reading", CheckInLogic.title(reading))
        assertEquals("Night gratitude", CheckInLogic.title(gratitude))
        assertTrue(CheckInLogic.isCounter(water))
        assertFalse(CheckInLogic.isCounter(reading))
    }

    @Test
    fun `the hint prefers never miss twice, then the plan, then the tiny version`() {
        val planned = reading.copy(timeMinutes = 21 * 60, location = "my bedroom", tinyVersion = "read one page")
        assertEquals("You missed it last time. Never miss twice.", CheckInLogic.hint(planned, true, false))
        assertEquals("I will read 10 pages at 9:00 PM in my bedroom.", CheckInLogic.hint(planned, false, false))
        assertEquals("Two-minute version: read one page", CheckInLogic.hint(reading.copy(tinyVersion = "read one page"), false, false))
        assertNull(CheckInLogic.hint(sleep, false, false))
    }

    @Test
    fun `the closing message`() {
        assertEquals("All caught up" to "10 of 10 habits done today. Great work.", CheckInLogic.summary(10, 10, "5:00 PM"))
        assertEquals("Got it" to "7 of 10 habits done today. I'll ask again at 5:00 PM.", CheckInLogic.summary(7, 10, "5:00 PM"))
        assertEquals("Got it" to "7 of 10 habits done today. See you tomorrow.", CheckInLogic.summary(7, 10, null))
    }
}
