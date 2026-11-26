package com.jarvis.app.logic

import com.jarvis.app.data.BackupCodec
import com.jarvis.app.data.BackupData
import com.jarvis.app.data.Habit
import com.jarvis.app.data.HabitCheck
import com.jarvis.app.data.Note
import com.jarvis.app.data.RoutineCheck
import com.jarvis.app.data.RoutineStep
import com.jarvis.app.data.Seed
import com.jarvis.app.data.Settings
import com.jarvis.app.data.Subtask
import com.jarvis.app.data.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** La copia de seguridad debe volver EXACTAMENTE igual tras exportar e importar. */
class BackupTest {

    private val sample = BackupData(
        settings = Settings(
            setupDone = true, userName = "Julian",
            cityName = "Bogotá", cityRegion = "Bogota D.C.", cityCountry = "Colombia",
            cityLat = 4.60971, cityLon = -74.08175, fahrenheit = false,
            alarmEnabled = true, alarmHour = 5, alarmMinute = 15, alarmDays = Days.WEEKDAYS,
            snoozeMinutes = 5, toneSeconds = 30, voiceEnabled = true, speechRate = 1.15f,
            voicePitch = 0.65f, voiceName = "en-gb-x-rjs-local",
            checkInsEnabled = false, checkInTimes = listOf(600, 1290)
        ),
        tasks = listOf(
            Task(
                id = 1, title = "Call the bank", notes = "Ask about \"fees\"\nand limits",
                dueDate = 20773, dueMinutes = 900, location = "the office", firstStep = "Find the number",
                priority = 3, project = "Finances", repeat = "WEEKLY", repeatDays = 5,
                reminderBefore = 30, done = true, doneAt = 1_794_000_000_000, spawnedId = 2, createdAt = 1_793_000_000_000
            ),
            Task(id = 2, title = "Sin fecha ñ 😀")
        ),
        subtasks = listOf(Subtask(id = 7, taskId = 1, title = "Step one", done = true, position = 0)),
        habits = listOf(
            Habit(
                id = 4, name = "Water", emoji = "💧", behavior = "drink a glass", cue = Habit.CUE_STACK,
                timeMinutes = 480, location = "kitchen", anchor = "I wake up", tinyVersion = "one sip",
                days = Days.ALL, target = 8, unit = "glasses", reminderMinutes = 600, position = 3, archived = false,
                askInCheckIns = false
            ),
            Habit(id = 5, name = "Reading")
        ),
        habitChecks = listOf(HabitCheck(4, 20773, 3), HabitCheck(5, 20772, 1)),
        routineSteps = listOf(RoutineStep(1, "Make your bed", 0), RoutineStep(2, "Do your push-ups", 1)),
        routineChecks = listOf(RoutineCheck(1, 20773)),
        notes = listOf(
            Note(
                id = 9, title = "Idea", body = "Line 1\nLine 2", takeaway = "Keep it small",
                category = "PROJECT", pinned = true, createdAt = 11, updatedAt = 22
            )
        )
    )

    @Test
    fun `export then import returns the same data`() {
        val json = BackupCodec.encode(sample)
        assertEquals(sample, BackupCodec.decode(json))
    }

    @Test
    fun `null values survive the round trip`() {
        val empty = sample.copy(settings = Settings(setupDone = true), tasks = listOf(Task(id = 3, title = "x")))
        val restored = BackupCodec.decode(BackupCodec.encode(empty))
        assertEquals(null, restored.settings.cityLat)
        assertEquals(null, restored.tasks[0].dueDate)
        assertEquals(null, restored.tasks[0].reminderBefore)
        assertEquals(empty, restored)
    }

    @Test
    fun `imported settings always skip the welcome screen`() {
        val json = BackupCodec.encode(sample.copy(settings = Settings(setupDone = false)))
        assertTrue(BackupCodec.decode(json).settings.setupDone)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `random text is rejected`() {
        BackupCodec.decode("hello world")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `other json files are rejected`() {
        BackupCodec.decode("""{"format":"something-else","version":1}""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `backups from a newer version are rejected`() {
        BackupCodec.decode("""{"format":"jarvis-backup","version":99}""")
    }

    @Test
    fun `seed data matches the spreadsheet`() {
        assertEquals(
            listOf(
                "Reading", "Go to Sleep early", "Diary", "Water", "Skincare",
                "Stop Watching Porn", "Social Media Limit", "Clean my Room", "Good Diet", "Night gratitude"
            ),
            Seed.HABITS.map { it.name }
        )
        assertEquals(4, Seed.ROUTINE.size)
    }
}
