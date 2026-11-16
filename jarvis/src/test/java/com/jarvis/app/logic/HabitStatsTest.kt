package com.jarvis.app.logic

import com.jarvis.app.data.Habit
import com.jarvis.app.data.HabitCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Rachas, "never miss twice", porcentajes del mes y recordatorios de habitos. */
class HabitStatsTest {

    // 16 nov 2026 es lunes.
    private val today = LocalDate.of(2026, 11, 16)

    /** Dias hechos, contados hacia atras desde hoy (0 = hoy, 1 = ayer...). */
    private fun daysAgo(vararg offsets: Int): Set<Long> = offsets.map { today.minusDays(it.toLong()).toEpochDay() }.toSet()

    @Test
    fun `done needs to reach the daily target`() {
        assertTrue(HabitStats.isDone(1, 1))
        assertFalse(HabitStats.isDone(0, 1))
        assertFalse(HabitStats.isDone(7, 8))
        assertTrue(HabitStats.isDone(8, 8))
        assertTrue(HabitStats.isDone(1, 0)) // meta invalida se trata como 1
    }

    @Test
    fun `streak counts consecutive days including today`() {
        assertEquals(3, HabitStats.currentStreak(Days.ALL, daysAgo(0, 1, 2), today))
    }

    @Test
    fun `streak survives while today is still pending`() {
        assertEquals(2, HabitStats.currentStreak(Days.ALL, daysAgo(1, 2), today))
    }

    @Test
    fun `streak breaks on a missed scheduled day`() {
        assertEquals(1, HabitStats.currentStreak(Days.ALL, daysAgo(0, 2, 3), today))
        assertEquals(0, HabitStats.currentStreak(Days.ALL, daysAgo(2, 3), today))
        assertEquals(0, HabitStats.currentStreak(Days.ALL, emptySet(), today))
    }

    @Test
    fun `days that are not scheduled do not break the streak`() {
        // Solo entre semana: hecho hoy lunes y el viernes y jueves pasados.
        assertEquals(3, HabitStats.currentStreak(Days.WEEKDAYS, daysAgo(0, 3, 4), today))
    }

    @Test
    fun `best streak looks at the whole history`() {
        // Racha de 4 hace tiempo, luego un fallo, luego 2.
        assertEquals(4, HabitStats.bestStreak(Days.ALL, daysAgo(0, 1, 3, 4, 5, 6), today))
        assertEquals(0, HabitStats.bestStreak(Days.ALL, emptySet(), today))
    }

    @Test
    fun `never miss twice warns after one miss`() {
        // Lo hizo anteayer, fallo ayer y hoy aun no.
        assertTrue(HabitStats.missedLastTime(Days.ALL, daysAgo(2), today))
    }

    @Test
    fun `no warning when yesterday was done or today is done`() {
        assertFalse(HabitStats.missedLastTime(Days.ALL, daysAgo(1), today))
        assertFalse(HabitStats.missedLastTime(Days.ALL, daysAgo(0, 2), today))
    }

    @Test
    fun `no warning for a habit without history`() {
        assertFalse(HabitStats.missedLastTime(Days.ALL, emptySet(), today))
    }

    @Test
    fun `never miss twice respects the schedule`() {
        // Entre semana: el ultimo dia que tocaba fue el viernes (hace 3 dias).
        assertTrue(HabitStats.missedLastTime(Days.WEEKDAYS, daysAgo(4), today))
        assertFalse(HabitStats.missedLastTime(Days.WEEKDAYS, daysAgo(3), today))
    }

    @Test
    fun `month summary counts only days so far`() {
        val november = YearMonth.of(2026, 11)
        val done = (1..12).map { november.atDay(it).toEpochDay() }.toSet()
        val summary = HabitStats.month(Days.ALL, done, november, today)
        assertEquals(12, summary.done)
        assertEquals(16, summary.expected) // del 1 al 16
        assertEquals(75, summary.percent)
    }

