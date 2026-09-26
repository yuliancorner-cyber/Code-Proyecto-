package com.focuszone.app.logica

/**
 * El "cerebro" de una sesion: decide cuando empieza, cuando salta la alerta,
 * cuando se cancela y cuando se completa.
 *
 * Es una "maquina de estados": en cada momento esta en UNA fase, y solo
 * cambia de fase por reglas concretas:
 *
 *   INACTIVO --iniciar()--> ESPERANDO
 *   ESPERANDO --boca abajo y quieto 2 s--> EN_CURSO
 *   EN_CURSO --fuera de posicion 0.7 s--> ALERTA   (el cronometro se pausa)
 *   EN_CURSO --llega a la meta--> COMPLETADA        (se ganan creditos)
 *   ALERTA --de vuelta en posicion 1 s--> EN_CURSO
 *   ALERTA --pasan 5 s sin volver--> CANCELADA      (sin creditos)
 *
 * No usa nada de Android: recibe el tiempo desde fuera (parametro `ahoraMs`),
 * lo que permite probarla simulando minutos en milisegundos (ver MotorSesionTest).
 */
class MotorSesion(private val reglas: Reglas = Reglas()) {

    /**
     * Tiempos de la maquina de estados, todos en milisegundos.
     *
     * @param estabilidadInicioMs cuanto debe estar boca abajo y quieto para arrancar.
     * @param confirmacionAlertaMs cuanto debe estar fuera de posicion para dar la
     *        alerta. Evita alertas falsas por un golpe a la mesa.
     * @param estabilidadReanudarMs cuanto debe volver a estar en posicion para reanudar.
     * @param toleranciaMs margen para volver a dejarlo antes de cancelar.
     */
    data class Reglas(
        val estabilidadInicioMs: Long = 2_000L,
        val confirmacionAlertaMs: Long = 700L,
        val estabilidadReanudarMs: Long = 1_000L,
        val toleranciaMs: Long = 5_000L
    )

    private enum class Fase { INACTIVO, ESPERANDO, EN_CURSO, ALERTA, COMPLETADA, CANCELADA }

    private var fase = Fase.INACTIVO
    private var metaMin = 0
    private var acumuladoMs = 0L       // tiempo estudiado (solo cuenta en EN_CURSO)
    private var ultimoAvanceMs = 0L    // ultima vez que sumamos tiempo
    private var enPosicion = false     // ultima lectura del detector
    private var enPosicionDesdeMs = 0L // desde cuando dura la lectura actual
    private var alertaDesdeMs = 0L
    private var motivo = MotivoCancelacion.ABANDONO

    /** Estado actual, listo para mostrar en pantalla. */
    var estado: EstadoSesion = EstadoSesion.Inactivo
        private set

    /** Empieza a esperar a que dejes el celular boca abajo. */
    fun iniciar(metaMin: Int, ahoraMs: Long) {
        require(metaMin > 0) { "La meta debe ser mayor que 0" }
        this.metaMin = metaMin
        fase = Fase.ESPERANDO
        acumuladoMs = 0L
        ultimoAvanceMs = ahoraMs
        enPosicion = false
        enPosicionDesdeMs = ahoraMs
        actualizarEstado(ahoraMs)
    }

    /** Llega una lectura nueva del detector de posicion. */
    fun lectura(enPosicion: Boolean, ahoraMs: Long) {
        avanzar(ahoraMs)
        if (enPosicion != this.enPosicion) {
            this.enPosicion = enPosicion
            enPosicionDesdeMs = ahoraMs
        }
        evaluar(ahoraMs)
    }

    /** Llamar periodicamente (p. ej. cada 250 ms) para que el tiempo avance. */
    fun tick(ahoraMs: Long) {
        avanzar(ahoraMs)
        evaluar(ahoraMs)
    }

