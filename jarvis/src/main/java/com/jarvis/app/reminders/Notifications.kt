package com.jarvis.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.jarvis.app.MainActivity
import com.jarvis.app.R

/** Canales de notificacion y utilidades comunes. */
object Notifications {
    const val CHANNEL_ALARM = "alarm"
    const val CHANNEL_REMINDERS = "reminders"

    /** Id fijo de la notificacion de la alarma (solo hay una a la vez). */
    const val ID_ALARM = 1
    const val ID_MISSED_ALARM = 2

    /** Color de acento de las notificaciones (cian). */
    const val ACCENT = 0xFF00E5FF.toInt()

    /**
     * Un "canal" agrupa notificaciones del mismo tipo; el usuario puede
     * silenciar cada canal por separado en Ajustes. Crearlos dos veces no
     * hace nada, asi que se llama en cada arranque.
     */
    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        val alarm = NotificationChannel(
            CHANNEL_ALARM,
            context.getString(R.string.channel_alarm),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_alarm_desc)
            // El sonido y la vibracion los pone el servicio de la alarma.
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        val reminders = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.channel_reminders),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_reminders_desc)
            enableVibration(true)
        }

        manager.createNotificationChannel(alarm)
        manager.createNotificationChannel(reminders)
    }

    fun enabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Al tocar una notificacion se abre la app. */
    fun openApp(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
