package com.jarvis.app.logic

import com.jarvis.app.data.Habit
import com.jarvis.app.data.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Frases de intencion de implementacion (James Clear). */
class IntentionTest {

    @Test
    fun `full sentence with time and place`() {
        assertEquals(
            "I will call the bank at 3:00 PM in the office.",
            Intention.sentence("Call the bank", "3:00 PM", "the office")
        )
    }

    @Test
    fun `sentence without time or place`() {
        assertEquals("I will call the bank.", Intention.sentence("Call the bank.", null, ""))
        assertEquals("I will call the bank at 3:00 PM.", Intention.sentence("Call the bank", "3:00 PM", " "))
        assertEquals("I will call the bank in the office.", Intention.sentence("Call the bank", null, "The office"))
    }

    @Test
    fun `place does not repeat the preposition`() {
        assertEquals("I will study in my room.", Intention.sentence("Study", null, "In my room"))
        assertEquals("I will train in the gym.", Intention.sentence("Train", null, "at the gym"))
    }

    @Test
    fun `empty behavior gives no sentence`() {
        assertEquals("", Intention.sentence("  ", "3:00 PM", "the office"))
    }

    @Test
    fun `task sentence uses its time and location`() {
        val task = Task(title = "Review notes", dueMinutes = 18 * 60 + 30, location = "the library")
        assertEquals("I will review notes at 6:30 PM in the library.", Intention.forTask(task))
        assertEquals("I will review notes at 18:30 in the library.", Intention.forTask(task, is24Hour = true))
    }

    @Test
    fun `habit stacking sentence`() {
        assertEquals(
            "After I brush my teeth, I will read one page.",
            Intention.stackSentence("I brush my teeth", "Read one page")
        )
        // Un "After" repetido por el usuario no se duplica.
        assertEquals(
            "After I wake up, I will make my bed.",
            Intention.stackSentence("After I wake up.", "make my bed")
        )
    }

    @Test
    fun `habit with time and place`() {
        val habit = Habit(name = "Reading", behavior = "read 10 pages", timeMinutes = 21 * 60, location = "my bedroom")
        assertEquals("I will read 10 pages at 9:00 PM in my bedroom.", Intention.forHabit(habit))
        assertTrue(Intention.hasPlan(habit))
    }

    @Test
    fun `habit falls back to its name when no behavior is written`() {
        val habit = Habit(name = "Skincare", timeMinutes = 22 * 60)
        assertEquals("I will skincare at 10:00 PM.", Intention.forHabit(habit))
    }

    @Test
    fun `stacked habit`() {
        val habit = Habit(name = "Diary", behavior = "write three lines", cue = Habit.CUE_STACK, anchor = "I get into bed")
        assertEquals("After I get into bed, I will write three lines.", Intention.forHabit(habit))
    }

    @Test
    fun `habit without a plan has no sentence`() {
        assertEquals("", Intention.forHabit(Habit(name = "Reading")))
        assertFalse(Intention.hasPlan(Habit(name = "Reading")))
        assertFalse(Intention.hasPlan(Habit(name = "Diary", cue = Habit.CUE_STACK)))
    }
}
