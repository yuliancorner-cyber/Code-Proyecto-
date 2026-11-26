package com.jarvis.app.reminders

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jarvis.app.R
import com.jarvis.app.data.AppGraph
import com.jarvis.app.data.Habit
import com.jarvis.app.data.Task
import com.jarvis.app.logic.Formats
import com.jarvis.app.logic.HabitStats
import com.jarvis.app.logic.Intention
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Recibe tres tipos de aviso:
 *  - ACTION_REMIND: llego la hora de un recordatorio -> muestra la notificacion
 *    (o, si es una hora de check-in, empieza la ronda de preguntas).
 *  - ACTION_DONE:   tocaste "Done" en la notificacion -> marca la tarea/habito.
 *  - ACTION_SNOOZE: tocaste "Snooze" -> vuelve a avisar en 10 minutos.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // La direccion tiene la forma jarvis://reminder/task/12
        val segments = intent.data?.pathSegments ?: return
        if (segments.size < 2) return
        val kind = segments[0]
        val id = segments[1].toLongOrNull() ?: return
        val key = "$kind/$id"

        // goAsync: pide a Android unos segundos extra para leer la base de datos.
        val pending = goAsync()
        AppGraph.scope.launch {
            try {
                when (intent.action) {
                    ReminderScheduler.ACTION_REMIND -> remind(context, kind, id, key)
                    ACTION_DONE -> {
                        cancelNotification(context, key)
                        markDone(kind, id)
                    }
                    ACTION_SNOOZE -> {
                        cancelNotification(context, key)
                        ReminderScheduler.snooze(context, key, SNOOZE_MINUTES)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun remind(context: Context, kind: String, id: Long, key: String) {
        val repo = AppGraph.repo
        val is24 = DateFormat.is24HourFormat(context)
        when (kind) {
            ReminderScheduler.KIND_CHECKIN -> CheckIns.startRound(context, id.toInt())
            ReminderScheduler.KIND_TASK -> {
                val task = repo.tasks.byId(id)
                if (task != null && !task.done) notifyTask(context, key, task, is24)
            }
            ReminderScheduler.KIND_HABIT -> {
                val habit = repo.habits.byId(id)
                val today = LocalDate.now().toEpochDay()
                if (habit != null && !habit.archived) {
                    val count = repo.habits.check(id, today)?.count ?: 0
                    if (!HabitStats.isDone(count, habit.target)) notifyHabit(context, key, habit, count, is24)
                }
            }
        }
        // Deja programado el siguiente aviso (p. ej. el del habito de manana).
        ReminderScheduler.sync(context)
    }

    private suspend fun markDone(kind: String, id: Long) {
        val repo = AppGraph.repo
        when (kind) {
            ReminderScheduler.KIND_TASK -> repo.setTaskDone(id, true)
            ReminderScheduler.KIND_HABIT -> {
                val habit = repo.habits.byId(id) ?: return
                repo.incrementHabit(habit, LocalDate.now().toEpochDay())
            }
        }
    }

    private fun notifyTask(context: Context, key: String, task: Task, is24: Boolean) {
        val lines = mutableListOf<String>()
        Intention.forTask(task, is24).takeIf { it.isNotEmpty() }?.let { lines += it }
        if (task.firstStep.isNotBlank()) lines += "Start with: ${task.firstStep.trim()}"
        val summary = listOfNotNull(
            task.dueMinutes?.let { Formats.time(it, is24) },
            task.location.takeIf { it.isNotBlank() }
        ).joinToString(" · ").ifEmpty { "It's time." }
        show(context, key, task.title, summary, lines.joinToString("\n"), doneLabel = "Done")
    }

    private fun notifyHabit(context: Context, key: String, habit: Habit, count: Int, is24: Boolean) {
        val title = listOf(habit.emoji, habit.name).filter { it.isNotBlank() }.joinToString(" ")
        val lines = mutableListOf<String>()
        Intention.forHabit(habit, is24).takeIf { it.isNotEmpty() }?.let { lines += it }
        if (habit.tinyVersion.isNotBlank()) lines += "Two-minute version: ${habit.tinyVersion.trim()}"
        val counter = habit.target > 1
        val summary = if (counter) {
            "$count of ${habit.target} ${habit.unit}".trim()
        } else {
            lines.firstOrNull() ?: "Time for your habit."
        }
        show(context, key, title, summary, lines.joinToString("\n"), doneLabel = if (counter) "+1" else "Done")
    }

    private fun show(
        context: Context,
        key: String,
        title: String,
        summary: String,
        detail: String,
        doneLabel: String
    ) {
        if (!Notifications.enabled(context)) return
        // Con el celular bloqueado (si ocultas el contenido sensible) no se ve el titulo.
        val public = NotificationCompat.Builder(context, Notifications.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(Notifications.ACCENT)
            .setContentTitle("JARVIS reminder")
            .build()
        val builder = NotificationCompat.Builder(context, Notifications.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(Notifications.ACCENT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
            .setContentTitle(title)
            .setContentText(summary)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(Notifications.openApp(context))
            .addAction(0, doneLabel, action(context, ACTION_DONE, key))
            .addAction(0, "Snooze $SNOOZE_MINUTES min", action(context, ACTION_SNOOZE, key))
        if (detail.isNotBlank()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(detail))
        }
        try {
            NotificationManagerCompat.from(context).notify(key.hashCode(), builder.build())
        } catch (e: SecurityException) {
            // Sin permiso de notificaciones: no hay nada que mostrar.
        }
    }

    private fun cancelNotification(context: Context, key: String) {
        NotificationManagerCompat.from(context).cancel(key.hashCode())
    }

    private fun action(context: Context, action: String, key: String): PendingIntent =
        PendingIntent.getBroadcast(
            context, 0,
            Intent(context, ReminderReceiver::class.java)
                .setAction(action)
                .setData(Uri.parse("jarvis://reminder/$key")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    companion object {
        const val ACTION_DONE = "com.jarvis.app.action.REMINDER_DONE"
        const val ACTION_SNOOZE = "com.jarvis.app.action.REMINDER_SNOOZE"
        const val SNOOZE_MINUTES = 10
    }
}
