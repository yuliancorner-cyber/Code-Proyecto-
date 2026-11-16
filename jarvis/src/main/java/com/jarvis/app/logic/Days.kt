package com.jarvis.app.logic

import java.time.DayOfWeek

/**
 * Dias de la semana guardados como "mascara de bits": un solo numero donde
 * cada bit es un dia (bit 0 = lunes ... bit 6 = domingo). Asi "lunes a viernes"
 * cabe en una sola columna de la base de datos (0b0011111 = 31).
 */
object Days {
    const val ALL = 0b1111111
    const val WEEKDAYS = 0b0011111
    const val WEEKEND = 0b1100000

    /** Orden en que se muestran los dias en la interfaz. */
    val ORDER: List<DayOfWeek> = DayOfWeek.values().toList()

    fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)

    fun contains(mask: Int, day: DayOfWeek): Boolean = mask and bit(day) != 0

    fun toggle(mask: Int, day: DayOfWeek): Int = mask xor bit(day)

    /** Letra corta para los botones de dia: M T W T F S S. */
    fun initial(day: DayOfWeek): String = shortName(day).take(1)

    fun shortName(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "Mon"
        DayOfWeek.TUESDAY -> "Tue"
        DayOfWeek.WEDNESDAY -> "Wed"
        DayOfWeek.THURSDAY -> "Thu"
        DayOfWeek.FRIDAY -> "Fri"
        DayOfWeek.SATURDAY -> "Sat"
        DayOfWeek.SUNDAY -> "Sun"
    }

    /** Texto legible de una mascara: "Every day", "Weekdays", "Mon, Wed, Fri"... */
    fun label(mask: Int): String = when (mask and ALL) {
        ALL -> "Every day"
        WEEKDAYS -> "Weekdays"
        WEEKEND -> "Weekends"
        0 -> "Never"
        else -> ORDER.filter { contains(mask, it) }.joinToString(", ") { shortName(it) }
    }
}
