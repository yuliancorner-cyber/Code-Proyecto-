package com.focuszone.app.logica

/**
 * Todos los estados posibles de una sesion de estudio.
 *
 * Un "sealed interface" es una lista CERRADA de variantes: el compilador sabe
 * que no existen otras, y nos obliga a manejarlas todas en cada `when`.
 * Asi es imposible olvidarse de dibujar la pantalla de algun estado.
 */
sealed interface EstadoSesion {

    /** No hay sesion: se muestra la camara con la zona y el boton "Iniciar sesion". */
    data object Inactivo : EstadoSesion

    /**
     * Ya pulsaste "Iniciar", pero el celular aun no esta boca abajo y quieto.
     * @param enPosicion true si ya esta boca abajo y quieto, contando los
     *        segundos de estabilidad antes de arrancar.
     */
    data class Esperando(val metaMin: Int, val enPosicion: Boolean) : EstadoSesion

    /** Sesion corriendo: el celular esta boca abajo y el cronometro avanza. */
    data class EnCurso(val metaMin: Int, val estudiadoSeg: Long) : EstadoSesion

    /**
     * Levantaste o moviste el celular. El cronometro esta en pausa y tienes
     * [restanteSeg] segundos para volver a dejarlo boca abajo.
     */
    data class Alerta(val metaMin: Int, val estudiadoSeg: Long, val restanteSeg: Int) : EstadoSesion

    /** Llegaste a la meta: ganaste [creditosMin] minutos de pantalla. */
    data class Completada(val metaMin: Int, val creditosMin: Int) : EstadoSesion

    /** La sesion termino antes de la meta y no se gano nada. */
    data class Cancelada(
        val metaMin: Int,
        val estudiadoSeg: Long,
        val motivo: MotivoCancelacion
    ) : EstadoSesion
}

enum class MotivoCancelacion {
    /** No volviste a dejar el celular boca abajo dentro del margen de tolerancia. */
    LEVANTADO,

    /** Pulsaste "Abandonar sesion". */
    ABANDONO
}
