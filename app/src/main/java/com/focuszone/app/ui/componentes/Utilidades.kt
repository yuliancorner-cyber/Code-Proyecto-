package com.focuszone.app.ui.componentes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import com.focuszone.app.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Mientras esta funcion este en pantalla, el celular no apaga la pantalla solo.
 * Asi, al levantar el celular durante la sesion, ves la alerta al instante.
 */
@Composable
fun MantenerPantallaEncendida() {
    val vista = LocalView.current
    DisposableEffect(vista) {
        vista.keepScreenOn = true
        onDispose { vista.keepScreenOn = false }
    }
}

/** Convierte segundos a "mm:ss" (p. ej. 754 -> "12:34"). */
fun formatearTiempo(segundos: Long): String {
    val min = (segundos / 60).toString().padStart(2, '0')
    val seg = (segundos % 60).toString().padStart(2, '0')
    return "$min:$seg"
}

/** Minutos a texto legible: 45 -> "45 min", 750 -> "12 h 30 min". */
@Composable
fun formatearDuracion(minutos: Int): String =
    if (minutos < 60) {
        stringResource(R.string.minutos_corto, minutos)
    } else {
        stringResource(R.string.duracion_horas, minutos / 60, minutos % 60)
    }

/** Fecha y hora relativas: "Hoy, 14:30", "Ayer, 09:05" o "6 oct, 18:00". */
@Composable
fun fechaRelativa(millis: Long): String {
    val zona = ZoneId.systemDefault()
    val momento = Instant.ofEpochMilli(millis).atZone(zona)
    val hoy = LocalDate.now(zona)
    val hora = momento.format(DateTimeFormatter.ofPattern("HH:mm"))
    return when (momento.toLocalDate()) {
        hoy -> stringResource(R.string.fecha_hoy, hora)
        hoy.minusDays(1) -> stringResource(R.string.fecha_ayer, hora)
        else -> stringResource(
            R.string.fecha_otro,
            momento.format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es"))),
            hora
        )
    }
}
