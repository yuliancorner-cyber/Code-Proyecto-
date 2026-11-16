package com.jarvis.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.jarvis.app.MainActivity
import com.jarvis.app.data.AppGraph
import com.jarvis.app.data.Settings
import com.jarvis.app.logic.AlarmTime
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Programa la alarma de la manana en AlarmManager.
 *
 * Usa setAlarmClock: es el metodo pensado para despertadores. Android lo
 * respeta aunque el celular este en reposo profundo o en ahorro de bateria,
 * y muestra el icono de alarma en la barra de estado.
 *
 * AlarmManager solo guarda UNA fecha: cada vez que suena (o cambian los
 * ajustes, o se reinicia el celular) hay que volver a programar la siguiente.
 */
object AlarmScheduler {
    const val ACTION_FIRE = "com.jarvis.app.action.ALARM_FIRE"

    private const val REQUEST_MAIN = 100
    private const val REQUEST_SNOOZE = 101
    private const val REQUEST_SHOW = 102

    /** Cuando sonara la proxima alarma segun los ajustes (null = desactivada). */
    fun nextAlarm(settings: Settings, now: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        if (!settings.alarmEnabled || !settings.setupDone) return null
        return AlarmTime.next(now, settings.alarmHour, settings.alarmMinute, settings.alarmDays)
    }

    /** Deja programada la proxima alarma (o la cancela si esta desactivada). */
    fun schedule(context: Context): LocalDateTime? {
        val app = context.applicationContext
        val manager = app.getSystemService(AlarmManager::class.java)
        val fire = firePendingIntent(app, REQUEST_MAIN)
        manager.cancel(fire)
        val next = nextAlarm(AppGraph.settings.value) ?: return null
        setClock(app, manager, next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), fire)
        return next
    }

    /** "Snooze": vuelve a sonar dentro de [minutes]. */
    fun snooze(context: Context, minutes: Int) {
        val app = context.applicationContext
        val manager = app.getSystemService(AlarmManager::class.java)
        val at = System.currentTimeMillis() + minutes * 60_000L
        setClock(app, manager, at, firePendingIntent(app, REQUEST_SNOOZE))
    }

    fun cancelSnooze(context: Context) {
        val app = context.applicationContext
        app.getSystemService(AlarmManager::class.java).cancel(firePendingIntent(app, REQUEST_SNOOZE))
    }

    private fun setClock(context: Context, manager: AlarmManager, at: Long, fire: PendingIntent) {
        // "show" = lo que se abre si tocas el icono de alarma del sistema.
        val show = PendingIntent.getActivity(
            context, REQUEST_SHOW,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            manager.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), fire)
        } catch (e: SecurityException) {
            // Solo pasa si el usuario quito el permiso de alarmas exactas
            // (Android 12): suena igual, pero puede retrasarse unos minutos.
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, fire)
        }
    }

    private fun firePendingIntent(context: Context, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, AlarmReceiver::class.java).setAction(ACTION_FIRE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
