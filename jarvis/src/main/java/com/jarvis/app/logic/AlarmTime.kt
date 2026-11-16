package com.jarvis.app.logic

import java.time.LocalDateTime
import java.time.LocalTime

/** Cuando debe sonar la proxima alarma. Logica pura, probada con tests. */
object AlarmTime {

    /**
     * Proxima fecha y hora (estrictamente posterior a [now]) en la que toca la
     * alarma de las [hour]:[minute], contando solo los dias marcados en
     * [daysMask] (ver [Days]). Devuelve null si no hay ningun dia marcado.
     */
    fun next(now: LocalDateTime, hour: Int, minute: Int, daysMask: Int): LocalDateTime? {
        if (daysMask and Days.ALL == 0) return null
        val time = LocalTime.of(hour, minute)
        // Como mucho hay que mirar 8 dias: hoy (si ya paso la hora) + una semana.
        for (offset in 0L..7L) {
            val candidate = now.toLocalDate().plusDays(offset).atTime(time)
            if (candidate.isAfter(now) && Days.contains(daysMask, candidate.dayOfWeek)) {
                return candidate
            }
        }
        return null
    }
}
