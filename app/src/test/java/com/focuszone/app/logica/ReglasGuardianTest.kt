package com.focuszone.app.logica

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasGuardianTest {

    @Test
    fun `sin apps marcadas no hay nada que vigilar`() {
        assertEquals(ReglasGuardian.Estado.SIN_APPS, ReglasGuardian.estado(false, true, true))
        assertEquals(ReglasGuardian.Estado.SIN_APPS, ReglasGuardian.estado(false, false, false))
    }

    @Test
    fun `con apps y servicio en marcha todo funciona`() {
        assertEquals(ReglasGuardian.Estado.FUNCIONANDO, ReglasGuardian.estado(true, true, true))
    }

    @Test
    fun `interruptor apagado es desactivado aunque el servicio parezca vivo`() {
        assertEquals(ReglasGuardian.Estado.DESACTIVADO, ReglasGuardian.estado(true, false, true))
        assertEquals(ReglasGuardian.Estado.DESACTIVADO, ReglasGuardian.estado(true, false, false))
    }

    @Test
    fun `interruptor encendido pero servicio parado es detenido`() {
        assertEquals(ReglasGuardian.Estado.DETENIDO, ReglasGuardian.estado(true, true, false))
    }

    @Test
    fun `solo avisa tras dos revisiones seguidas con el bloqueo caido`() {
        assertFalse(ReglasGuardian.debeAvisar(0))
        assertFalse(ReglasGuardian.debeAvisar(1))
        assertTrue(ReglasGuardian.debeAvisar(2))
        assertTrue(ReglasGuardian.debeAvisar(5))
    }
}
