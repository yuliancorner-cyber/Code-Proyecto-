package com.focuszone.app.bloqueo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Las ultimas "miradas" del servicio a WhatsApp e Instagram, para poder
 * ajustar la deteccion de secciones si esas apps cambian por dentro.
 *
 * Solo guarda nombres internos (pantalla, ids, pestaña activa), nunca
 * mensajes. Vive en memoria: se borra al cerrar FocusZone.
 */
object Diagnostico {

    private const val MAXIMO = 8

    private val _lineas = MutableStateFlow<List<String>>(emptyList())
    val lineas: StateFlow<List<String>> = _lineas.asStateFlow()

    /** Agrega una linea arriba (si no es igual a la anterior). */
    fun anotar(linea: String) {
        val actuales = _lineas.value
        if (actuales.firstOrNull() == linea) return
        _lineas.value = (listOf(linea) + actuales).take(MAXIMO)
    }
}
