package com.jarvis.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** AlarmManager llama aqui a la hora de la alarma (aunque la app este cerrada). */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_FIRE) return
        AlarmService.fire(context)
        // Deja ya programada la alarma del dia siguiente.
        AlarmScheduler.schedule(context)
    }
}