    /**
     * El usuario pulsa "Cancelar" / "Abandonar".
     * - Si aun no habia empezado (ESPERANDO): vuelve a INACTIVO sin dejar rastro.
     * - Si ya estaba estudiando: se cancela sin creditos.
     */
    fun abandonar(ahoraMs: Long) {
        avanzar(ahoraMs)
        when (fase) {
            Fase.ESPERANDO -> fase = Fase.INACTIVO
            Fase.EN_CURSO, Fase.ALERTA -> {
                motivo = MotivoCancelacion.ABANDONO
                fase = Fase.CANCELADA
            }
            else -> Unit
        }
        actualizarEstado(ahoraMs)
    }

    /** true si la sesion llego a un final (completada o cancelada). */
    val terminada: Boolean
        get() = fase == Fase.COMPLETADA || fase == Fase.CANCELADA

    // ---------------------------------------------------------------------

    /** Suma al acumulado el tiempo transcurrido, solo si estamos EN_CURSO. */
    private fun avanzar(ahoraMs: Long) {
        if (fase == Fase.EN_CURSO) {
            acumuladoMs += (ahoraMs - ultimoAvanceMs).coerceAtLeast(0L)
        }
        ultimoAvanceMs = ahoraMs
    }

    /** Aplica las reglas de transicion entre fases. */
    private fun evaluar(ahoraMs: Long) {
        val duracionLecturaActual = ahoraMs - enPosicionDesdeMs
        val metaMs = metaMin * 60_000L

        when (fase) {
            Fase.ESPERANDO -> {
                if (enPosicion && duracionLecturaActual >= reglas.estabilidadInicioMs) {
                    fase = Fase.EN_CURSO
                }
            }

            Fase.EN_CURSO -> {
                if (acumuladoMs >= metaMs) {
                    acumuladoMs = metaMs
                    fase = Fase.COMPLETADA
                } else if (!enPosicion && duracionLecturaActual >= reglas.confirmacionAlertaMs) {
                    fase = Fase.ALERTA
                    alertaDesdeMs = ahoraMs
                }
            }

            Fase.ALERTA -> {
                val margenAgotado = ahoraMs - alertaDesdeMs >= reglas.toleranciaMs
                if (enPosicion && duracionLecturaActual >= reglas.estabilidadReanudarMs) {
                    fase = Fase.EN_CURSO
                } else if (margenAgotado && !enPosicion) {
                    // Si justo lo estas volviendo a dejar (enPosicion = true pero aun
                    // sin el segundo de estabilidad) no cancelamos: te damos ese segundo.
                    motivo = MotivoCancelacion.LEVANTADO
                    fase = Fase.CANCELADA
                }
            }

            Fase.INACTIVO, Fase.COMPLETADA, Fase.CANCELADA -> Unit
        }
        actualizarEstado(ahoraMs)
    }

    /** Traduce las variables internas al [EstadoSesion] que ve la interfaz. */
    private fun actualizarEstado(ahoraMs: Long) {
        val estudiadoSeg = acumuladoMs / 1000
        estado = when (fase) {
            Fase.INACTIVO -> EstadoSesion.Inactivo
            Fase.ESPERANDO -> EstadoSesion.Esperando(metaMin, enPosicion)
            Fase.EN_CURSO -> EstadoSesion.EnCurso(metaMin, estudiadoSeg)
            Fase.ALERTA -> {
                val restanteMs = (reglas.toleranciaMs - (ahoraMs - alertaDesdeMs)).coerceAtLeast(0L)
                // Redondeo hacia arriba: con 4.2 s restantes se muestra "5".
                val restanteSeg = ((restanteMs + 999) / 1000).toInt()
                EstadoSesion.Alerta(metaMin, estudiadoSeg, restanteSeg)
            }
            Fase.COMPLETADA -> EstadoSesion.Completada(metaMin, Recompensa.creditosPor(metaMin))
            Fase.CANCELADA -> EstadoSesion.Cancelada(metaMin, estudiadoSeg, motivo)
        }
    }
}
