package com.focuszone.app.logica

import java.time.Instant
import java.time.ZoneId

/**
 * Reglas del saldo diario de tiempo de pantalla.
 *
 *  - Saldo de hoy = minutos ganados hoy - minutos gastados hoy.
 *  - Se reinicia a medianoche: lo no gastado se pierde.
 *  - Se gasta en "ventanas" de 5, 10 o 15 min, descontadas por adelantado.
 *  - Sin emergencias: con saldo 0 no hay forma de desbloquear desde la app.
 *
 * Sin nada de Android: probado en ReglasSaldoTest.
 */
object ReglasSaldo {

    /** Duraciones de desbloqueo que se ofrecen, en minutos. */
    val OPCIONES_MINUTOS = listOf(5, 10, 15)

    /** Nunca negativo, aunque los datos digan otra cosa. */
    fun saldo(ganadosHoy: Int, gastadosHoy: Int): Int = (ganadosHoy - gastadosHoy).coerceAtLeast(0)

    /**
     * Que opciones de desbloqueo mostrar segun el saldo.
     * Si te quedan menos de 15 min y no coinciden con una opcion, se ofrece
     * tambien "todo lo que queda" (p. ej. con 7 min: 5 y 7).
     */
    fun opciones(saldo: Int): List<Int> {
        if (saldo <= 0) return emptyList()
        val normales = OPCIONES_MINUTOS.filter { it <= saldo }
        val todoLoQueQueda = if (saldo < OPCIONES_MINUTOS.last() && saldo !in normales) listOf(saldo) else emptyList()
        return normales + todoLoQueQueda
    }

    /** Milisegundos del inicio del dia (00:00) en que cae [ahoraMillis], en la zona horaria dada. */
    fun inicioDelDia(ahoraMillis: Long, zona: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(ahoraMillis).atZone(zona).toLocalDate()
            .atStartOfDay(zona).toInstant().toEpochMilli()
}
