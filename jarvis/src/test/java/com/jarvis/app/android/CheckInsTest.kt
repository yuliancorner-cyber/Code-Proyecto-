package com.jarvis.app.android

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jarvis.app.data.AppGraph
import com.jarvis.app.data.Habit
import com.jarvis.app.logic.CheckInLogic
import com.jarvis.app.reminders.CheckIns
import com.jarvis.app.reminders.Notifications
import com.jarvis.app.reminders.ReminderScheduler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Los check-ins en un Android simulado: la notificacion pregunta por cada
 * habito pendiente y los botones lo marcan en la cuadricula sin abrir la app.
 * Los botones se pulsan de verdad: se toma el "sobre" (PendingIntent) de cada
 * boton y se entrega a JARVIS tal como lo haria Android.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CheckInsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notifications get() = shadowOf(context.getSystemService(NotificationManager::class.java))
    private val repo get() = AppGraph.repo
    private val day get() = LocalDate.now().toEpochDay()

    @Before
    fun setUp() {
        AppGraph.settings.update { it.copy(setupDone = true) }
    }

    private fun current(): Notification? = notifications.getNotification(Notifications.ID_CHECKIN)

    private fun Notification.title() = extras.getCharSequence(Notification.EXTRA_TITLE).toString()
    private fun Notification.text() = extras.getCharSequence(Notification.EXTRA_TEXT).toString()
    private fun Notification.subText() = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
    private fun Notification.buttons() = actions.orEmpty().map { it.title.toString() }

    /** Pulsa un boton de la notificacion actual. */
    private fun press(label: String) = runBlocking {
        val action = current()!!.actions.first { it.title.toString() == label }
        val intent: Intent = shadowOf(action.actionIntent).savedIntent
        CheckIns.handle(context, intent)
    }

    private fun habit(name: String): Habit = runBlocking { repo.habits.all().first { it.name == name } }
    private fun count(name: String): Int = runBlocking { repo.habits.check(habit(name).id, day)?.count ?: 0 }
    private fun markDone(vararg names: String) = runBlocking {
        for (name in names) repo.setHabitCount(habit(name).id, day, habit(name).target)
    }

    private fun checkInNow(): Boolean = runBlocking { CheckIns.startRound(context, minutes = null) }

    @Test
    fun `a check-in asks about the first unchecked habit`() {
        markDone("Reading")
        assertTrue(checkInNow())

        val n = current()!!
        assertEquals("😴 Go to Sleep early", n.title())
        assertEquals("Did you keep this habit today?", n.text())
        assertEquals("Check-in · 9 left", n.subText())
        assertEquals(listOf("Yes", "Not today", "Later"), n.buttons())
        assertEquals(Notifications.CHANNEL_CHECKINS, n.channelId)
        // Con el celular bloqueado no se ve el nombre del habito.
        assertEquals("Habit check-in", n.publicVersion.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
    }

    @Test
    fun `Yes fills the habit tracker and moves to the next question`() {
        checkInNow()
        assertEquals("📖 Reading", current()!!.title())

        press("Yes")
        assertEquals(1, count("Reading"))
        assertEquals("😴 Go to Sleep early", current()!!.title())
        assertEquals("Check-in · 9 left", current()!!.subText())
    }

    @Test
    fun `the water counter can be filled from the notification`() {
        markDone("Reading", "Go to Sleep early", "Diary")
        checkInNow()
        assertEquals("💧 Water", current()!!.title())
        assertEquals("0 of 8 glasses so far today.", current()!!.text())
        assertEquals(listOf("+1", "All 8", "Later"), current()!!.buttons())

        press("+1")
        press("+1")
        assertEquals(2, count("Water"))
        assertEquals("💧 Water", current()!!.title()) // sigue preguntando por el agua
        assertEquals("2 of 8 glasses so far today.", current()!!.text())

        press("All 8")
        assertEquals(8, count("Water"))
        assertEquals("🧼 Skincare", current()!!.title())
    }

    @Test
    fun `Not today is not asked again today, Later is asked in the next round`() {
        checkInNow()
        press("Not today") // Reading
        press("Later")     // Go to Sleep early
        assertEquals("📓 Diary", current()!!.title())
        assertEquals(0, count("Reading"))

        // Siguiente ronda (la de las 5:00 PM).
        runBlocking { CheckIns.startRound(context, minutes = 17 * 60) }
        assertEquals("😴 Go to Sleep early", current()!!.title())
    }

    @Test
    fun `after the last answer a short summary replaces the question`() {
        val names = runBlocking { repo.habits.all().map { it.name } }
        markDone(*names.dropLast(1).toTypedArray())
        checkInNow()
        assertEquals("🙏 Night gratitude", current()!!.title())
        assertEquals("Check-in · last one", current()!!.subText())

        press("Yes")
        val summary = current()!!
        assertEquals("All caught up", summary.title())
        assertEquals("10 of 10 habits done today. Great work.", summary.text())
        assertTrue("the summary disappears by itself", summary.timeoutAfter > 0)
        assertTrue(summary.buttons().isEmpty())
    }

    @Test
    fun `answering Later to the rest says when it will ask again`() {
        val names = runBlocking { repo.habits.all().map { it.name } }
        markDone(*names.dropLast(1).toTypedArray())
        checkInNow()
        press("Later")
        assertEquals("Got it", current()!!.title())
        assertTrue(current()!!.text().startsWith("9 of 10 habits done today."))
    }

    @Test
    fun `nothing pending means no notification`() {
        val names = runBlocking { repo.habits.all().map { it.name } }
        markDone(*names.toTypedArray())
        assertFalse(checkInNow())
        assertNull(current())
    }

    @Test
    fun `checking a habit in the app updates the open question`() {
        checkInNow()
        assertEquals("📖 Reading", current()!!.title())

        runBlocking { repo.toggleHabit(habit("Reading"), day) }
        assertEquals("😴 Go to Sleep early", current()!!.title())

        // Si en la app se marca todo, la pregunta desaparece (sin resumen).
        val names = runBlocking { repo.habits.all().map { it.name } }
        markDone(*names.toTypedArray())
        assertNull(current())
    }

    @Test
    fun `a habit switched off in its editor is skipped`() {
        runBlocking { repo.saveHabit(habit("Reading").copy(askInCheckIns = false)) }
        checkInNow()
        assertEquals("😴 Go to Sleep early", current()!!.title())
    }

    @Test
    fun `scheduled rounds respect the switch in Settings`() {
        AppGraph.settings.update { it.copy(checkInsEnabled = false) }
        assertFalse(runBlocking { CheckIns.startRound(context, minutes = 12 * 60) })
        assertNull(current())
        // "Check in now" funciona igual, aunque esten apagados.
        assertTrue(checkInNow())
    }

    @Test
    fun `check-in times are scheduled as alarms and removed when switched off`() {
        val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
        AppGraph.settings.update { it.copy(checkInTimes = listOf(21 * 60, 12 * 60)) }
        runBlocking { ReminderScheduler.sync(context) }

        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now()
        val expected = listOf(12 * 60, 21 * 60)
            .map { CheckInLogic.nextOccurrence(it, now).atZone(zone).toInstant().toEpochMilli() }
            .sorted()
        assertEquals(expected, alarms.scheduledAlarms.map { it.triggerAtMs }.sorted())

        AppGraph.settings.update { it.copy(checkInsEnabled = false) }
        runBlocking { ReminderScheduler.sync(context) }
        assertTrue(alarms.scheduledAlarms.isEmpty())
    }

    @Test
    fun `a scheduled alarm starts the round`() {
        val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
        AppGraph.settings.update { it.copy(checkInTimes = listOf(12 * 60)) }
        runBlocking { ReminderScheduler.sync(context) }
        val alarm = alarms.scheduledAlarms.single()

        // Android entregaria este "sobre" a ReminderReceiver a las 12:00.
        alarm.operation!!.send()
        val deadline = System.currentTimeMillis() + 10_000
        while (current() == null && System.currentTimeMillis() < deadline) {
            shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        assertNotNull("the 12:00 round must show a question", current())
        assertEquals("📖 Reading", current()!!.title())
    }
}
