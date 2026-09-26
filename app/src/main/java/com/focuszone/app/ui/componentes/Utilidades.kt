package com.focuszone.app.ui.componentes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

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
