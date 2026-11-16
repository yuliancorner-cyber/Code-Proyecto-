package com.jarvis.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.jarvis.app.data.AppGraph
import com.jarvis.app.logic.HabitReminders
import com.jarvis.app.logic.HabitStats
import com.jarvis.app.logic.TaskLists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Programa los recordatorios de tareas y habitos con AlarmManager (el
 * "despertador" interno de Android, que funciona con la app cerrada).
 *
 * Estrategia simple y robusta: [sync] se llama despues de CUALQUIER cambio
 * (y al encender el celular) y deja programado exactamente lo que toca:
 * un aviso por cada tarea con recordatorio pendiente y por cada habito con
 * hora de aviso. Lo que ya no toca se cancela.
 */
object ReminderScheduler {
    const val ACTION_REMIND = "com.jarvis.app.action.REMIND"
    const val KIND_TASK = "task"
    const val KIND_HABIT = "habit"

    private const val PREFS = "reminders"
    private const val KEY_SCHEDULED = "scheduled"

    // Evita que dos sync a la vez se pisen.
    private val mutex = Mutex()

    suspend fun sync(context: Context) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val app = context.applicationContext
            val repo = AppGraph.repo
            val now = LocalDateTime.now()
            val today = now.toLocalDate().toEpochDay()
            val zone = ZoneId.systemDefault()

            // clave ("task/12") -> momento del aviso en milisegundos
            val wanted = mutableMapOf<String, Long>()

            for (task in repo.tasks.all()) {
                val at = TaskLists.reminderAt(task) ?: continue
                if (at.isAfter(now)) {
                    wanted["$KIND_TASK/${task.id}"] = at.atZone(zone).toInstant().toEpochMilli()
                }
            }

            val checksToday = repo.habits.allChecks().filter { it.date == today }.associateBy { it.habitId }
            for (habit in repo.habits.all()) {
                val minutes = habit.reminderMinutes ?: continue
                if (habit.archived) continue
                val doneToday = HabitStats.isDone(checksToday[habit.id]?.count ?: 0, habit.target)
                val at = HabitReminders.next(habit.days, minutes, doneToday, now) ?: continue
                wanted["$KIND_HABIT/${habit.id}"] = at.atZone(zone).toInstant().toEpochMilli()
            }

            val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val previous = prefs.getStringSet(KEY_SCHEDULED, emptySet()) ?: emptySet()
            val manager = app.getSystemService(AlarmManager::class.java)

            for (key in previous - wanted.keys) {
                manager.cancel(pendingIntent(app, key))
            }
            for ((key, at) in wanted) {
                setExact(manager, at, pendingIntent(app, key))
            }
            prefs.edit().putStringSet(KEY_SCHEDULED, wanted.keys.toSet()).apply()
        }
    }

    /** Posponer: vuelve a avisar dentro de [minutes] (aparte de lo que programe sync). */
    fun snooze(context: Context, key: String, minutes: Int) {
        val app = context.applicationContext
        val manager = app.getSystemService(AlarmManager::class.java)
        val at = System.currentTimeMillis() + minutes * 60_000L
        setExact(manager, at, pendingIntent(app, key, snoozed = true))
    }

    /**
     * El "sobre" que AlarmManager nos devolvera a la hora indicada. Cada clave
     * tiene una direccion (Uri) distinta para que Android no los confunda.
     */
    private fun pendingIntent(context: Context, key: String, snoozed: Boolean = false): PendingIntent {
        val suffix = if (snoozed) "?snoozed=1" else ""
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_REMIND)
            .setData(Uri.parse("jarvis://reminder/$key$suffix"))
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun setExact(manager: AlarmManager, at: Long, intent: PendingIntent) {
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        try {
            if (canExact) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            } else {
                // Sin permiso de alarmas exactas: llega igual, con unos minutos de margen.
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            }
        } catch (e: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
    }
}
