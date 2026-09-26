package com.focuszone.app.logica

import kotlin.math.sqrt

/**
 * Interpreta las lecturas del ACELEROMETRO para saber si el celular esta
 * "boca abajo y quieto".
 *
 * Como funciona el acelerometro: mide la aceleracion en 3 ejes (x, y, z) en m/s².
 * Con el celular quieto, lo unico que mide es la gravedad (unos 9.8 m/s²),
 * y el eje hacia donde "cae" nos dice la orientacion:
 *  - Pantalla hacia arriba, sobre la mesa: z ≈ +9.8
 *  - Pantalla hacia abajo (boca abajo):    z ≈ -9.8
 *
 * Para separar la gravedad (lenta) de los movimientos (rapidos) usamos un
 * "filtro paso bajo": un promedio que se actualiza poco a poco. Lo que se
 * aleja de ese promedio es movimiento.
 *
 * Esta clase no usa nada de Android, asi que se puede probar sin celular
 * (ver DetectorPosicionTest).
 *
 * @param umbralBocaAbajo cuanto de la gravedad debe caer en -z. 8.0 de 9.8
 *        permite una inclinacion de unos 35° (escritorio no del todo plano).
 * @param umbralMovimiento aceleracion (m/s²) a partir de la cual se considera
 *        que el celular se esta moviendo. Escribir o apoyar el codo en la mesa
 *        produce vibraciones menores a esto.
 * @param ventanaQuietoMs cuanto tiempo sin movimiento hace falta para volver a
 *        considerarlo "quieto".
 * @param suavizado peso del valor anterior en el filtro (0..1). Mas alto =
 *        gravedad mas estable pero que reacciona mas lento.
 */
class DetectorPosicion(
    private val umbralBocaAbajo: Float = 8.0f,
    private val umbralMovimiento: Float = 1.5f,
    private val ventanaQuietoMs: Long = 500L,
    private val suavizado: Float = 0.8f
) {
    // Gravedad estimada en cada eje.
    private var gx = 0f
    private var gy = 0f
    private var gz = 0f
    private var inicializado = false

    // Momento del ultimo movimiento fuerte. Empieza "en el pasado lejano".
    private var ultimoMovimientoMs = Long.MIN_VALUE / 2

    /**
     * Procesa una lectura del acelerometro.
     *
     * @param ahoraMs reloj en milisegundos (monotono: no retrocede nunca).
     * @return true si el celular esta boca abajo Y quieto.
     */
    fun procesar(x: Float, y: Float, z: Float, ahoraMs: Long): Boolean {
        if (!inicializado) {
            gx = x; gy = y; gz = z
            inicializado = true
        } else {
            gx = suavizado * gx + (1 - suavizado) * x
            gy = suavizado * gy + (1 - suavizado) * y
            gz = suavizado * gz + (1 - suavizado) * z
        }

        // Aceleracion "sobrante" una vez quitada la gravedad = movimiento.
        val mx = x - gx
        val my = y - gy
        val mz = z - gz
        val movimiento = sqrt(mx * mx + my * my + mz * mz)
        if (movimiento > umbralMovimiento) {
            ultimoMovimientoMs = ahoraMs
        }

        val bocaAbajo = gz < -umbralBocaAbajo
        val quieto = ahoraMs - ultimoMovimientoMs > ventanaQuietoMs
        return bocaAbajo && quieto
    }

    /** Olvida todo lo anterior (al empezar una sesion nueva). */
    fun reiniciar() {
        inicializado = false
        ultimoMovimientoMs = Long.MIN_VALUE / 2
    }
}
