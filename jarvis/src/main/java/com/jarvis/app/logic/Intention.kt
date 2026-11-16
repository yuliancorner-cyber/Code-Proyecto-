package com.jarvis.app.logic

import com.jarvis.app.data.Habit
import com.jarvis.app.data.Task

/**
 * Intenciones de implementacion (James Clear, "Atomic Habits").
 *
 * La investigacion muestra que cumples mucho mas un plan cuando dices
 * exactamente CUANDO y DONDE lo haras. Las dos formulas:
 *
 *   "I will [BEHAVIOR] at [TIME] in [LOCATION]."
 *   "After [CURRENT HABIT], I will [NEW HABIT]."   (apilar habitos)
 *
 * Aqui se construyen esas frases a partir de los datos guardados.
 */
object Intention {

    /** "call the bank at 3:00 PM in the office" (sin el "I will" ni el punto). */
    fun phrase(behavior: String, timeText: String?, location: String): String {
        val what = Formats.lowerFirst(Formats.stripEndPunctuation(behavior))
        if (what.isEmpty()) return ""
        val sb = StringBuilder(what)
        if (!timeText.isNullOrBlank()) sb.append(" at ").append(timeText)
        val where = cleanPlace(location)
        if (where.isNotEmpty()) sb.append(" in ").append(where)
        return sb.toString()
    }

    /** "I will call the bank at 3:00 PM in the office." */
    fun sentence(behavior: String, timeText: String?, location: String): String {
        val phrase = phrase(behavior, timeText, location)
        return if (phrase.isEmpty()) "" else "I will $phrase."
    }

    /** "After I brush my teeth, I will read one page." */
    fun stackSentence(anchor: String, behavior: String): String {
        val what = Formats.lowerFirst(Formats.stripEndPunctuation(behavior))
        val after = cleanAnchor(anchor)
        if (what.isEmpty() || after.isEmpty()) return ""
        return "After $after, I will $what."
    }

    fun forTask(task: Task, is24Hour: Boolean = false): String =
        sentence(task.title, task.dueMinutes?.let { Formats.time(it, is24Hour) }, task.location)

    /** Frase del habito, o "" si aun no tiene plan (ni hora/lugar ni ancla). */
    fun forHabit(habit: Habit, is24Hour: Boolean = false): String {
        val behavior = habit.behavior.ifBlank { habit.name }
        return if (habit.cue == Habit.CUE_STACK) {
            stackSentence(habit.anchor, behavior)
        } else {
            if (habit.timeMinutes == null && habit.location.isBlank()) return ""
            sentence(behavior, habit.timeMinutes?.let { Formats.time(it, is24Hour) }, habit.location)
        }
    }

    /** ¿El habito ya tiene un plan concreto (cuando/donde o un ancla)? */
    fun hasPlan(habit: Habit): Boolean = forHabit(habit).isNotEmpty()

    /** "in the office" si el usuario escribio "In the office" o "at the office". */
    private fun cleanPlace(location: String): String {
        var place = Formats.stripEndPunctuation(location)
        for (prefix in listOf("in ", "at ", "on ")) {
            if (place.startsWith(prefix, ignoreCase = true)) {
                place = place.substring(prefix.length).trim()
                break
            }
        }
        return Formats.lowerFirst(place)
    }

    /** Quita un "After" inicial repetido: "After I wake up" -> "I wake up". */
    private fun cleanAnchor(anchor: String): String {
        var a = Formats.stripEndPunctuation(anchor)
        if (a.startsWith("after ", ignoreCase = true)) a = a.substring(6).trim()
        return a
    }
}
