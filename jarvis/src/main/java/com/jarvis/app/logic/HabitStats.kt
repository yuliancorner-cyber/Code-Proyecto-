package com.jarvis.app.logic

import java.time.LocalDate
import java.time.YearMonth

/**
 * Numeros de los habitos: rachas y porcentajes del mes (los mismos que
 * calculaba tu hoja de calculo con COUNTIF). Logica pura, probada con tests.
 *
 * En todas las funciones, [doneDays] es el conjunto de dias (epochDay) en los
 * que el habito se dio por HECHO, y [mask] los dias de la semana en que toca.
 */
object HabitStats {

    /** Un habito con meta diaria (p. ej. 8 vasos) esta hecho al llegar a la meta. */
    fun isDone(count: Int, target: Int): Boolean = count >= maxOf(1, target)

    private fun scheduled(mask: Int, day: LocalDate): Boolean =
        // Sin dias marcados lo tratamos como diario, para no dividir entre cero.
        mask and Days.ALL == 0 || Days.contains(mask, day.dayOfWeek)

    /**
     * Racha actual: dias seguidos (de los que tocaba) cumplidos hasta hoy.
     * Si hoy aun no lo hiciste, la racha no se rompe: cuenta hasta ayer.
     * Los dias en que no tocaba no rompen la racha.
     */
    fun currentStreak(mask: Int, doneDays: Set<Long>, today: LocalDate): Int {
        if (doneDays.isEmpty()) return 0
        val first = doneDays.min()
        var day = if (today.toEpochDay() in doneDays) today else today.minusDays(1)
        var streak = 0
        while (day.toEpochDay() >= first) {
            val done = day.toEpochDay() in doneDays
            if (done) streak++ else if (scheduled(mask, day)) break
            day = day.minusDays(1)
        }
        return streak
    }

    /** La racha mas larga de toda la historia. */
    fun bestStreak(mask: Int, doneDays: Set<Long>, today: LocalDate): Int {
        if (doneDays.isEmpty()) return 0
        var best = 0
        var run = 0
        var day = LocalDate.ofEpochDay(doneDays.min())
        while (!day.isAfter(today)) {
            val done = day.toEpochDay() in doneDays
            if (done) {
                run++
                if (run > best) best = run
            } else if (scheduled(mask, day)) {
                run = 0
            }
            day = day.plusDays(1)
        }
        return best
    }

    /**
     * Regla "never miss twice" (James Clear): fallar un dia es un accidente,
     * fallar dos es el inicio de un habito nuevo. Devuelve true si el ultimo
     * dia que tocaba (antes de hoy) NO se hizo y hoy todavia tampoco.
     * Un habito sin historial previo no avisa.
     */
    fun missedLastTime(mask: Int, doneDays: Set<Long>, today: LocalDate): Boolean {
        if (today.toEpochDay() in doneDays) return false
        if (!scheduled(mask, today)) return false
        var day = today.minusDays(1)
        repeat(7) {
            if (scheduled(mask, day)) {
                val missed = day.toEpochDay() !in doneDays
                val hasHistory = doneDays.any { it < day.toEpochDay() }
                return missed && hasHistory
            }
            day = day.minusDays(1)
        }
        return false
    }

    /** Resumen de un habito en un mes. */
    data class MonthSummary(
        /** Dias hechos en el mes (la columna "Count" de la hoja). */
        val done: Int,
        /** Dias que tocaba, contados solo hasta hoy si es el mes actual. */
        val expected: Int
    ) {
        /** Porcentaje 0..100 (la columna "Complete"). */
        val percent: Int get() = if (expected == 0) 0 else minOf(100, done * 100 / expected)
    }

    fun month(mask: Int, doneDays: Set<Long>, month: YearMonth, today: LocalDate): MonthSummary {
        var done = 0
        var expected = 0
        for (d in 1..month.lengthOfMonth()) {
            val day = month.atDay(d)
            if (day.isAfter(today)) break
            val isDone = day.toEpochDay() in doneDays
            if (isDone) done++
            // Un dia hecho cuenta aunque "no tocara", para que nunca pase de 100 %.
            if (scheduled(mask, day) || isDone) expected++
        }
        return MonthSummary(done, expected)
    }

    /**
     * Porcentaje del DIA: habitos hechos entre los que tocaban (la fila
     * "%Complete" de la hoja). [habits] = pares (mascara de dias, hecho ese dia).
     */
    fun dayPercent(habits: List<Pair<Int, Boolean>>, day: LocalDate): Int {
        var done = 0
        var expected = 0
        for ((mask, isDone) in habits) {
            if (isDone) done++
            if (scheduled(mask, day) || isDone) expected++
        }
        return if (expected == 0) 0 else done * 100 / expected
    }

    /** ¿Toca este habito en [day]? */
    fun isScheduled(mask: Int, day: LocalDate): Boolean = scheduled(mask, day)
}

/** Cuando toca el proximo recordatorio diario de un habito. */
object HabitReminders {

    /**
     * Proximo aviso a las [reminderMinutes] en un dia que toque ([mask]).
     * Hoy solo cuenta si la hora aun no paso y el habito no esta ya hecho.
     */
    fun next(
        mask: Int,
        reminderMinutes: Int,
        doneToday: Boolean,
        now: java.time.LocalDateTime
    ): java.time.LocalDateTime? {
        val time = java.time.LocalTime.of((reminderMinutes / 60) % 24, reminderMinutes % 60)
        for (offset in 0L..7L) {
            val day = now.toLocalDate().plusDays(offset)
            if (!HabitStats.isScheduled(mask, day)) continue
            val candidate = day.atTime(time)
            if (offset == 0L && (doneToday || !candidate.isAfter(now))) continue
            return candidate
        }
        return null
    }
}
