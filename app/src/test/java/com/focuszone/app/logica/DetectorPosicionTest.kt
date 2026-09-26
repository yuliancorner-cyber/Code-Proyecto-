package com.focuszone.app.logica

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectorPosicionTest {

    private val g = 9.81f

    /** Envia la misma lectura durante [ms] milisegundos, cada 60 ms (ritmo real del sensor). */
    private fun DetectorPosicion.repetir(x: Float, y: Float, z: Float, desde: Long, ms: Long): Pair<Long, Boolean> {
        var t = desde
        var resultado = false
        while (t < desde + ms) {
            t += 60
            resultado = procesar(x, y, z, t)
        }
        return t to resultado
    }

    @Test
    fun `boca abajo y quieto se detecta`() {
        val d = DetectorPosicion()
        val (_, enPosicion) = d.repetir(0f, 0f, -g, 0, 2_000)
        assertTrue(enPosicion)
    }

    @Test
    fun `boca arriba no cuenta`() {
        val d = DetectorPosicion()
        val (_, enPosicion) = d.repetir(0f, 0f, g, 0, 2_000)
        assertFalse(enPosicion)
    }

    @Test
    fun `de pie en la mano no cuenta`() {
        val d = DetectorPosicion()
        val (_, enPosicion) = d.repetir(0f, g, 0f, 0, 2_000)
        assertFalse(enPosicion)
    }

    @Test
    fun `una mesa algo inclinada si cuenta`() {
        // Unos 20 grados de inclinacion
        val d = DetectorPosicion()
        val (_, enPosicion) = d.repetir(0f, 3.35f, -9.22f, 0, 2_000)
        assertTrue(enPosicion)
    }

    @Test
    fun `una sacudida lo marca como en movimiento`() {
        val d = DetectorPosicion()
        var (t, _) = d.repetir(0f, 0f, -g, 0, 2_000)
        t += 60
        val durante = d.procesar(4f, 3f, -g + 5f, t) // tiron brusco
        assertFalse(durante)

        // Tras medio segundo quieto vuelve a estar en posicion
        val (_, despues) = d.repetir(0f, 0f, -g, t, 1_500)
        assertTrue(despues)
    }

    @Test
    fun `vibraciones leves de la mesa se ignoran`() {
        val d = DetectorPosicion()
        var t = 0L
        var enPosicion = false
        repeat(40) { i ->
            t += 60
            val ruido = if (i % 2 == 0) 0.3f else -0.3f
            enPosicion = d.procesar(ruido, ruido, -g + ruido, t)
        }
        assertTrue(enPosicion)
    }
}
