package com.focuszone.app.bloqueo

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * La lista de apps que marcaste como "distractoras".
 *
 * Se guarda en SharedPreferences: un pequeno archivo de ajustes clave-valor,
 * ideal para configuraciones simples como esta (para datos con historia,
 * como las sesiones, usamos Room).
 *
 * Tambien se mantiene en memoria como StateFlow, para que:
 *  - el servicio de bloqueo consulte la lista al instante (cada vez que cambias de app),
 *  - la pantalla de ajustes se redibuje sola al marcar o desmarcar una app.
 */
object AppsBloqueadas {

    private const val ARCHIVO = "bloqueo"
    private const val CLAVE_APPS = "apps_bloqueadas"

    private var flujo: MutableStateFlow<Set<String>>? = null

    /** Lista observable de paquetes bloqueados (p. ej. "com.instagram.android"). */
    fun observar(context: Context): StateFlow<Set<String>> = obtenerFlujo(context)

    fun contiene(context: Context, paquete: String): Boolean =
        paquete in obtenerFlujo(context).value

    /** Marca la app si no lo estaba, o la desmarca si ya lo estaba. */
    fun alternar(context: Context, paquete: String) {
        val f = obtenerFlujo(context)
        val nuevo = if (paquete in f.value) f.value - paquete else f.value + paquete
        f.value = nuevo
        preferencias(context).edit().putStringSet(CLAVE_APPS, nuevo).apply()
    }

    @Synchronized
    private fun obtenerFlujo(context: Context): MutableStateFlow<Set<String>> =
        flujo ?: MutableStateFlow(
            // toSet(): Android prohibe modificar el conjunto que devuelve getStringSet.
            preferencias(context).getStringSet(CLAVE_APPS, null)?.toSet() ?: emptySet()
        ).also { flujo = it }

    private fun preferencias(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
}
