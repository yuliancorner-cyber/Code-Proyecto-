package com.focuszone.app.sesion

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.focuszone.app.logica.EstadoSesion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Punto de encuentro entre el servicio (que vigila los sensores) y las
 * pantallas (que muestran el estado).
 *
 * - El servicio PUBLICA aqui cada cambio de estado.
 * - Las pantallas OBSERVAN [estado] y se redibujan solas cuando cambia.
 * - Las pantallas envian ORDENES (iniciar, abandonar) al servicio con los
 *   metodos de abajo.
 *
 * Un "StateFlow" es una caja que siempre tiene un valor y avisa a quien la
 * observe cada vez que ese valor cambia.
 */
object SesionActual {

    private val _estado = MutableStateFlow<EstadoSesion>(EstadoSesion.Inactivo)
    val estado: StateFlow<EstadoSesion> = _estado.asStateFlow()

    /** Solo lo llama el servicio. */
    internal fun publicar(nuevo: EstadoSesion) {
        _estado.value = nuevo
    }

    /** Arranca el servicio y empieza a esperar a que dejes el celular boca abajo. */
    fun iniciar(context: Context, metaMin: Int) {
        val intent = Intent(context, ServicioSesion::class.java)
            .setAction(ServicioSesion.ACCION_INICIAR)
            .putExtra(ServicioSesion.EXTRA_META_MIN, metaMin)
        // "Foreground service" = servicio con notificacion visible, que Android
        // no mata aunque la app no este en pantalla.
        ContextCompat.startForegroundService(context, intent)
    }

    /** Cancela la espera o abandona la sesion en curso. */
    fun abandonar(context: Context) {
        val intent = Intent(context, ServicioSesion::class.java)
            .setAction(ServicioSesion.ACCION_ABANDONAR)
        context.startService(intent)
    }

    /** Tras ver el resultado (completada o cancelada), vuelve a la pantalla inicial. */
    fun volverAlInicio() {
        val actual = _estado.value
        if (actual is EstadoSesion.Completada || actual is EstadoSesion.Cancelada) {
            _estado.value = EstadoSesion.Inactivo
        }
    }
}
