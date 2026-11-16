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
        // Solo se atienden los avisos del sistema para los que esta registrado.
        if (intent.action !in ACTIONS) return
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

    private companion object {
        val ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
    }
}
