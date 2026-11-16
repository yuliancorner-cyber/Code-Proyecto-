package com.jarvis.app.android

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jarvis.app.alarm.AlarmScheduler
import com.jarvis.app.data.AppGraph
import com.jarvis.app.data.BackupCodec
import com.jarvis.app.data.Habit
import com.jarvis.app.data.Note
import com.jarvis.app.data.Subtask
import com.jarvis.app.data.Task
import com.jarvis.app.logic.Days
import com.jarvis.app.reminders.ReminderScheduler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Pruebas con un Android simulado (Robolectric): la base de datos real, las
 * acciones del repositorio y lo que queda programado en AlarmManager.
 * Cada prueba empieza con la app recien instalada.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class DataFlowTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repo get() = AppGraph.repo
    private val today: LocalDate get() = LocalDate.now()

    private fun scheduledAlarms() =
        shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms

    @Test
    fun `a new install starts with the spreadsheet habits and the morning routine`() = runBlocking {
        val habits = repo.habits.all()
        assertEquals(10, habits.size)
        assertEquals("Reading", habits.first().name)
        assertEquals("Night gratitude", habits.last().name)
        assertEquals(8, habits.first { it.name == "Water" }.target)
        assertTrue(habits.all { it.days == Days.ALL && !it.archived })
        assertEquals(
            listOf("Make your bed", "Do your push-ups", "Organize your space", "Drink a big glass of water"),
            repo.routine.steps().map { it.title }
        )
    }

    @Test
    fun `saving a task stores its steps and editing replaces them`() = runBlocking {
        val id = repo.saveTask(
            Task(title = "Write report", location = "my desk"),
            listOf(Subtask(taskId = 0, title = "Outline"), Subtask(taskId = 0, title = "  "), Subtask(taskId = 0, title = "Draft"))
        )
        val stored = repo.tasks.byId(id)!!
        assertEquals("Write report", stored.title)
        assertTrue(stored.createdAt > 0)
        assertEquals(listOf("Outline", "Draft"), repo.tasks.subtasksOf(id).map { it.title })

        repo.saveTask(stored.copy(title = "Write the report"), listOf(Subtask(taskId = id, title = "Only step", done = true)))
        assertEquals("Write the report", repo.tasks.byId(id)!!.title)
        assertEquals(listOf("Only step"), repo.tasks.subtasksOf(id).map { it.title })

        repo.deleteTask(id)
        assertNull(repo.tasks.byId(id))
        assertTrue(repo.tasks.subtasksOf(id).isEmpty()) // borrado en cascada
    }

    @Test
    fun `completing a repeating task creates the next one and undo removes it`() = runBlocking {
        val id = repo.saveTask(
            Task(title = "Take vitamins", dueDate = today.toEpochDay(), dueMinutes = 8 * 60, repeat = "DAILY"),
            listOf(Subtask(taskId = 0, title = "Step", done = true))
        )
        repo.setTaskDone(id, true)

        val done = repo.tasks.byId(id)!!
        assertTrue(done.done)
        assertNotNull(done.doneAt)
        val next = repo.tasks.byId(done.spawnedId!!)!!
        assertFalse(next.done)
        assertEquals(today.plusDays(1).toEpochDay(), next.dueDate)
        assertEquals(8 * 60, next.dueMinutes)
        assertEquals(listOf(false), repo.tasks.subtasksOf(next.id).map { it.done }) // pasos reiniciados

        repo.setTaskDone(id, false)
        assertFalse(repo.tasks.byId(id)!!.done)
        assertNull(repo.tasks.byId(next.id))
        assertEquals(1, repo.tasks.all().size)
    }

    @Test
    fun `a one-off task does not spawn anything`() = runBlocking {
        val id = repo.quickTask("Buy milk", forToday = true)
        repo.setTaskDone(id, true)
        assertEquals(1, repo.tasks.all().size)
        assertNull(repo.tasks.byId(id)!!.spawnedId)
    }

    @Test
    fun `habit checkbox toggles and the water counter stops at its target`() = runBlocking {
        val day = today.toEpochDay()
        val reading = repo.habits.all().first { it.name == "Reading" }
        val water = repo.habits.all().first { it.name == "Water" }

        repo.toggleHabit(reading, day)
        assertEquals(1, repo.habits.check(reading.id, day)?.count)
        repo.toggleHabit(reading, day)
        assertNull(repo.habits.check(reading.id, day))

        repeat(3) { repo.incrementHabit(water, day) }
        assertEquals(3, repo.habits.check(water.id, day)?.count)
        repeat(20) { repo.incrementHabit(water, day) }
        assertEquals(8, repo.habits.check(water.id, day)?.count)

        // La casilla de la cuadricula: llena la meta de golpe o la vacia.
        repo.toggleHabit(water, day)
        assertNull(repo.habits.check(water.id, day))
        repo.toggleHabit(water, day)
        assertEquals(8, repo.habits.check(water.id, day)?.count)
    }

    @Test
    fun `habits can be added, reordered and deleted with their history`() = runBlocking {
        val id = repo.saveHabit(Habit(name = "Stretch", emoji = "🤸"))
        assertEquals(11, repo.habits.all().size)
        assertEquals("Stretch", repo.habits.all().last().name)

        repo.moveHabit(id, -1)
        assertEquals("Stretch", repo.habits.all()[9].name)
        // Guardar cambios del habito no deshace el nuevo orden.
        repo.saveHabit(repo.habits.byId(id)!!.copy(name = "Stretching", position = 99))
        assertEquals("Stretching", repo.habits.all()[9].name)

        repo.setHabitCount(id, today.toEpochDay(), 1)
        repo.deleteHabit(id)
        assertEquals(10, repo.habits.all().size)
        assertTrue(repo.habits.allChecks().none { it.habitId == id })
    }

    @Test
    fun `routine steps can be edited`() = runBlocking {
        repo.addRoutineStep("  Stretch for 5 minutes ")
        var steps = repo.routine.steps()
        assertEquals("Stretch for 5 minutes", steps.last().title)

        repo.moveRoutineStep(steps.last().id, -4)
        steps = repo.routine.steps()
        assertEquals("Stretch for 5 minutes", steps.first().title)

        repo.renameRoutineStep(steps[1], "Make the bed")
        repo.deleteRoutineStep(steps.first().id)
        assertEquals(
            listOf("Make the bed", "Do your push-ups", "Organize your space", "Drink a big glass of water"),
            repo.routine.steps().map { it.title }
        )

        val step = repo.routine.steps().first()
        repo.setRoutineDone(step.id, today.toEpochDay(), true)
        assertEquals(1, repo.routine.allChecks().size)
        repo.setRoutineDone(step.id, today.toEpochDay(), false)
        assertTrue(repo.routine.allChecks().isEmpty())
    }

    @Test
    fun `notes are captured to the inbox and keep their dates`() = runBlocking {
        val id = repo.quickNote("  An idea worth keeping ")
        val note = repo.notes.byId(id)!!
        assertEquals("An idea worth keeping", note.body)
        assertEquals("INBOX", note.category)
        assertTrue(note.createdAt > 0)

        repo.saveNote(note.copy(title = "Idea", category = "PROJECT"))
        val edited = repo.notes.byId(id)!!
        assertEquals("PROJECT", edited.category)
        assertEquals(note.createdAt, edited.createdAt)
        assertTrue(edited.updatedAt >= note.updatedAt)
    }

    // ------------------------------------------------- Alarma y recordatorios

    @Test
    fun `the alarm is not scheduled until the welcome screen is finished`() {
        assertNull(AlarmScheduler.schedule(context))
        assertTrue(scheduledAlarms().isEmpty())

        AppGraph.settings.update { it.copy(setupDone = true) }
        val next = AlarmScheduler.schedule(context)!!
        assertEquals(5, next.hour)
        assertEquals(15, next.minute)
        assertTrue(next.isAfter(LocalDateTime.now()))

        val alarms = scheduledAlarms()
        assertEquals(1, alarms.size)
        assertEquals(next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), alarms[0].triggerAtMs)
        assertNotNull("must be a real alarm clock (shows the status bar icon)", alarms[0].alarmClockInfo)
    }

    @Test
    fun `changing or disabling the alarm reschedules it`() {
        AppGraph.settings.update { it.copy(setupDone = true, alarmHour = 6, alarmMinute = 30) }
        val next = AlarmScheduler.schedule(context)!!
        assertEquals(6, next.hour)
        assertEquals(30, next.minute)
        assertEquals(1, scheduledAlarms().size)

        AppGraph.settings.update { it.copy(alarmEnabled = false) }
        assertNull(AlarmScheduler.schedule(context))
        assertTrue(scheduledAlarms().isEmpty())
    }

    @Test
    fun `task reminders are scheduled and cancelled`() = runBlocking {
        val tomorrow = today.plusDays(1)
        val id = repo.saveTask(
            Task(title = "Dentist", dueDate = tomorrow.toEpochDay(), dueMinutes = 15 * 60, reminderBefore = 30),
            emptyList()
        )
        val expected = tomorrow.atTime(14, 30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(listOf(expected), scheduledAlarms().map { it.triggerAtMs })

        // Una tarea sin recordatorio, o ya pasada, no programa nada.
        repo.saveTask(Task(title = "No reminder", dueDate = tomorrow.toEpochDay()), emptyList())
        repo.saveTask(Task(title = "Past", dueDate = today.minusDays(2).toEpochDay(), reminderBefore = 0), emptyList())
        assertEquals(1, scheduledAlarms().size)

        repo.setTaskDone(id, true)
        assertTrue(scheduledAlarms().isEmpty())
    }

    @Test
    fun `habit reminders follow the habit`() = runBlocking {
        val habit = repo.habits.all().first()
        repo.saveHabit(habit.copy(reminderMinutes = 23 * 60 + 59))
        assertEquals(1, scheduledAlarms().size)
        val first = scheduledAlarms()[0].triggerAtMs

        // Al marcarlo hoy, el siguiente aviso pasa a manana (o sigue en manana).
        repo.toggleHabit(repo.habits.byId(habit.id)!!, today.toEpochDay())
        assertEquals(1, scheduledAlarms().size)
        assertTrue(scheduledAlarms()[0].triggerAtMs >= first)

        repo.saveHabit(repo.habits.byId(habit.id)!!.copy(reminderMinutes = null))
        ReminderScheduler.sync(context)
        assertTrue(scheduledAlarms().isEmpty())
    }

    // ------------------------------------------------------ Copia de seguridad

    @Test
    fun `a backup restores everything on a clean install`() = runBlocking {
        val day = today.toEpochDay()
        val taskId = repo.saveTask(
            Task(title = "Call the bank", dueDate = day, dueMinutes = 900, location = "office", priority = 3),
            listOf(Subtask(taskId = 0, title = "Find the number"))
        )
        repo.toggleHabit(repo.habits.all()[2], day)
        repo.setRoutineDone(repo.routine.steps()[0].id, day, true)
        repo.saveNote(Note(title = "Idea", body = "Body", category = "AREA", pinned = true))
        AppGraph.settings.update { it.copy(setupDone = true, userName = "Julian", alarmHour = 6) }

        val exported = repo.exportData(AppGraph.settings.value)
        val json = BackupCodec.encode(exported)

        // Se destroza todo y se restaura desde el archivo.
        repo.deleteTask(taskId)
        repo.habits.all().forEach { repo.deleteHabit(it.id) }
        repo.importData(BackupCodec.decode(json))

        assertEquals(exported, repo.exportData(AppGraph.settings.value))
        assertEquals(10, repo.habits.all().size)
        assertEquals("Find the number", repo.tasks.subtasksOf(taskId).single().title)
    }
}
