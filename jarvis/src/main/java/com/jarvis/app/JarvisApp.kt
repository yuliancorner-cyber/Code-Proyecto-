package com.jarvis.app

import android.app.Application
import com.jarvis.app.alarm.AlarmScheduler
import com.jarvis.app.data.AppGraph
import com.jarvis.app.reminders.Notifications
import com.jarvis.app.reminders.ReminderScheduler
import kotlinx.coroutines.launch

/**
 * La clase "Application" se crea antes que cualquier pantalla, servicio o
 * receptor. Aqui se prepara lo que todos comparten.
 */
class JarvisApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        Notifications.ensureChannels(this)

        // Por si el sistema borro lo programado (p. ej. tras forzar el cierre).
        AppGraph.scope.launch {
            AlarmScheduler.schedule(this@JarvisApp)
            ReminderScheduler.sync(this@JarvisApp)
        }
    }
}
