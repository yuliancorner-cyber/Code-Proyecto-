package com.jarvis.app.android

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.jarvis.app.alarm.AlarmPhase
import com.jarvis.app.alarm.AlarmUi
import com.jarvis.app.alarm.BriefingScreen
import com.jarvis.app.alarm.RingingScreen
import com.jarvis.app.data.AppGraph
import com.jarvis.app.data.Note
import com.jarvis.app.data.Subtask
import com.jarvis.app.data.Task
import com.jarvis.app.logic.BriefingBuilder
import com.jarvis.app.logic.BriefingInput
import com.jarvis.app.logic.BriefingTask
import com.jarvis.app.logic.Weather
import com.jarvis.app.ui.JarvisRoot
import com.jarvis.app.ui.theme.Black
import com.jarvis.app.ui.theme.JarvisTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Recorre las pantallas de la app con datos de ejemplo, comprobando que se
 * dibujan y que los toques principales funcionan. Con la opcion -Pscreenshots
 * guarda ademas una imagen de cada pantalla en jarvis/build/screenshots.
 *
 * La pantalla simulada es alta a proposito, para ver cada pantalla entera.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h2300dp-xhdpi")
class ScreensTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val today: LocalDate = LocalDate.now()
    private val day = today.toEpochDay()

    private fun shot(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    private fun show(content: @Composable () -> Unit) {
        compose.setContent {
            JarvisTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Black) { content() }
            }
        }
    }

    /** Toca una pestana de la barra inferior (el mismo texto puede salir en otra parte). */
    private fun openTab(label: String) {
        compose.onNode(
            hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        ).performClick()
    }

    /** Espera a que aparezca un texto (los datos llegan de la base de datos en segundo plano). */
    private fun waitForText(text: String, substring: Boolean = false) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Deja la app como si llevara unas semanas en uso. */
    private fun fillSampleData() = runBlocking {
        val repo = AppGraph.repo
        AppGraph.settings.update {
            it.copy(
                setupDone = true, userName = "Julian",
                cityName = "Bogotá", cityRegion = "Bogota D.C.", cityCountry = "Colombia",
                cityLat = 4.60971, cityLon = -74.08175
            )
        }
        // Clima ya descargado (asi la prueba no necesita internet).
        context.getSharedPreferences("weather", Context.MODE_PRIVATE).edit()
            .putString(
                "json",
                """{"current":{"temperature_2m":14.0,"weather_code":3},
                    "daily":{"weather_code":[61],"temperature_2m_max":[21.0],"temperature_2m_min":[9.7],
                    "precipitation_probability_max":[35]}}"""
            )
            .putLong("fetchedAt", System.currentTimeMillis())
            .putString("city", "4.60971,-74.08175")
            .apply()

        repo.saveTask(
            Task(
                title = "Study calculus", dueDate = day, dueMinutes = 8 * 60, location = "the library",
                firstStep = "Open the notebook", priority = 3, project = "University",
                repeat = "WEEKDAYS", reminderBefore = 10
            ),
            listOf(Subtask(taskId = 0, title = "Chapter 3", done = true), Subtask(taskId = 0, title = "Exercises"))
        )
        repo.saveTask(Task(title = "Call the bank", dueDate = day, dueMinutes = 15 * 60, priority = 2, project = "Finances"), emptyList())
        val done = repo.saveTask(Task(title = "Buy groceries", dueDate = day), emptyList())
        repo.setTaskDone(done, true)
        repo.saveTask(Task(title = "Renew my ID", dueDate = day - 2, location = "downtown", priority = 1), emptyList())
        repo.saveTask(Task(title = "Plan the weekend trip", dueDate = day + 1), emptyList())
        repo.saveTask(Task(title = "Read Atomic Habits"), emptyList())

        // Tres semanas de habitos con algunos huecos.
        val habits = repo.habits.all()
        for (offset in 1..20L) {
            habits.forEachIndexed { i, habit ->
                if ((offset + i) % 4 != 0L) repo.setHabitCount(habit.id, day - offset, habit.target)
            }
        }
        repo.toggleHabit(habits[0], day)
        repo.toggleHabit(habits[4], day)
        repeat(3) { repo.incrementHabit(habits[3], day) }
        repo.saveHabit(
            habits[0].copy(behavior = "read 10 pages", timeMinutes = 21 * 60, location = "my bedroom", tinyVersion = "read one page")
        )
        repo.setRoutineDone(repo.routine.steps()[0].id, day, true)
        repo.setRoutineDone(repo.routine.steps()[1].id, day, true)

        repo.saveNote(Note(title = "Thesis outline", body = "Intro, method, results.", takeaway = "Start with the method chapter.", category = "PROJECT", pinned = true))
        repo.saveNote(Note(body = "Idea: record a 1-minute voice memo after every lecture."))
        repo.saveNote(Note(title = "Workout plan", body = "Push, pull, legs.", category = "AREA"))
    }

    @Test
    fun `welcome screen asks for the basics`() {
        show { JarvisRoot() }
        compose.onNodeWithText("Good to meet you.").assertIsDisplayed()
        compose.onNodeWithText("Julian").assertIsDisplayed()
        compose.onNodeWithText("5:15 AM").assertIsDisplayed()
        shot("01-welcome")
    }

    @Test
    fun `the main tabs show the sample data`() {
        fillSampleData()
        show { JarvisRoot() }

        // --- Today ---
        waitForText("Study calculus")
        compose.onNodeWithText("Julian").assertIsDisplayed()
        waitForText("Bogotá")
        compose.onNodeWithText("14°C").assertIsDisplayed()
        compose.onNodeWithText("Make your bed").assertIsDisplayed()
        compose.onNodeWithText("3/8").assertIsDisplayed() // contador de agua
        shot("02-today")

        // --- Tasks ---
        openTab("Tasks")
        waitForText("OVERDUE")
        compose.onNodeWithText("Renew my ID").assertIsDisplayed()
        compose.onNodeWithText("Read Atomic Habits").assertIsDisplayed()
        shot("03-tasks")

        // --- Habits ---
        openTab("Habits")
        waitForText("HABIT")
        compose.onNodeWithText("Never miss twice.").assertIsDisplayed()
        shot("04-habits")

        // --- Brain ---
        openTab("Brain")
        waitForText("Thesis outline")
        shot("05-brain")

        // --- Settings ---
        openTab("Settings")
        waitForText("MORNING ALARM")
        shot("06-settings")
    }

    @Test
    fun `checking a task and a habit on Today updates the progress`() {
        fillSampleData()
        show { JarvisRoot() }
        waitForText("Call the bank")

        compose.onNodeWithContentDescription("Call the bank, not done").performClick()
        compose.waitUntil(10_000) {
            runBlocking { AppGraph.repo.tasks.all().first { it.title == "Call the bank" }.done }
        }

        compose.onNodeWithContentDescription("Diary, not done").performClick()
        compose.waitUntil(10_000) {
            runBlocking {
                val diary = AppGraph.repo.habits.all().first { it.name == "Diary" }
                AppGraph.repo.habits.check(diary.id, day) != null
            }
        }
    }

    @Test
    fun `a new task can be written and saved from the editor`() {
        fillSampleData()
        show { JarvisRoot() }
        waitForText("Call the bank")

        compose.onNodeWithText("Add").performClick()
        compose.onNodeWithText("New task").assertIsDisplayed()
        compose.onNode(hasText("What will you do?")).performTextInput("Review my notes")
        compose.onNodeWithText("My desk, the office, the gym…").performTextInput("my desk")
        // La frase de la intencion se arma en vivo.
        compose.onNodeWithText("I will review my notes in my desk.").assertIsDisplayed()
        shot("07-task-editor")

        compose.onNodeWithText("Save").performClick()
        waitForText("Review my notes")
        val saved = runBlocking { AppGraph.repo.tasks.all().first { it.title == "Review my notes" } }
        assertEquals("my desk", saved.location)
        assertEquals(day, saved.dueDate) // creada desde Today: para hoy
    }

    @Test
    fun `habit editor shows the plan`() {
        fillSampleData()
        show { JarvisRoot() }
        waitForText("Call the bank")
        openTab("Habits")
        waitForText("HABIT")
        compose.onAllNodesWithText("📖 Reading")[0].performClick()
        waitForText("Edit habit")
        compose.onNodeWithText("I will read 10 pages at 9:00 PM in my bedroom.").assertIsDisplayed()
        shot("08-habit-editor")
    }

    @Test
    fun `alarm screen while ringing`() {
        fillSampleData()
        var dismissed = false
        show { RingingScreen(speaking = true, onSnooze = {}, onDismiss = { dismissed = true }) }
        compose.onNodeWithText("Snooze 10 min").assertIsDisplayed()
        shot("09-alarm-ringing")
        compose.onNodeWithText("I'm up").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun `briefing screen after the alarm`() {
        fillSampleData()
        val briefing = BriefingBuilder.build(
            BriefingInput(
                name = "Julian",
                now = LocalDateTime.of(2026, 11, 16, 5, 15),
                city = "Bogotá",
                weather = Weather(14.0, 3, 21.0, 9.7, 61, 35, 0),
                fahrenheit = false,
                routine = listOf("Make your bed", "Do your push-ups", "Organize your space", "Drink a big glass of water"),
                tasks = listOf(
                    BriefingTask("Study calculus", 8 * 60, "the library"),
                    BriefingTask("Call the bank", 15 * 60, "")
                ),
                overdue = 1,
                habits = 10
            )
        )
        show { BriefingScreen(AlarmUi(AlarmPhase.BRIEFING, briefing, speaking = true), onStopVoice = {}, onDone = {}) }
        waitForText("Organize your space")
        compose.onNodeWithText("Good morning, Julian.").assertIsDisplayed()
        compose.onNodeWithText("Start my day").assertIsDisplayed()
        shot("10-alarm-briefing")
    }
}