    @Test
    fun `past month uses the whole month`() {
        val october = YearMonth.of(2026, 10)
        val summary = HabitStats.month(Days.ALL, emptySet(), october, today)
        assertEquals(0, summary.done)
        assertEquals(31, summary.expected)
        assertEquals(0, summary.percent)
    }

    @Test
    fun `future month expects nothing`() {
        val summary = HabitStats.month(Days.ALL, emptySet(), YearMonth.of(2026, 12), today)
        assertEquals(0, summary.expected)
        assertEquals(0, summary.percent)
    }

    @Test
    fun `month percent never exceeds 100 with extra days`() {
        // Habito de fin de semana hecho tambien un lunes.
        val november = YearMonth.of(2026, 11)
        val done = setOf(1, 7, 8, 14, 15, 16).map { november.atDay(it).toEpochDay() }.toSet()
        val summary = HabitStats.month(Days.WEEKEND, done, november, today)
        assertEquals(100, summary.percent)
    }

    @Test
    fun `day percent like the spreadsheet`() {
        // 10 habitos diarios, 7 hechos -> 70 %.
        val habits = List(10) { Days.ALL to (it < 7) }
        assertEquals(70, HabitStats.dayPercent(habits, today))
        assertEquals(0, HabitStats.dayPercent(emptyList(), today))
    }

    @Test
    fun `day percent ignores habits that are not scheduled`() {
        // Lunes: uno diario hecho y uno de fin de semana sin hacer -> 100 %.
        assertEquals(100, HabitStats.dayPercent(listOf(Days.ALL to true, Days.WEEKEND to false), today))
    }

    @Test
    fun `check index answers counts and done days`() {
        val water = Habit(id = 4, name = "Water", target = 8)
        val day = today.toEpochDay()
        val index = CheckIndex(
            listOf(HabitCheck(4, day, 3), HabitCheck(4, day - 1, 8), HabitCheck(1, day, 1))
        )
        assertEquals(3, index.count(4, day))
        assertEquals(0, index.count(4, day - 5))
        assertFalse(index.isDone(water, day))
        assertTrue(index.isDone(water, day - 1))
        assertEquals(setOf(day - 1), index.doneDays(water))
    }

    @Test
    fun `day progress percent`() {
        val progress = DayProgress(routineDone = 2, routineTotal = 4, tasksDone = 1, tasksTotal = 3, habitsDone = 4, habitsTotal = 10)
        assertEquals(7, progress.done)
        assertEquals(17, progress.total)
        assertEquals(41, progress.percent)
        assertEquals(0, DayProgress(0, 0, 0, 0, 0, 0).percent)
    }

    // ------------------------------------------------- Recordatorios

    @Test
    fun `reminder later today`() {
        val now = today.atTime(9, 0)
        assertEquals(today.atTime(20, 0), HabitReminders.next(Days.ALL, 20 * 60, doneToday = false, now = now))
    }

    @Test
    fun `reminder moves to tomorrow when done or already passed`() {
        val tomorrow = today.plusDays(1).atTime(20, 0)
        assertEquals(tomorrow, HabitReminders.next(Days.ALL, 20 * 60, doneToday = true, now = today.atTime(9, 0)))
        assertEquals(tomorrow, HabitReminders.next(Days.ALL, 20 * 60, doneToday = false, now = today.atTime(21, 0)))
    }

    @Test
    fun `reminder skips days that are not scheduled`() {
        // Viernes por la noche, habito entre semana -> lunes.
        val friday = today.plusDays(4)
        assertEquals(
            today.plusDays(7).atTime(7, 0),
            HabitReminders.next(Days.WEEKDAYS, 7 * 60, doneToday = false, now = friday.atTime(22, 0))
        )
    }

    @Test
    fun `habit with no days is reminded daily`() {
        // Sin dias marcados se trata como diario (nunca queda sin aviso).
        assertEquals(
            today.atTime(20, 0),
            HabitReminders.next(0, 20 * 60, doneToday = false, now = today.atTime(9, 0))
        )
    }
}
