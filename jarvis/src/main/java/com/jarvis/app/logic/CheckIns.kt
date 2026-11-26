package com.jarvis.app.logic

import com.jarvis.app.data.Habit
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * "Check-ins": varias veces al dia JARVIS pregunta, con una notificacion, por
 * cada habito que aun no has marcado, y respondes ahi mismo sin abrir la app.
 *
 * Aqui esta la parte pura (sin Android, con tests): a que horas preguntar, que
 * habitos tocan en cada ronda y como se redacta cada pregunta.
 */
object CheckInLogic {

    /** Horas por defecto: mediodia, tarde y noche. */
    val DEFAULT_TIMES = listOf(12 * 60, 17 * 60, 21 * 60)

    const val MAX_TIMES = 8

    private const val MINUTES_PER_DAY = 24 * 60

    /** Horas validas, sin repetir y en orden. */
    fun normalize(times: List<Int>): List<Int> =
        times.filter { it in 0 until MINUTES_PER_DAY }.distinct().sorted().take(MAX_TIMES)

    /**
     * ¿Es la ultima ronda del dia? En ella se pregunta por TODO lo pendiente,
     * aunque su hora planeada aun no haya llegado (si no, nunca se preguntaria).
     */
    fun isLastOfDay(times: List<Int>, minutes: Int): Boolean {
        val last = normalize(times).lastOrNull() ?: return true
        return minutes >= last
    }

    /** Proxima vez (despues de [now]) que toca la ronda de las [minutes]: hoy o manana. */
    fun nextOccurrence(minutes: Int, now: LocalDateTime): LocalDateTime {
        val today = now.toLocalDate().atTime(LocalTime.of(minutes / 60, minutes % 60))
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    /** La siguiente ronda de HOY despues de [nowMinutes], o null si ya no quedan. */
    fun nextToday(times: List<Int>, nowMinutes: Int): Int? = normalize(times).firstOrNull { it > nowMinutes }

    /**
     * Habitos por los que preguntar ahora, en el orden de la cuadricula.
     *
     * Se pregunta por un habito si: no esta archivado, tiene activadas las
     * preguntas, toca ese dia, aun no esta hecho, no respondiste "Not today"
     * ([skipped]) ni "Later" en esta ronda ([later]), y su hora planeada ya
     * llego (o no tiene hora, o [askEverything] porque es la ultima ronda).
     */
    fun pending(
        habits: List<Habit>,
        index: CheckIndex,
        day: LocalDate,
        nowMinutes: Int,
        askEverything: Boolean,
        skipped: Set<Long>,
        later: Set<Long>
    ): List<Habit> {
        val epochDay = day.toEpochDay()
        return habits
            .filter { h ->
                !h.archived &&
                    h.askInCheckIns &&
                    HabitStats.isScheduled(h.days, day) &&
                    !index.isDone(h, epochDay) &&
                    h.id !in skipped &&
                    h.id !in later &&
                    (askEverything || h.cue != Habit.CUE_TIME || h.timeMinutes == null || h.timeMinutes <= nowMinutes)
            }
            .sortedWith(compareBy<Habit>({ it.position }, { it.id }))
    }

    /** Un habito con contador (agua: 8 vasos) se responde de otra forma. */
    fun isCounter(habit: Habit): Boolean = habit.target > 1

    /** La pregunta principal de la notificacion. */
    fun question(habit: Habit, count: Int): String {
        if (isCounter(habit)) {
            val unit = habit.unit.trim().ifEmpty { "times" }
            return "$count of ${habit.target} $unit so far today."
        }
        val behavior = Formats.lowerFirst(Formats.stripEndPunctuation(habit.behavior))
        return if (behavior.isEmpty()) "Did you keep this habit today?" else "Did you $behavior today?"
    }

    /**
     * Una linea extra debajo de la pregunta: el aviso "never miss twice" si
     * fallaste la ultima vez; si no, tu plan; si no, la version de dos minutos.
     */
    fun hint(habit: Habit, missedLastTime: Boolean, is24Hour: Boolean): String? {
        if (missedLastTime) return "You missed it last time. Never miss twice."
        val plan = Intention.forHabit(habit, is24Hour)
        if (plan.isNotEmpty()) return plan
        if (habit.tinyVersion.isNotBlank()) return "Two-minute version: ${habit.tinyVersion.trim()}"
        return null
    }

    /** "📖 Reading" */
    fun title(habit: Habit): String =
        listOf(habit.emoji.trim(), habit.name.trim()).filter { it.isNotEmpty() }.joinToString(" ")

    /**
     * El mensaje final cuando ya no queda nada que preguntar.
     * @param nextTime la hora de la siguiente ronda de hoy ya formateada, o null.
     * @return titulo y texto.
     */
    fun summary(done: Int, total: Int, nextTime: String?): Pair<String, String> {
        val tally = "$done of $total habits done today."
        return if (total > 0 && done >= total) {
            "All caught up" to "$tally Great work."
        } else {
            val next = if (nextTime != null) "I'll ask again at $nextTime." else "See you tomorrow."
            "Got it" to "$tally $next"
        }
    }
}
