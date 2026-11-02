package com.focuszone.app.bloqueo

import com.focuszone.app.logica.Subventana
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * "El usuario intento abrir esta app bloqueada": aviso que el servicio de
 * accesibilidad deja aqui justo antes de abrir FocusZone, para que esta
 * muestre la pantalla de desbloqueo de esa app.
 *
 * El servicio y las pantallas viven en el mismo proceso, asi que comparten
 * este objeto directamente.
 */
object SolicitudDesbloqueo {

    /** [seccion] != null si lo bloqueado no es la app entera sino una seccion (p. ej. Reels). */
    data class Solicitud(val paquete: String, val cuandoMillis: Long, val seccion: Subventana? = null)

    /** Una solicitud mas vieja que esto se ignora (te fuiste sin decidir). */
    private const val VIGENCIA_MS = 60_000L

    private val _actual = MutableStateFlow<Solicitud?>(null)
    val actual: StateFlow<Solicitud?> = _actual.asStateFlow()

    fun pedir(paquete: String, seccion: Subventana? = null) {
        _actual.value = Solicitud(paquete, System.currentTimeMillis(), seccion)
    }

    fun limpiar() {
        _actual.value = null
    }

    fun vigente(solicitud: Solicitud?, ahoraMillis: Long = System.currentTimeMillis()): Boolean =
        solicitud != null && ahoraMillis - solicitud.cuandoMillis < VIGENCIA_MS
}
