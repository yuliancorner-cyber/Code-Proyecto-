package com.focuszone.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focuszone.app.datos.BaseDatos
import com.focuszone.app.logica.EstadoSesion
import com.focuszone.app.sesion.SesionActual

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

    // Total de creditos desde la base de datos; se actualiza solo al guardar
    // una sesion nueva. remember{} evita crear una consulta nueva en cada redibujo.
    val dao = remember { BaseDatos.obtener(context).sesionDao() }
    val totalCreditos by remember { dao.totalCreditos() }.collectAsStateWithLifecycle(initialValue = 0)

    // true mientras se muestra la pantalla de configuracion del bloqueo.
    var mostrandoBloqueo by rememberSaveable { mutableStateOf(false) }

    when (val actual = estado) {
        EstadoSesion.Inactivo, is EstadoSesion.Esperando ->
            if (mostrandoBloqueo && actual == EstadoSesion.Inactivo) {
                PantallaBloqueo(onVolver = { mostrandoBloqueo = false })
            } else {
                // Inactivo y Esperando comparten pantalla (misma camara, sin parpadeo).
                PantallaInicio(
                    totalCreditosMin = totalCreditos,
                    esperando = actual as? EstadoSesion.Esperando,
                    onIniciar = { meta -> SesionActual.iniciar(context, meta) },
                    onCancelar = { SesionActual.abandonar(context) },
                    onAbrirBloqueo = { mostrandoBloqueo = true }
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
            totalCreditosMin = totalCreditos,
            onVolver = { SesionActual.volverAlInicio() }
        )
    }
}
