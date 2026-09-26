package com.focuszone.app.logica

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas automaticas de la maquina de estados.
 * Se ejecutan en el PC, sin celular: clic derecho sobre esta clase > Run.
 *
 * Truco: el motor recibe el tiempo desde fuera, asi que "simulamos" 25 minutos
 * avanzando un numero, sin esperar de verdad.
 */
class MotorSesionTest {

    private val paso = 250L // simulamos lecturas cada 250 ms, como el servicio real

    /** Avanza el reloj [duracionMs] enviando siempre la misma lectura. */
    private fun MotorSesion.mantener(enPosicion: Boolean, desdeMs: Long, duracionMs: Long): Long {
        var t = desdeMs
        val fin = desdeMs + duracionMs
        while (t < fin) {
            t += paso
            lectura(enPosicion, t)
        }
        return t
    }

    @Test
    fun `espera hasta estar 2 segundos boca abajo antes de arrancar`() {
        val motor = MotorSesion()
        motor.iniciar(metaMin = 25, ahoraMs = 0)
        assertEquals(EstadoSesion.Esperando(25, enPosicion = false), motor.estado)

        var t = motor.mantener(enPosicion = true, desdeMs = 0, duracionMs = 1_500)
        assertTrue("A 1.5 s aun deberia esperar", motor.estado is EstadoSesion.Esperando)

        t = motor.mantener(enPosicion = true, desdeMs = t, duracionMs = 1_000)
        assertTrue("A 2.5 s ya deberia estar en curso", motor.estado is EstadoSesion.EnCurso)
    }

    @Test
    fun `completa la sesion al llegar a la meta y da los creditos`() {
        val motor = MotorSesion()
        motor.iniciar(metaMin = 25, ahoraMs = 0)
        motor.mantener(enPosicion = true, desdeMs = 0, duracionMs = 26 * 60_000L)

        assertEquals(EstadoSesion.Completada(metaMin = 25, creditosMin = 5), motor.estado)
        assertTrue(motor.terminada)
    }

    @Test
    fun `un golpe corto a la mesa no dispara la alerta`() {
        val motor = MotorSesion()
        motor.iniciar(metaMin = 15, ahoraMs = 0)
        var t = motor.mantener(true, 0, 3_000)
        // 500 ms fuera de posicion: menos que los 700 ms de confirmacion
        t = motor.mantener(false, t, 500)
        motor.lectura(true, t + paso)
        assertTrue(motor.estado is EstadoSesion.EnCurso)
    }

    @Test
    fun `levantar el celular dispara la alerta y pausa el cronometro`() {
        val motor = MotorSesion()
        motor.iniciar(metaMin = 15, ahoraMs = 0)
        var t = motor.mantener(true, 0, 62_000) // ~1 minuto estudiando
        t = motor.mantener(false, t, 1_000)

        val alerta = motor.estado
        assertTrue("Deberia haber alerta: $alerta", alerta is EstadoSesion.Alerta)
        val segundosAlEntrar = (alerta as EstadoSesion.Alerta).estudiadoSeg

        motor.mantener(false, t, 2_000)
        val sigue = motor.estado as EstadoSesion.Alerta
        assertEquals("El cronometro no debe avanzar en alerta", segundosAlEntrar, sigue.estudiadoSeg)
    }

    @Test
    fun `volver a dejarlo dentro del margen reanuda la sesion`() {
        val motor = MotorSesion()
        motor.iniciar(metaMin = 15, ahoraMs = 0)
        var t = motor.mantener(true, 0, 10_000)
        t = motor.mantener(false, t, 2_000)
        assertTrue(motor.estado is EstadoSesion.Alerta)

        motor.mantener(true, t, 1_500)
        assertTrue("Deberia reanudar: ${motor.estado}", motor.estado is EstadoSesion.EnCurso)
    }

    @Test
    fun `no volver en 5 segundos cancela sin creditos`() {
        val motor = MotorSesion()
        motor.iniciar(metaMin = 15, ahoraMs = 0)
        var t = motor.mantener(true, 0, 10_000)
        t = motor.mantener(false, t, 1_000) // entra en alerta
        motor.mantener(false, t, 5_500)

        val estado = motor.estado
        assertTrue("Deberia cancelarse: $estado", estado is EstadoSesion.Cancelada)
        assertEquals(MotivoCancelacion.LEVANTADO, (estado as EstadoSesion.Cancelada).motivo)
    }

    @Test
    fun `la cuenta atras de la alerta empieza en 5`() {
        val motor = MotorSesion()
        motor.iniciar(metaMin = 15, ahoraMs = 0)
        var t = motor.mantener(true, 0, 5_000)
        // La 1a lectura "fuera" llega a los 250 ms; +700 ms de confirmacion
        // = la alerta salta con la lectura de los 1000 ms.
        motor.mantener(false, t, 1_000)
        val estado = motor.estado
        assertTrue("Deberia haber alerta: $estado", estado is EstadoSesion.Alerta)
        assertEquals(5, (estado as EstadoSesion.Alerta).restanteSeg)
    }

    @Test
    fun `cancelar mientras espera vuelve al inicio sin registrar nada`() {
        val motor = MotorSesion()
        motor.iniciar(metaMin = 25, ahoraMs = 0)
        motor.abandonar(ahoraMs = 1_000)
        assertEquals(EstadoSesion.Inactivo, motor.estado)
    }

    @Test
    fun `abandonar a mitad de sesion cancela sin creditos`() {
        val motor = MotorSesion()
        motor.iniciar(metaMin = 25, ahoraMs = 0)
        val t = motor.mantener(true, 0, 60_000)
        motor.abandonar(t)
        val estado = motor.estado as EstadoSesion.Cancelada
        assertEquals(MotivoCancelacion.ABANDONO, estado.motivo)
    }
}
