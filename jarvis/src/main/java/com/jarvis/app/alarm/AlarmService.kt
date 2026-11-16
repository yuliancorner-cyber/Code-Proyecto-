package com.jarvis.app.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.jarvis.app.R
import com.jarvis.app.data.AppGraph
import com.jarvis.app.logic.Briefing
import com.jarvis.app.logic.BriefingBuilder
import com.jarvis.app.logic.Formats
import com.jarvis.app.reminders.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * El servicio que HACE SONAR la alarma. Es un "servicio en primer plano":
 * sigue funcionando con la pantalla apagada y la app cerrada, a cambio de
 * mostrar una notificacion mientras dura.
 *
 * Secuencia:
 *  1. Suena el tono (subiendo de volumen poco a poco) y vibra.
 *  2. Tras unos segundos baja el tono y JARVIS lee el resumen del dia.
 *     Luego el tono vuelve, y asi en bucle hasta que la apagues.
 *  3. Al tocar "I'm up": se calla el tono y repite el resumen una vez, ya
 *     contigo despierto. "Snooze": calla todo y vuelve a sonar en unos minutos.
 *  4. Si nadie la apaga en 10 minutos, se detiene sola y deja un aviso.
 */
class AlarmService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var speaker: Speaker? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var ringJob: Job? = null
    private var afterDismissJob: Job? = null
    private var briefing: Deferred<Briefing>? = null

    /** true mientras JARVIS habla: el tono se queda bajito de fondo. */
    private var ducked = false
    private var rampVolume = RAMP_START

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_FIRE -> fire()
            ACTION_DISMISS -> dismiss()
            ACTION_SNOOZE -> snooze()
            ACTION_STOP -> finish()
            // Android reinicio el servicio sin instrucciones: no hay nada que hacer.
            else -> if (ringJob == null && afterDismissJob == null) finish()
        }
        return START_NOT_STICKY
    }

    // ------------------------------------------------------------ Sonar

    private fun fire() {
        // Desde Android 8, un servicio arrancado en segundo plano debe mostrar
        // su notificacion en pocos segundos o el sistema lo mata.
        startForegroundCompat(ringingNotification())

        // Si ya estaba sonando (dos avisos seguidos), no empezar de nuevo.
        if (ringJob?.isActive == true) return
        afterDismissJob?.cancel()

        val settings = AppGraph.settings.value
        acquireWakeLock()
        AlarmState.update { AlarmUi(phase = AlarmPhase.RINGING) }

        val startedAt = SystemClock.elapsedRealtime()
        startTone()
        startVibration()
        openAlarmScreen()

        // El resumen se prepara en paralelo (incluye bajar el clima de internet).
        val loading = scope.async { loadBriefing() }
        briefing = loading

        ringJob = scope.launch {
            // Subida gradual del volumen durante los primeros segundos.
            launch {
                while (isActive && rampVolume < 1f) {
                    delay(1_000)
                    rampVolume = (rampVolume + RAMP_STEP).coerceAtMost(1f)
                    applyToneVolume()
                }
            }

            val text = loading.await()
            AlarmState.update { it.copy(briefing = text) }

            while (isActive) {
                delay(settings.toneSeconds.coerceIn(5, 120) * 1_000L)
                if (settings.voiceEnabled) {
                    say(text, settings.speechRate)
                }
                if (SystemClock.elapsedRealtime() - startedAt > MAX_RING_MS) {
                    giveUp()
                    break
                }
            }
        }
    }

    /** JARVIS lee el resumen con el tono bajito de fondo. */
    private suspend fun say(text: Briefing, rate: Float) {
        ducked = true
        applyToneVolume()
        AlarmState.update { it.copy(speaking = true) }
        try {
            voice().speak(text.all, rate)
        } finally {
            ducked = false
            applyToneVolume()
            AlarmState.update { it.copy(speaking = false) }
        }
    }

    // ----------------------------------------------------- Apagar / posponer

    /** "I'm up": se acabo el tono; JARVIS repite el resumen y el servicio termina. */
    private fun dismiss() {
        if (ringJob == null) {
            // No estaba sonando (p. ej. tocaron "Stop" de la notificacion final).
            finish()
            return
        }
        val ringing = ringJob
        ringing?.cancel()
        ringJob = null
        stopTone()
        stopVibration()
        speaker?.stop()
        AlarmScheduler.cancelSnooze(this)

        val settings = AppGraph.settings.value
        AlarmState.update { it.copy(phase = AlarmPhase.BRIEFING, speaking = false) }
        notify(briefingNotification())

        afterDismissJob = scope.launch {
            // Espera a que la fase anterior termine de callarse; si no, su
            // "silencio" cortaria la primera frase del resumen.
            ringing?.join()
            val text = briefing?.await() ?: loadBriefing()
            AlarmState.update { it.copy(briefing = text) }
            if (settings.voiceEnabled) {
                AlarmState.update { it.copy(speaking = true) }
                try {
                    voice().speak(text.all, settings.speechRate)
                } finally {
                    AlarmState.update { it.copy(speaking = false) }
                }
            }
            finish()
        }
    }

    private fun snooze() {
        val minutes = AppGraph.settings.value.snoozeMinutes.coerceIn(1, 60)
        AlarmScheduler.snooze(this, minutes)
        AlarmState.reset()
        finish()
    }

    /** Nadie apago la alarma: se detiene y deja una notificacion. */
    private fun giveUp() {
        AlarmState.reset()
        if (Notifications.enabled(this)) {
            val missed = NotificationCompat.Builder(this, Notifications.CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(Notifications.ACCENT)
                .setContentTitle("Missed alarm")
                .setContentText("Your ${timeText()} alarm rang for 10 minutes.")
                .setAutoCancel(true)
                .setContentIntent(Notifications.openApp(this))
                .build()
            try {
                NotificationManagerCompat.from(this).notify(Notifications.ID_MISSED_ALARM, missed)
            } catch (e: SecurityException) {
                // Sin permiso de notificaciones.
            }
        }
        finish()
    }

    /** Detiene todo y cierra el servicio. */
    private fun finish() {
        ringJob?.cancel()
        ringJob = null
        afterDismissJob?.cancel()
        afterDismissJob = null
        stopTone()
        stopVibration()
        speaker?.stop()
        AlarmState.update {
            // Si estaba sonando, la pantalla de la alarma se cierra. Si ya
            // estaba en el resumen, se queda abierta para poder leerlo.
            if (it.phase == AlarmPhase.RINGING) AlarmUi() else it.copy(speaking = false)
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        stopTone()
        stopVibration()
        speaker?.shutdown()
        speaker = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        super.onDestroy()
    }

    // ------------------------------------------------------------- Piezas

    private suspend fun loadBriefing(): Briefing =
        try {
            BriefingLoader.load()
        } catch (e: Exception) {
            // Pase lo que pase con los datos, la alarma tiene que saludar.
            val name = AppGraph.settings.value.userName.trim()
            val hello = BriefingBuilder.greetingWord(LocalTime.now().hour)
            Briefing(if (name.isEmpty()) "$hello." else "$hello, $name.", listOf("It's time to get up."))
        }

    private fun voice(): Speaker =
        speaker ?: Speaker(this, asAlarm = true).also { speaker = it }

    private fun startTone() {
        stopTone()
        rampVolume = RAMP_START
        ducked = false
        // El tono de alarma que tengas elegido en el celular; si no hay, el de llamada.
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        if (uri == null) return
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        // USAGE_ALARM: suena por el volumen de alarma, tambien en silencio.
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmService, uri)
                isLooping = true
                setVolume(rampVolume, rampVolume)
                prepare()
                start()
            }
        } catch (e: Exception) {
            // Sin tono (archivo borrado, etc.): quedan la vibracion y la voz.
            player = null
        }
    }

    private fun applyToneVolume() {
        val volume = if (ducked) DUCKED_VOLUME else rampVolume
        try {
            player?.setVolume(volume, volume)
        } catch (e: IllegalStateException) {
            // El reproductor ya se habia liberado.
        }
    }

    private fun stopTone() {
        player?.let {
            try {
                it.stop()
            } catch (e: IllegalStateException) {
                // No estaba sonando.
            }
            it.release()
        }
        player = null
    }

    private fun startVibration() {
        val v = getSystemService(Vibrator::class.java) ?: return
        if (!v.hasVibrator()) return
        vibrator = v
        // Patron: vibra 0,6 s, pausa 1 s, y repite desde el principio (indice 0).
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 600, 1_000), 0)
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        @Suppress("DEPRECATION")
        v.vibrate(effect, attributes)
    }

    private fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val power = getSystemService(PowerManager::class.java)
        // Mantiene despierto el procesador (no la pantalla) mientras suena.
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:alarm").apply {
            acquire(MAX_RING_MS + 5 * 60_000L)
        }
    }

    /** Intenta abrir la pantalla de la alarma directamente (si Android lo permite). */
    private fun openAlarmScreen() {
        try {
            startActivity(AlarmActivity.intent(this))
        } catch (e: Exception) {
            // Bloqueado por el sistema: la abrira la notificacion a pantalla completa.
        }
    }

    // ------------------------------------------------------ Notificaciones

    private fun timeText(): String {
        val now = LocalTime.now()
        return Formats.time(now.hour * 60 + now.minute, DateFormat.is24HourFormat(this))
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this, requestCode,
            Intent(this, AlarmService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun screenPendingIntent(): PendingIntent =
        PendingIntent.getActivity(
            this, 0, AlarmActivity.intent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun ringingNotification(): Notification {
        val name = AppGraph.settings.value.userName.trim()
        val hello = BriefingBuilder.greetingWord(LocalTime.now().hour)
        val screen = screenPendingIntent()
        return NotificationCompat.Builder(this, Notifications.CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(Notifications.ACCENT)
            .setContentTitle(if (name.isEmpty()) hello else "$hello, $name")
            .setContentText("${timeText()} · Time to get up")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(screen)
            // "Pantalla completa": con el celular bloqueado, abre la pantalla
            // de la alarma en vez de mostrar solo la notificacion.
            .setFullScreenIntent(screen, true)
            .addAction(0, "Snooze", servicePendingIntent(ACTION_SNOOZE, 1))
            .addAction(0, "I'm up", servicePendingIntent(ACTION_DISMISS, 2))
            .build()
    }

    private fun briefingNotification(): Notification =
        NotificationCompat.Builder(this, Notifications.CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(Notifications.ACCENT)
            .setContentTitle("Your morning briefing")
            .setContentText("JARVIS is speaking")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(screenPendingIntent())
            .addAction(0, "Stop", servicePendingIntent(ACTION_STOP, 3))
            .build()

    private fun startForegroundCompat(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else 0
        ServiceCompat.startForeground(this, Notifications.ID_ALARM, notification, type)
    }

    private fun notify(notification: Notification) {
        try {
            NotificationManagerCompat.from(this).notify(Notifications.ID_ALARM, notification)
        } catch (e: SecurityException) {
            // Sin permiso de notificaciones: la alarma suena igual.
        }
    }

    companion object {
        private const val ACTION_FIRE = "com.jarvis.app.action.SERVICE_FIRE"
        private const val ACTION_DISMISS = "com.jarvis.app.action.SERVICE_DISMISS"
        private const val ACTION_SNOOZE = "com.jarvis.app.action.SERVICE_SNOOZE"
        private const val ACTION_STOP = "com.jarvis.app.action.SERVICE_STOP"

        /** Tiempo maximo sonando sin que nadie la apague. */
        private const val MAX_RING_MS = 10 * 60_000L

        private const val RAMP_START = 0.15f
        private const val RAMP_STEP = 0.06f
        private const val DUCKED_VOLUME = 0.06f

        /** Hace sonar la alarma ahora (la llama AlarmReceiver, o "Test alarm"). */
        fun fire(context: Context) {
            Notifications.ensureChannels(context)
            ContextCompat.startForegroundService(
                context, Intent(context, AlarmService::class.java).setAction(ACTION_FIRE)
            )
        }

        fun dismiss(context: Context) = send(context, ACTION_DISMISS)
        fun snooze(context: Context) = send(context, ACTION_SNOOZE)
        fun stop(context: Context) = send(context, ACTION_STOP)

        private fun send(context: Context, action: String) {
            try {
                context.startService(Intent(context, AlarmService::class.java).setAction(action))
            } catch (e: IllegalStateException) {
                // El servicio ya no existe y la app esta en segundo plano: nada que detener.
                AlarmState.reset()
            }
        }
    }
}
