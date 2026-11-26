package com.jarvis.app.android

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jarvis.app.alarm.AlarmActivity
import com.jarvis.app.alarm.AlarmPhase
import com.jarvis.app.alarm.AlarmService
import com.jarvis.app.alarm.AlarmState
import com.jarvis.app.data.AppGraph
import com.jarvis.app.logic.VoicePicker
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowTextToSpeech
import java.time.Duration
import java.util.Locale

/**
 * La alarma de principio a fin en un Android simulado: suena, lee el resumen
 * con voz grave de hombre, y responde a "I'm up" y a "Snooze".
 * (El tono y la vibracion reales no se pueden comprobar sin un celular.)
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AlarmServiceTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val context: Context = app
    private val mainLooper get() = shadowOf(Looper.getMainLooper())

    @Before
    fun setUp() {
        AlarmState.reset()
        AppGraph.settings.update {
            it.copy(setupDone = true, userName = "Julian", toneSeconds = 5, voiceEnabled = true)
        }
        // Voces que "tiene instaladas" el celular simulado.
        ShadowTextToSpeech.addVoice(Voice("en-us-x-sfg-local", Locale.US, 400, 200, false, emptySet()))
        ShadowTextToSpeech.addVoice(Voice("en-gb-x-rjs-local", Locale.UK, 400, 200, false, emptySet()))
        shadowOf(app).clearStartedServices()
    }

    @After
    fun tearDown() {
        AlarmState.reset()
    }

    /** Deja correr el hilo principal y los trabajos en segundo plano hasta que se cumpla algo. */
    private fun pump(what: String, until: () -> Boolean) {
        // Margen amplio: la primera prueba paga el arranque del Android simulado.
        val deadline = System.currentTimeMillis() + 20_000
        while (System.currentTimeMillis() < deadline) {
            mainLooper.idle()
            if (until()) return
            Thread.sleep(15)
        }
        fail("Timed out waiting for: $what")
    }

    /** Hace sonar la alarma como lo haria AlarmReceiver y devuelve el servicio en marcha. */
    private fun fire(): ServiceController<AlarmService> {
        AlarmService.fire(context)
        val intent = shadowOf(app).nextStartedService
        assertNotNull("fire() must start the alarm service", intent)
        return Robolectric.buildService(AlarmService::class.java, intent).create().startCommand(0, 1)
    }

    /** Entrega al servicio la orden que acaba de enviar la pantalla (dismiss/snooze/stop). */
    private fun deliver(controller: ServiceController<AlarmService>, startId: Int) {
        val intent = shadowOf(app).nextStartedService
        assertNotNull("the action must be sent to the service", intent)
        controller.get().onStartCommand(intent, 0, startId)
    }

    /** El motor de voz simulado no avisa solo de que esta listo: lo hacemos aqui. */
    private fun readyVoice(): ShadowTextToSpeech {
        pump("the voice engine to be created") { ShadowTextToSpeech.getLastTextToSpeechInstance() != null }
        val shadow = shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance())
        shadow.onInitListener.onInit(TextToSpeech.SUCCESS)
        return shadow
    }

    @Test
    fun `the alarm rings, reads the briefing in a deep male voice and stops on I'm up`() {
        val controller = fire()
        val service = controller.get()

        // --- Sonando ---
        assertEquals(AlarmPhase.RINGING, AlarmState.value.phase)
        val notification = shadowOf(service).lastForegroundNotification
        assertNotNull("must run as a foreground service", notification)
        assertNotNull("must open over the lock screen", notification.fullScreenIntent)
        assertEquals(AlarmActivity::class.java.name, shadowOf(app).nextStartedActivity?.component?.className)

        pump("the briefing to be prepared") { AlarmState.value.briefing != null }
        val briefing = AlarmState.value.briefing!!
        assertEquals("Good morning, Julian.".substringAfter(", "), briefing.greeting.substringAfter(", "))
        assertTrue(briefing.spoken.contains("10 habits"))

        // --- Tras los segundos de tono, JARVIS habla ---
        assertNull("the voice must wait for the tone first", ShadowTextToSpeech.getLastTextToSpeechInstance())
        mainLooper.idleFor(Duration.ofSeconds(6))
        val voice = readyVoice()
        pump("the whole briefing to be spoken") { voice.spokenTextList.size >= briefing.all.size }
        assertEquals(briefing.all, voice.spokenTextList.take(briefing.all.size))
        assertEquals("en-gb-x-rjs-local", voice.currentVoice?.name)
        pump("speaking to end") { !AlarmState.value.speaking }
        assertEquals(AlarmPhase.RINGING, AlarmState.value.phase) // sigue sonando hasta que la apaguen

        // --- "I'm up": calla el tono y repite el resumen una vez ---
        AlarmService.dismiss(context)
        deliver(controller, 2)
        assertEquals(AlarmPhase.BRIEFING, AlarmState.value.phase)
        pump("the briefing to be repeated") { voice.spokenTextList.size >= briefing.all.size * 2 }
        pump("the service to finish") { shadowOf(service).isStoppedBySelf }
        // La pantalla se queda en el resumen (para leerlo) hasta "Start my day".
        assertEquals(AlarmPhase.BRIEFING, AlarmState.value.phase)
        assertFalse(AlarmState.value.speaking)
        assertEquals(briefing, AlarmState.value.briefing)
    }

    @Test
    fun `the deep tone and the chosen voice are applied`() {
        AppGraph.settings.update { it.copy(voiceName = "en-us-x-sfg-local", voicePitch = VoicePicker.PITCH_VERY_DEEP) }
        val controller = fire()
        pump("the briefing to be prepared") { AlarmState.value.briefing != null }
        mainLooper.idleFor(Duration.ofSeconds(6))
        val voice = readyVoice()
        pump("JARVIS to start speaking") { voice.spokenTextList.isNotEmpty() }
        assertEquals("en-us-x-sfg-local", voice.currentVoice?.name)
        AlarmService.stop(context)
        deliver(controller, 2)
    }

    @Test
    fun `snooze silences the alarm and schedules it again`() {
        val controller = fire()
        val service = controller.get()
        val before = System.currentTimeMillis()

        AlarmService.snooze(context)
        deliver(controller, 2)

        assertEquals(AlarmPhase.IDLE, AlarmState.value.phase)
        assertTrue(shadowOf(service).isStoppedBySelf)
        val alarms = shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms
        val snoozed = alarms.single { it.alarmClockInfo != null && it.triggerAtMs < before + 11 * 60_000L }
        assertTrue(snoozed.triggerAtMs >= before + 9 * 60_000L)
    }

    @Test
    fun `with the voice turned off the alarm only rings`() {
        AppGraph.settings.update { it.copy(voiceEnabled = false) }
        val controller = fire()
        pump("the briefing to be prepared") { AlarmState.value.briefing != null }
        mainLooper.idleFor(Duration.ofSeconds(30))
        assertNull(ShadowTextToSpeech.getLastTextToSpeechInstance())
        assertEquals(AlarmPhase.RINGING, AlarmState.value.phase)

        AlarmService.dismiss(context)
        deliver(controller, 2)
        pump("the service to finish") { shadowOf(controller.get()).isStoppedBySelf }
        assertNull(ShadowTextToSpeech.getLastTextToSpeechInstance())
    }

    @Test
    fun `an alarm nobody turns off gives up after ten minutes`() {
        AppGraph.settings.update { it.copy(voiceEnabled = false, toneSeconds = 60) }
        val controller = fire()
        pump("the briefing to be prepared") { AlarmState.value.briefing != null }
        mainLooper.idleFor(Duration.ofMinutes(12))
        pump("the service to stop by itself") { shadowOf(controller.get()).isStoppedBySelf }
        assertEquals(AlarmPhase.IDLE, AlarmState.value.phase)
    }
}
