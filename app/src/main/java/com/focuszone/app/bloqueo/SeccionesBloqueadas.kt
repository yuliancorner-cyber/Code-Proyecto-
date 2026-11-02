package com.focuszone.app.bloqueo

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import com.focuszone.app.R
import com.focuszone.app.logica.Subventana
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Las secciones de apps que marcaste para bloquear (Novedades de WhatsApp,
 * Historias y Reels de Instagram). Igual que AppsBloqueadas: guardadas en
 * SharedPreferences y en memoria como StateFlow.
 */
object SeccionesBloqueadas {

    private const val ARCHIVO = "bloqueo"
    private const val CLAVE_SECCIONES = "secciones_bloqueadas"

    private var flujo: MutableStateFlow<Set<Subventana>>? = null

    fun observar(context: Context): StateFlow<Set<Subventana>> = obtenerFlujo(context)

    fun actuales(context: Context): Set<Subventana> = obtenerFlujo(context).value

    /** Nombre corto para avisos y titulos: "Reels de Instagram". */
    @StringRes
    fun nombre(seccion: Subventana): Int = when (seccion) {
        Subventana.WHATSAPP_NOVEDADES -> R.string.seccion_whatsapp_novedades
        Subventana.INSTAGRAM_HISTORIAS -> R.string.seccion_instagram_historias
        Subventana.INSTAGRAM_REELS -> R.string.seccion_instagram_reels
    }

    /** Que incluye, para la lista de la configuracion. */
    @StringRes
    fun descripcion(seccion: Subventana): Int = when (seccion) {
        Subventana.WHATSAPP_NOVEDADES -> R.string.seccion_whatsapp_novedades_desc
        Subventana.INSTAGRAM_HISTORIAS -> R.string.seccion_instagram_historias_desc
        Subventana.INSTAGRAM_REELS -> R.string.seccion_instagram_reels_desc
    }

    /** Marca la seccion si no lo estaba, o la desmarca si ya lo estaba. */
    fun alternar(context: Context, seccion: Subventana) {
        val f = obtenerFlujo(context)
        val nuevo = if (seccion in f.value) f.value - seccion else f.value + seccion
        f.value = nuevo
        preferencias(context).edit().putStringSet(CLAVE_SECCIONES, nuevo.map { it.name }.toSet()).apply()
        ServicioGuardian.actualizar(context)
    }

    @Synchronized
    private fun obtenerFlujo(context: Context): MutableStateFlow<Set<Subventana>> =
        flujo ?: MutableStateFlow(leer(context)).also { flujo = it }

    /** Ignora nombres que ya no existan (por si un dia cambiamos la lista). */
    private fun leer(context: Context): Set<Subventana> {
        val guardadas = preferencias(context).getStringSet(CLAVE_SECCIONES, null) ?: return emptySet()
        return Subventana.entries.filter { it.name in guardadas }.toSet()
    }

    private fun preferencias(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
}
