package com.focuszone.app.logica

/**
 * Reglas de recompensa: cuanto tiempo de pantalla ganas por estudiar.
 *
 * Regla elegida: 1 minuto de pantalla libre por cada 5 minutos de estudio.
 */
object Recompensa {

    /** Minutos de estudio necesarios para ganar 1 minuto de pantalla. */
    const val MINUTOS_ESTUDIO_POR_CREDITO = 5

    /** Metas de sesion que puedes elegir antes de empezar, en minutos. */
    val METAS_MINUTOS = listOf(15, 25, 45, 60)

    /** Meta seleccionada por defecto (la clasica de un Pomodoro). */
    const val META_POR_DEFECTO = 25

    /** Minutos de pantalla que se ganan al completar una sesion de [metaMin] minutos. */
    fun creditosPor(metaMin: Int): Int = metaMin / MINUTOS_ESTUDIO_POR_CREDITO
}
