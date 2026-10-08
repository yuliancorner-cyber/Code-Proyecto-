package com.focuszone.app.ui.componentes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focuszone.app.bloqueo.Desbloqueo
import com.focuszone.app.bloqueo.GestorSaldo
import com.focuszone.app.logica.ReglasSaldo
import kotlinx.coroutines.delay

/**
 * Saldo de hoy en minutos, siempre al dia.
 *
 * Al volver a la app (ON_RESUME) se recalcula el inicio del dia: asi, si la
 * dejaste abierta de un dia para otro, el saldo se reinicia correctamente.
 */
@Composable
fun recordarSaldoHoy(): Int {
    val context = LocalContext.current
    var inicioDelDia by remember { mutableLongStateOf(ReglasSaldo.inicioDelDia(System.currentTimeMillis())) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        inicioDelDia = ReglasSaldo.inicioDelDia(System.currentTimeMillis())
    }
    val flujo = remember(inicioDelDia) { GestorSaldo.saldoDesde(context, inicioDelDia) }
    val saldo by flujo.collectAsStateWithLifecycle(initialValue = 0)
    return saldo
}

/** Segundos que quedan de la ventana de desbloqueo actual (0 si no hay). Cuenta hacia atras. */
@Composable
fun recordarSegundosDesbloqueo(): Long {
    val context = LocalContext.current
    val hasta by remember { Desbloqueo.observar(context) }.collectAsStateWithLifecycle()
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Mientras la ventana siga abierta, actualiza "ahora" cada segundo.
    LaunchedEffect(hasta) {
        ahora = System.currentTimeMillis()
        while (ahora < hasta) {
            delay(1_000L)
            ahora = System.currentTimeMillis()
        }
    }
    return ((hasta - ahora).coerceAtLeast(0L) + 999L) / 1000L
}
