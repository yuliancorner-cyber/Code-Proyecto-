package com.jarvis.app.reminders

import android.app.NotificationManager
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
import com.jarvis.app.logic.CheckInLogic
import com.jarvis.app.logic.CheckIndex
import com.jarvis.app.logic.Formats
import com.jarvis.app.logic.HabitStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

/**
 * Los check-ins de habitos: una notificacion que pregunta, uno por uno, por
 * los habitos que aun no marcaste hoy. Respondes con los botones y la misma
 * notificacion pasa a la siguiente pregunta, sin abrir la app:
 *
 *  - Casilla normal:  "Yes" (hecho) · "Not today" (no se vuelve a preguntar hoy)
 *                     · "Later" (se pregunta en la proxima ronda).
 *  - Contador (agua): "+1" · "All 8" · "Later".
 *
 * Cada "ronda" empieza a una hora elegida en Ajustes (la programa
 * ReminderScheduler) o con el boton "Check in now".
 */
object CheckIns {
    const val ACTION_YES = "com.jarvis.app.action.CHECKIN_YES"
    const val ACTION_NO = "com.jarvis.app.action.CHECKIN_NO"
    const val ACTION_LATER = "com.jarvis.app.action.CHECKIN_LATER"
    const val ACTION_PLUS = "com.jarvis.app.action.CHECKIN_PLUS"
    const val ACTION_FILL = "com.jarvis.app.action.CHECKIN_FILL"

    private const val PREFS = "checkins"
    private const val SUMMARY_TIMEOUT_MS = 15_000L
    private const val MINUTES_PER_DAY = 24 * 60

    // Evita que dos respuestas seguidas se pisen.
    private val mutex = Mutex()

    /** La ronda en curso: para que dia es, a que hora empezo y si pregunta por todo. */
    private data class Round(val day: Long, val minutes: Int, val askEverything: Boolean)

