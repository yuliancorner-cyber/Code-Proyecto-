package com.focuszone.app.logica

import org.junit.Assert.assertEquals
import org.junit.Test

class RecompensaTest {
    @Test
    fun `un minuto de pantalla por cada cinco de estudio`() {
        assertEquals(3, Recompensa.creditosPor(15))
        assertEquals(5, Recompensa.creditosPor(25))
        assertEquals(9, Recompensa.creditosPor(45))
        assertEquals(12, Recompensa.creditosPor(60))
    }
}
