package com.focuszone.app.logica

/**
 * Reglas del "guardian": el servicio permanente que vigila que el bloqueo de
 * apps siga funcionando (HyperOS/MIUI suele detenerlo para ahorrar bateria).
 *
 * Sin nada de Android: probado en ReglasGuardianTest.
 */
object ReglasGuardian {

    /** Cada cuanto se revisa, en milisegundos. */
    const val INTERVALO_REVISION_MS = 60_000L

    /**
     * Revisiones seguidas con el bloqueo caido antes de avisar. Evita avisos
     * falsos en el instante en que Android esta reconectando el servicio.
     */
    const val FALLOS_ANTES_DE_AVISAR = 2

    enum class Estado {
        /** No hay apps marcadas: no hay nada que proteger, el guardian se apaga. */
        SIN_APPS,

        /** Todo bien. */
        FUNCIONANDO,

        /** El interruptor de accesibilidad esta apagado (lo apago el sistema o el usuario). */
        DESACTIVADO,

        /** El interruptor esta encendido pero el servicio no esta en marcha. */
        DETENIDO
    }

    fun estado(hayAppsMarcadas: Boolean, accesibilidadActivada: Boolean, servicioFuncionando: Boolean): Estado =
        when {
            !hayAppsMarcadas -> Estado.SIN_APPS
            !accesibilidadActivada -> Estado.DESACTIVADO
            !servicioFuncionando -> Estado.DETENIDO
            else -> Estado.FUNCIONANDO
        }

    /** true si ya toca avisar al usuario, tras [fallosSeguidos] revisiones con el bloqueo caido. */
    fun debeAvisar(fallosSeguidos: Int): Boolean = fallosSeguidos >= FALLOS_ANTES_DE_AVISAR
}