    /**
     * Empieza una ronda de preguntas.
     * @param minutes hora programada de la ronda, o null si la pidio el usuario ("Check in now").
     * @return true si habia algo que preguntar (y se mostro la notificacion).
     */
    suspend fun startRound(context: Context, minutes: Int?): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val settings = AppGraph.settings.value
            if (minutes != null && (!settings.checkInsEnabled || !settings.setupDone)) return@withLock false
            val now = LocalTime.now()
            val nowMinutes = now.hour * 60 + now.minute
            val round = Round(
                day = LocalDate.now().toEpochDay(),
                minutes = minutes ?: nowMinutes,
                // A mano, o en la ultima ronda del dia, se pregunta por todo lo pendiente.
                askEverything = minutes == null || CheckInLogic.isLastOfDay(settings.checkInTimes, minutes)
            )
            saveRound(context, round)
            // Se quita la de la ronda anterior para que la nueva suene.
            NotificationManagerCompat.from(context).cancel(Notifications.ID_CHECKIN)
            show(context, round, answered = false)
        }
    }

    /** Responde a un boton de la notificacion. */
    suspend fun handle(context: Context, intent: Intent) = withContext(Dispatchers.IO) {
        mutex.withLock {
            // La direccion tiene la forma jarvis://checkin/<habitId>/<dia>
            val segments = intent.data?.pathSegments ?: return@withLock
            val habitId = segments.getOrNull(0)?.toLongOrNull() ?: return@withLock
            val day = segments.getOrNull(1)?.toLongOrNull() ?: return@withLock
            val round = loadRound(context) ?: return@withLock
            val repo = AppGraph.repo
            val habit = repo.habits.byId(habitId)

            if (habit != null) {
                val count = repo.habits.check(habitId, day)?.count ?: 0
                val target = maxOf(1, habit.target)
                when (intent.action) {
                    ACTION_YES, ACTION_FILL -> repo.setHabitCount(habitId, day, target, refreshCheckIn = false)
                    ACTION_PLUS -> repo.setHabitCount(habitId, day, (count + 1).coerceAtMost(target), refreshCheckIn = false)
                    ACTION_NO -> addTo(context, KEY_SKIPPED, round.day, habitId)
                    ACTION_LATER -> addTo(context, KEY_LATER, round.day, habitId)
                }
            }
            show(context, round, answered = true)
        }
    }

    /**
     * Si la pregunta esta en pantalla, la pone al dia (p. ej. marcaste ese
     * habito desde la app). Si ya no queda nada que preguntar, la quita.
     */
    suspend fun refreshIfShowing(context: Context) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!isShowing(context)) return@withLock
            val round = loadRound(context)
            if (round == null) {
                NotificationManagerCompat.from(context).cancel(Notifications.ID_CHECKIN)
            } else {
                show(context, round, answered = false)
            }
        }
    }

    // ------------------------------------------------------------ Mostrar

    /**
     * Muestra la siguiente pregunta de la ronda. Si no queda ninguna: tras
     * responder, un resumen que se va solo; si no, quita la notificacion.
     */
    private suspend fun show(context: Context, round: Round, answered: Boolean): Boolean {
        val repo = AppGraph.repo
        val day = LocalDate.ofEpochDay(round.day)
        val today = LocalDate.now()
        val now = LocalTime.now()
        // Si respondes a la pregunta de ayer, a esa hora ya paso todo lo de ayer.
        val nowMinutes = if (day.isBefore(today)) MINUTES_PER_DAY else now.hour * 60 + now.minute

        val habits = repo.habits.all()
        val index = CheckIndex(repo.habits.allChecks())
        val pending = CheckInLogic.pending(
            habits = habits,
            index = index,
            day = day,
            nowMinutes = nowMinutes,
            askEverything = round.askEverything,
            skipped = setFor(context, KEY_SKIPPED, round.day),
            later = setFor(context, KEY_LATER, round.day)
        )

        if (!Notifications.enabled(context)) return false
        val is24 = DateFormat.is24HourFormat(context)
        val notification = if (pending.isEmpty()) {
            if (!answered) {
                NotificationManagerCompat.from(context).cancel(Notifications.ID_CHECKIN)
                return false
            }
            val scheduled = habits.filter { !it.archived && HabitStats.isScheduled(it.days, day) }
            val done = scheduled.count { index.isDone(it, round.day) }
            val nextTime = if (day == today) {
                CheckInLogic.nextToday(AppGraph.settings.value.checkInTimes, nowMinutes)
                    ?.takeIf { AppGraph.settings.value.checkInsEnabled }
                    ?.let { Formats.time(it, is24) }
            } else null
            val (title, text) = CheckInLogic.summary(done, scheduled.size, nextTime)
            baseBuilder(context)
                .setContentTitle(title)
                .setContentText(text)
                .setSilent(true)
                .setAutoCancel(true)
                .setTimeoutAfter(SUMMARY_TIMEOUT_MS)
                .build()
        } else {
            val habit = pending.first()
            val count = index.count(habit.id, round.day)
            val missed = HabitStats.missedLastTime(habit.days, index.doneDays(habit), day)
            questionNotification(context, habit, count, missed, pending.size, round.day, is24)
        }
        return try {
            NotificationManagerCompat.from(context).notify(Notifications.ID_CHECKIN, notification)
            true
        } catch (e: SecurityException) {
            // Sin permiso de notificaciones.
            false
        }
    }

    private fun questionNotification(
        context: Context,
        habit: Habit,
        count: Int,
        missedLastTime: Boolean,
        left: Int,
        day: Long,
        is24: Boolean
    ): android.app.Notification {
        val question = CheckInLogic.question(habit, count)
        val hint = CheckInLogic.hint(habit, missedLastTime, is24)
        val builder = baseBuilder(context)
            .setContentTitle(CheckInLogic.title(habit))
            .setContentText(question)
            .setSubText(if (left == 1) "Check-in · last one" else "Check-in · $left left")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(if (hint != null) "$question\n$hint" else question)
            )
            .setOngoing(false)
            .setAutoCancel(false)

        if (CheckInLogic.isCounter(habit)) {
            builder.addAction(0, "+1", action(context, ACTION_PLUS, habit.id, day))
            builder.addAction(0, "All ${habit.target}", action(context, ACTION_FILL, habit.id, day))
        } else {
            builder.addAction(0, "Yes", action(context, ACTION_YES, habit.id, day))
            builder.addAction(0, "Not today", action(context, ACTION_NO, habit.id, day))
        }
        builder.addAction(0, "Later", action(context, ACTION_LATER, habit.id, day))
        return builder.build()
    }

    private fun baseBuilder(context: Context): NotificationCompat.Builder {
        // Lo que se ve con el celular bloqueado si ocultas el contenido sensible:
        // nunca el nombre del habito.
        val public = NotificationCompat.Builder(context, Notifications.CHANNEL_CHECKINS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(Notifications.ACCENT)
            .setContentTitle("Habit check-in")
            .setContentText("JARVIS has a question for you")
            .build()
        return NotificationCompat.Builder(context, Notifications.CHANNEL_CHECKINS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(Notifications.ACCENT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
            // Solo suena la primera pregunta de cada ronda, no cada respuesta.
            .setOnlyAlertOnce(true)
            .setContentIntent(Notifications.openApp(context))
    }

    private fun action(context: Context, action: String, habitId: Long, day: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context, 0,
            Intent(context, CheckInReceiver::class.java)
                .setAction(action)
                .setData(Uri.parse("jarvis://checkin/$habitId/$day")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun isShowing(context: Context): Boolean =
        try {
            context.getSystemService(NotificationManager::class.java)
                .activeNotifications.any { it.id == Notifications.ID_CHECKIN }
        } catch (e: Exception) {
            false
        }

    // ------------------------------------------------------------- Estado
    //
    // Se guarda en un archivo pequeno (SharedPreferences) porque cada boton
    // llega por separado, quiza con la app cerrada.

    private const val KEY_SKIPPED = "skipped"
    private const val KEY_LATER = "later"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun saveRound(context: Context, round: Round) {
        val p = prefs(context)
        val editor = p.edit()
            .putLong("day", round.day)
            .putInt("minutes", round.minutes)
            .putBoolean("askEverything", round.askEverything)
            // "Later" solo vale para la ronda en la que se dijo.
            .remove(KEY_LATER)
        // "Not today" vale todo el dia: se borra al cambiar de dia.
        if (p.getLong("${KEY_SKIPPED}Day", -1) != round.day) editor.remove(KEY_SKIPPED)
        editor.putLong("${KEY_SKIPPED}Day", round.day).putLong("${KEY_LATER}Day", round.day).apply()
    }

    private fun loadRound(context: Context): Round? {
        val p = prefs(context)
        if (!p.contains("day")) return null
        return Round(p.getLong("day", 0), p.getInt("minutes", 0), p.getBoolean("askEverything", true))
    }

    private fun setFor(context: Context, key: String, day: Long): Set<Long> {
        val p = prefs(context)
        if (p.getLong("${key}Day", -1) != day) return emptySet()
        return p.getStringSet(key, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
    }

    private fun addTo(context: Context, key: String, day: Long, habitId: Long) {
        val current = setFor(context, key, day).map { it.toString() }.toSet()
        prefs(context).edit()
            .putStringSet(key, current + habitId.toString())
            .putLong("${key}Day", day)
            .apply()
    }
}

/** Recibe los botones de la notificacion de check-in. */
class CheckInReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // goAsync: pide a Android unos segundos extra para guardar la respuesta.
        val pending = goAsync()
        AppGraph.scope.launch {
            try {
                CheckIns.handle(context, intent)
            } finally {
                pending.finish()
            }
        }
    }
}
