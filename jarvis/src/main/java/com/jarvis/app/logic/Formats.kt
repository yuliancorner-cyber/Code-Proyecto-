package com.jarvis.app.logic

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Como se escriben horas y fechas en la app. Todo en ingles (Locale.US),
 * sin depender del idioma del celular.
 *
 * Las horas se guardan como "minutos desde medianoche" (5:15 AM = 315) y las
 * fechas como "epochDay" (dias desde 1970): numeros simples, faciles de
 * guardar y comparar.
 */
object Formats {
    private val DAY_MONTH = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
    private val FULL_DATE = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US)
    private val MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)

    /** 315 -> "5:15 AM" (o "05:15" en formato 24 h). */
    fun time(minutes: Int, is24Hour: Boolean = false): String {
        val h = (minutes / 60) % 24
        val m = minutes % 60
        if (is24Hour) return "%02d:%02d".format(Locale.US, h, m)
        val h12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        val suffix = if (h < 12) "AM" else "PM"
        return "%d:%02d %s".format(Locale.US, h12, m, suffix)
    }

    /** "Today", "Tomorrow", "Yesterday" o "Mon, Nov 16". */
    fun relativeDate(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> DAY_MONTH.format(date)
    }

    /** "Monday, November 16". */
    fun fullDate(date: LocalDate): String = FULL_DATE.format(date)

    /** "November 2026". */
    fun monthYear(date: LocalDate): String = MONTH_YEAR.format(date)

    /** Temperatura redondeada, convirtiendo a Fahrenheit si hace falta. */
    fun degrees(celsius: Double, fahrenheit: Boolean): Int {
        val value = if (fahrenheit) celsius * 9.0 / 5.0 + 32.0 else celsius
        return Math.round(value).toInt()
    }

    /** "call the bank" a partir de "Call the bank"; respeta siglas como "NASA". */
    fun lowerFirst(text: String): String {
        val t = text.trim()
        if (t.isEmpty()) return t
        val firstWord = t.takeWhile { !it.isWhitespace() }
        val isAcronym = firstWord.length > 1 && firstWord.all { !it.isLetter() || it.isUpperCase() }
        // "I" suelto (p. ej. "I need...") tampoco se toca.
        if (isAcronym || firstWord == "I") return t
        return t.replaceFirstChar { it.lowercase(Locale.US) }
    }

    /** Quita el punto final: las frases se unen despues con comas. */
    fun stripEndPunctuation(text: String): String = text.trim().trimEnd('.', '!', ',', ';', ':').trim()
}
