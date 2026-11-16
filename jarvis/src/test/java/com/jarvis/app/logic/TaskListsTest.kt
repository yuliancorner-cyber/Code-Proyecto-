package com.jarvis.app.logic

import com.jarvis.app.data.Note
import com.jarvis.app.data.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Agrupar, ordenar y buscar tareas y notas; hora de los recordatorios. */
class TaskListsTest {

    private val today = LocalDate.of(2026, 11, 16)
    private val day = today.toEpochDay()
    private val zone = ZoneId.of("America/Bogota")

    private fun task(id: Long, title: String = "Task $id", due: Long? = null, minutes: Int? = null, priority: Int = 0) =
        Task(id = id, title = title, dueDate = due, dueMinutes = minutes, priority = priority)

    @Test
    fun `sections by due date`() {
        assertEquals(TaskSection.OVERDUE, TaskLists.sectionOf(task(1, due = day - 1), today))
        assertEquals(TaskSection.TODAY, TaskLists.sectionOf(task(1, due = day), today))
        assertEquals(TaskSection.TOMORROW, TaskLists.sectionOf(task(1, due = day + 1), today))
        assertEquals(TaskSection.WEEK, TaskLists.sectionOf(task(1, due = day + 7), today))
        assertEquals(TaskSection.LATER, TaskLists.sectionOf(task(1, due = day + 8), today))
        assertEquals(TaskSection.SOMEDAY, TaskLists.sectionOf(task(1), today))
        assertEquals(TaskSection.DONE, TaskLists.sectionOf(task(1, due = day - 1).copy(done = true), today))
    }

    @Test
    fun `grouped lists keep section order and hide empty ones`() {
        val tasks = listOf(task(1), task(2, due = day), task(3, due = day - 2), task(4, due = day).copy(done = true))
        val groups = TaskLists.grouped(tasks, today)
        assertEquals(listOf(TaskSection.OVERDUE, TaskSection.TODAY, TaskSection.SOMEDAY), groups.map { it.first })
        val withDone = TaskLists.grouped(tasks, today, includeDone = true)
        assertEquals(TaskSection.DONE, withDone.last().first)
    }

    @Test
    fun `tasks sort by time then by priority`() {
        val tasks = listOf(
            task(1, due = day),                        // sin hora, sin prioridad
            task(2, due = day, priority = 3),          // sin hora, prioridad alta
            task(3, due = day, minutes = 15 * 60),     // 3:00 PM
            task(4, due = day, minutes = 9 * 60)       // 9:00 AM
        )
        assertEquals(listOf(4L, 3L, 2L, 1L), tasks.sortedWith(TaskLists.ORDER).map { it.id })
    }

    @Test
    fun `pending today includes overdue but not future or done`() {
        val tasks = listOf(
            task(1, due = day), task(2, due = day - 3), task(3, due = day + 1),
            task(4), task(5, due = day).copy(done = true)
        )
        assertEquals(listOf(2L, 1L), TaskLists.pendingToday(tasks, today).map { it.id })
    }

    @Test
    fun `done today uses the local date of completion`() {
        val noonToday = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val lateYesterday = today.minusDays(1).atTime(23, 30).atZone(zone).toInstant().toEpochMilli()
        val tasks = listOf(
            task(1).copy(done = true, doneAt = noonToday),
            task(2).copy(done = true, doneAt = lateYesterday),
            task(3)
        )
        assertEquals(listOf(1L), TaskLists.doneToday(tasks, today, zone).map { it.id })
    }

    @Test
    fun `reminder time is before the due time`() {
        val t = task(1, due = day, minutes = 15 * 60).copy(reminderBefore = 30)
        assertEquals(today.atTime(14, 30), TaskLists.reminderAt(t))
        assertEquals(today.atTime(15, 0), TaskLists.reminderAt(t.copy(reminderBefore = 0)))
        assertEquals(today.minusDays(1).atTime(15, 0), TaskLists.reminderAt(t.copy(reminderBefore = 1440)))
    }

    @Test
    fun `reminder without a time counts from 9 AM`() {
        val t = task(1, due = day).copy(reminderBefore = 0)
        assertEquals(today.atTime(9, 0), TaskLists.reminderAt(t))
    }

    @Test
    fun `no reminder when off, undated or done`() {
        assertNull(TaskLists.reminderAt(task(1, due = day, minutes = 600)))
        assertNull(TaskLists.reminderAt(task(1).copy(reminderBefore = 10)))
        assertNull(TaskLists.reminderAt(task(1, due = day).copy(reminderBefore = 10, done = true)))
    }

    @Test
    fun `search looks in several fields`() {
        val t = Task(title = "Call the bank", location = "Downtown office", project = "Finances")
        assertTrue(TaskLists.matches(t, "bank"))
        assertTrue(TaskLists.matches(t, "DOWNTOWN"))
        assertTrue(TaskLists.matches(t, "finan"))
        assertTrue(TaskLists.matches(t, "  "))
        assertFalse(TaskLists.matches(t, "gym"))
    }

    @Test
    fun `priority and reminder labels`() {
        assertEquals(Priority.HIGH, Priority.of(3))
        assertEquals(Priority.NONE, Priority.of(42))
        assertEquals("No reminder", ReminderOptions.label(null))
        assertEquals("At time of task", ReminderOptions.label(0))
        assertEquals("1 day before", ReminderOptions.label(1440))
        assertEquals("45 minutes before", ReminderOptions.label(45))
    }

    // -------------------------------------------------------------- Notas

    @Test
    fun `notes filter by category and text, pinned first`() {
        val notes = listOf(
            Note(id = 1, title = "Thesis outline", category = "PROJECT", updatedAt = 10),
            Note(id = 2, body = "Idea about sleep", category = "INBOX", updatedAt = 30),
            Note(id = 3, title = "Workout plan", category = "AREA", pinned = true, updatedAt = 5),
            Note(id = 4, title = "Old stuff", takeaway = "sleep matters", category = "ARCHIVE", updatedAt = 20)
        )
        assertEquals(listOf(3L, 2L, 4L, 1L), NoteLists.filter(notes, null, "").map { it.id })
        assertEquals(listOf(1L), NoteLists.filter(notes, NoteCategory.PROJECT, "").map { it.id })
        assertEquals(listOf(2L, 4L), NoteLists.filter(notes, null, "SLEEP").map { it.id })
        assertTrue(NoteLists.filter(notes, NoteCategory.RESOURCE, "").isEmpty())
    }

    @Test
    fun `note title falls back to the first line`() {
        assertEquals("Thesis", NoteLists.displayTitle(Note(title = "Thesis", body = "text")))
        assertEquals("First line", NoteLists.displayTitle(Note(body = "\n  First line\nSecond")))
        assertEquals("Untitled", NoteLists.displayTitle(Note()))
        assertEquals(NoteCategory.INBOX, NoteCategory.of("???"))
    }
}
