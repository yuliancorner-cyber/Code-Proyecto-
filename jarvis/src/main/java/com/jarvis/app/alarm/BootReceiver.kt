package com.jarvis.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jarvis.app.data.AppGraph
import com.jarvis.app.reminders.ReminderScheduler
import kotlinx.coroutines.launch

/**
 * Al reiniciar el celular Android olvida todas las alarmas programadas, y al
 * cambiar la hora o la zona horaria quedan desfasadas. En esos casos (y al
 * actualizar la app) se vuelve a programar todo.
 *
 * En Xiaomi esto solo funciona con "Inicio automatico" activado para JARVIS.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        AppGraph.scope.launch {
            try {
                AlarmScheduler.schedule(context)
                ReminderScheduler.sync(context)
            } finally {
                pending.finish()
            }
        }
    }
}
