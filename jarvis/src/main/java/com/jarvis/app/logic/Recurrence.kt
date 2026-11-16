package com.jarvis.app.logic

import java.time.LocalDate

/** Cada cuanto se repite una tarea. Se guarda por nombre ("DAILY") en la base de datos. */
enum class Repeat(val label: String) {
    NONE("Does not repeat"),
    DAILY("Every day"),
    WEEKDAYS("Weekdays (Mon–Fri)"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    companion object {
        fun of(name: String): Repeat = entries.firstOrNull { it.name == name } ?: NONE
    }
}

object Recurrence {

    /**
     * Fecha de la SIGUIENTE repeticion de una tarea que tocaba el [due] y que
     * se completa el [completedOn].
     *
     * Siempre cae despues de las dos fechas: si una tarea diaria estaba atrasada
     * tres dias y la haces hoy, la siguiente es manana (no "ayer" otra vez).
     *
     * @param weeklyMask para [Repeat.WEEKLY]: dias de la semana elegidos
     *        (ver [Days]). Si es 0, se repite el mismo dia de la semana que [due].
     * @return null si la tarea no se repite.
     */
    fun next(due: LocalDate, repeat: Repeat, weeklyMask: Int, completedOn: LocalDate): LocalDate? {
        val base = if (completedOn.isAfter(due)) completedOn else due
        return when (repeat) {
            Repeat.NONE -> null
            Repeat.DAILY -> base.plusDays(1)
            Repeat.WEEKDAYS -> firstAfter(base, Days.WEEKDAYS)
            Repeat.WEEKLY -> {
                val mask = if (weeklyMask and Days.ALL == 0) Days.bit(due.dayOfWeek) else weeklyMask
                firstAfter(base, mask)
            }
            Repeat.MONTHLY -> {
                // Se calcula siempre desde la fecha original para conservar el
                // dia del mes (31 ene -> 28 feb -> 31 mar, no 28 mar).
                var months = 1L
                var candidate = due.plusMonths(months)
                while (!candidate.isAfter(base)) {
                    months++
                    candidate = due.plusMonths(months)
                }
                candidate
            }
        }
    }

    /** Primer dia posterior a [base] que este marcado en [mask]. */
    private fun firstAfter(base: LocalDate, mask: Int): LocalDate {
        var day = base.plusDays(1)
        repeat(7) {
            if (Days.contains(mask, day.dayOfWeek)) return day
            day = day.plusDays(1)
        }
        return day
    }
}
