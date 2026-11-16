package com.jarvis.app.logic

import java.time.LocalDateTime
import kotlin.math.abs

/** Una tarea tal como la necesita el resumen hablado. */
data class BriefingTask(
    val title: String,
    val minutes: Int?,
    val location: String
)

/** Todo lo que JARVIS necesita saber para dar los buenos dias. */
data class BriefingInput(
    val name: String,
    val now: LocalDateTime,
    /** Nombre de la ciudad, o null si aun no se eligio. */
    val city: String?,
    /** Clima, o null si no se pudo descargar. */
    val weather: Weather?,
    val fahrenheit: Boolean,
    /** Pasos de la rutina de la manana, en orden. */
    val routine: List<String>,
    /** Tareas de hoy, ya ordenadas. */
    val tasks: List<BriefingTask>,
    val overdue: Int,
    /** Habitos que tocan hoy. */
    val habits: Int
)

/** El resumen: un saludo y varias frases. Lo mismo se muestra y se lee en voz alta. */
data class Briefing(
    val greeting: String,
    val lines: List<String>
) {
    val all: List<String> get() = listOf(greeting) + lines
    val spoken: String get() = all.joinToString(" ")
}

/**
 * Construye el resumen de la manana:
 * "Good morning, Julian. It's 5:15 AM... Right now it's 14 degrees...
 *  First things first: make your bed... Today you have 3 tasks..."
 *
 * Logica pura (sin Android): por eso se puede probar con tests en el PC.
 */
object BriefingBuilder {

    /** Como mucho se leen en voz alta estas tareas; del resto solo se dice cuantas son. */
    const val MAX_SPOKEN_TASKS = 5

    private val CLOSINGS = listOf(
        "Let's make it count.",
        "One step at a time. You've got this.",
        "Small wins add up. Go get the first one.",
        "Make today a day you'll be proud of.",
        "Stay focused, and enjoy the day.",
        "Discipline now, freedom later. Let's go.",
        "You don't have to be perfect, just consistent."
    )

    fun greetingWord(hour: Int): String = when (hour) {
        in 4..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    fun build(input: BriefingInput): Briefing {
        val name = input.name.trim()
        val hello = greetingWord(input.now.hour)
        val greeting = if (name.isEmpty()) "$hello." else "$hello, $name."

        val lines = mutableListOf<String>()
        val time = Formats.time(input.now.hour * 60 + input.now.minute)
        lines += "It's $time on ${Formats.fullDate(input.now.toLocalDate())}."

        weatherLine(input)?.let { lines += it }
        routineLine(input.routine)?.let { lines += it }
        lines += taskLines(input.tasks)

        if (input.overdue == 1) lines += "You also have one overdue task waiting."
        if (input.overdue > 1) lines += "You also have ${input.overdue} overdue tasks waiting."

        if (input.habits == 1) lines += "You have one habit to keep today."
        if (input.habits > 1) lines += "You have ${input.habits} habits to keep today."

        lines += CLOSINGS[input.now.dayOfYear % CLOSINGS.size]
        return Briefing(greeting, lines)
    }

    private fun weatherLine(input: BriefingInput): String? {
        val city = input.city?.takeIf { it.isNotBlank() } ?: return null
        val w = input.weather ?: return "I couldn't reach the weather service for $city this time."
        val now = spokenDegrees(w.tempC, input.fahrenheit)
        val max = spokenNumber(Formats.degrees(w.maxC, input.fahrenheit))
        val min = spokenNumber(Formats.degrees(w.minC, input.fahrenheit))
        val sb = StringBuilder("Right now it's $now and ${WeatherApi.describe(w.code)} in $city.")
        sb.append(" Today's high is $max and the low is $min")
        if (w.rainProbability != null) sb.append(", with a ${w.rainProbability} percent chance of rain")
        sb.append('.')
        WeatherApi.advice(w)?.let { sb.append(' ').append(it) }
        return sb.toString()
    }

    private fun routineLine(routine: List<String>): String? {
        val steps = routine.map { Formats.lowerFirst(Formats.stripEndPunctuation(it)) }.filter { it.isNotEmpty() }
        if (steps.isEmpty()) return null
        return "First things first: ${joinList(steps)}."
    }

    private fun taskLines(tasks: List<BriefingTask>): List<String> {
        if (tasks.isEmpty()) return listOf("You have no tasks scheduled for today.")
        fun phrase(t: BriefingTask) =
            Intention.phrase(t.title, t.minutes?.let { Formats.time(it) }, t.location)
        if (tasks.size == 1) return listOf("You have one task today: ${phrase(tasks[0])}.")

        val lines = mutableListOf("You have ${tasks.size} tasks today.")
        val spoken = tasks.take(MAX_SPOKEN_TASKS)
        spoken.forEachIndexed { i, task ->
            val isLast = i == spoken.lastIndex && tasks.size <= MAX_SPOKEN_TASKS
            val lead = when {
                i == 0 -> "First,"
                isLast && spoken.size > 2 -> "And finally,"
                else -> "Then,"
            }
            lines += "$lead ${phrase(task)}."
        }
        val rest = tasks.size - spoken.size
        if (rest > 0) lines += "Plus $rest more on your list."
        return lines
    }

    /** "14 degrees" / "minus 3 degrees" (el motor de voz lee mal el signo "-"). */
    private fun spokenDegrees(celsius: Double, fahrenheit: Boolean): String {
        val value = Formats.degrees(celsius, fahrenheit)
        val word = if (abs(value) == 1) "degree" else "degrees"
        return "${spokenNumber(value)} $word"
    }

    private fun spokenNumber(value: Int): String = if (value < 0) "minus ${abs(value)}" else value.toString()

    /** ["a"] -> "a"; ["a","b"] -> "a and b"; ["a","b","c"] -> "a, b, and c". */
    fun joinList(items: List<String>): String = when (items.size) {
        0 -> ""
        1 -> items[0]
        2 -> "${items[0]} and ${items[1]}"
        else -> items.dropLast(1).joinToString(", ") + ", and " + items.last()
    }
}
