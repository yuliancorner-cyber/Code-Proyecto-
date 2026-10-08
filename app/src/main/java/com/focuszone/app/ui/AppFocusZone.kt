package com.focuszone.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focuszone.app.bloqueo.SolicitudDesbloqueo
import com.focuszone.app.logica.EstadoSesion
import com.focuszone.app.sesion.SesionActual
import com.focuszone.app.ui.componentes.recordarSaldoHoy

/**
 * Raiz de la interfaz: elige que pantalla mostrar segun el estado de la sesion.
 *
 * No hay "navegacion" con botones de atras: la pantalla la decide el estado.
 * Asi, si cierras la app a mitad de sesion y la vuelves a abrir, apareces
 * directamente en la pantalla correcta.
 */
@Composable
fun AppFocusZone() {
    val context = LocalContext.current

    // Estado publicado por el servicio. "WithLifecycle" = deja de escuchar
    // cuando la app no esta visible (ahorra bateria).
    val estado by SesionActual.estado.collectAsStateWithLifecycle()

    // Saldo de hoy (ganado - gastado), se actualiza solo.
    val saldoHoy = recordarSaldoHoy()

    // ¿El servicio de bloqueo pidio mostrar "¿Desbloquear X min?" para alguna app?
    val solicitud by SolicitudDesbloqueo.actual.collectAsStateWithLifecycle()

    // Pantalla secundaria abierta desde el inicio (si hay alguna).
    var extra by rememberSaveable { mutableStateOf(PantallaExtra.NINGUNA) }

    when (val actual = estado) {
        EstadoSesion.Inactivo, is EstadoSesion.Esperando ->
            if (actual == EstadoSesion.Inactivo && SolicitudDesbloqueo.vigente(solicitud)) {
                PantallaDesbloqueo(paquete = solicitud!!.paquete, saldoHoy = saldoHoy)
            } else if (extra == PantallaExtra.BLOQUEO && actual == EstadoSesion.Inactivo) {
                PantallaBloqueo(onVolver = { extra = PantallaExtra.NINGUNA })
            } else if (extra == PantallaExtra.ESTADISTICAS && actual == EstadoSesion.Inactivo) {
                PantallaEstadisticas(onVolver = { extra = PantallaExtra.NINGUNA })
            } else {
                // Inactivo y Esperando comparten pantalla (misma camara, sin parpadeo).
                PantallaInicio(
                    saldoHoy = saldoHoy,
                    esperando = actual as? EstadoSesion.Esperando,
                    onIniciar = { meta -> SesionActual.iniciar(context, meta) },
                    onCancelar = { SesionActual.abandonar(context) },
                    onAbrirBloqueo = { extra = PantallaExtra.BLOQUEO },
                    onAbrirEstadisticas = { extra = PantallaExtra.ESTADISTICAS }
                )
            }

        is EstadoSesion.EnCurso -> PantallaSesion(
            estado = actual,
            onAbandonar = { SesionActual.abandonar(context) }
        )

        is EstadoSesion.Alerta -> PantallaAlerta(
            estado = actual,
            onAbandonar = { SesionActual.abandonar(context) }
        )

        is EstadoSesion.Completada, is EstadoSesion.Cancelada -> PantallaResultado(
            estado = actual,
            saldoHoy = saldoHoy,
            onVolver = { SesionActual.volverAlInicio() }
        )
    }
}

/** Pantallas secundarias a las que se llega desde el inicio. */
private enum class PantallaExtra { NINGUNA, BLOQUEO, ESTADISTICAS }
