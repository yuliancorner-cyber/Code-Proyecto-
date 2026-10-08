package com.focuszone.app.bloqueo

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * La "ventana" de desbloqueo activa: hasta que hora estan libres tus apps
 * distractoras despues de gastar saldo.
 *
 * Se guarda en SharedPreferences (lectura instantanea): el servicio de
 * accesibilidad la consulta cada vez que cambias de app y no puede esperar
 * a una consulta a la base de datos. El historial de desbloqueos, en cambio,
 * va a Room (ver GestorSaldo).
 */
object Desbloqueo {

    private const val ARCHIVO = "bloqueo"
    private const val CLAVE_HASTA = "desbloqueado_hasta_millis"

    private var flujo: MutableStateFlow<Long>? = null

    /** Hora (millis) a la que termina el desbloqueo actual; 0 si no hay. */
    fun observar(context: Context): StateFlow<Long> = obtenerFlujo(context)

    fun hasta(context: Context): Long = obtenerFlujo(context).value

    fun activo(context: Context, ahoraMillis: Long = System.currentTimeMillis()): Boolean =
        ahoraMillis < hasta(context)

    /** Abre la ventana durante [minutos] a partir de ahora. */
    fun abrir(context: Context, minutos: Int, ahoraMillis: Long = System.currentTimeMillis()) {
        guardar(context, ahoraMillis + minutos * 60_000L)
    }

    /** "Bloquear ya": cierra la ventana antes de tiempo (sin devolver minutos). */
    fun cerrar(context: Context) = guardar(context, 0L)

    private fun guardar(context: Context, hastaMillis: Long) {
        obtenerFlujo(context).value = hastaMillis
        preferencias(context).edit().putLong(CLAVE_HASTA, hastaMillis).apply()
    }

    @Synchronized
    private fun obtenerFlujo(context: Context): MutableStateFlow<Long> =
        flujo ?: MutableStateFlow(preferencias(context).getLong(CLAVE_HASTA, 0L)).also { flujo = it }

    private fun preferencias(context: Context) =
        context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
}
