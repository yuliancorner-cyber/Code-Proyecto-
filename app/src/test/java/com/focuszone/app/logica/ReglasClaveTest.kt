package com.focuszone.app.logica

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasClaveTest {

    @Test
    fun `solo valen de 4 a 8 digitos`() {
        assertTrue(ReglasClave.esValida("1234"))
        assertTrue(ReglasClave.esValida("12345678"))
        assertFalse(ReglasClave.esValida("123"))
        assertFalse(ReglasClave.esValida("123456789"))
        assertFalse(ReglasClave.esValida("12a4"))
        assertFalse(ReglasClave.esValida(""))
    }

    @Test
    fun `la clave correcta coincide y una incorrecta no`() {
        val guardada = ReglasClave.proteger("2580")
        assertTrue(ReglasClave.coincide("2580", guardada))
        assertFalse(ReglasClave.coincide("2581", guardada))
        assertFalse(ReglasClave.coincide("25800", guardada))
    }

    @Test
    fun `lo guardado no contiene la clave`() {
        val guardada = ReglasClave.proteger("13579")
        assertFalse(guardada.contains("13579"))
    }

    @Test
    fun `la misma clave guardada dos veces da textos distintos por la sal`() {
        assertNotEquals(ReglasClave.proteger("1111"), ReglasClave.proteger("1111"))
    }

    @Test
    fun `un texto guardado danado nunca coincide`() {
        assertFalse(ReglasClave.coincide("1234", ""))
        assertFalse(ReglasClave.coincide("1234", "pbkdf2:abc:%%%:%%%"))
        assertFalse(ReglasClave.coincide("1234", "1234"))
    }

    @Test
    fun `hay que esperar cada 5 fallos y la espera crece hasta 5 minutos`() {
        assertEquals(0L, ReglasClave.esperaTrasFallos(1))
        assertEquals(0L, ReglasClave.esperaTrasFallos(4))
        assertEquals(30_000L, ReglasClave.esperaTrasFallos(5))
        assertEquals(0L, ReglasClave.esperaTrasFallos(6))
        assertEquals(60_000L, ReglasClave.esperaTrasFallos(10))
        assertEquals(300_000L, ReglasClave.esperaTrasFallos(100))
    }

    @Test
    fun `intentos restantes antes de la espera`() {
        assertEquals(4, ReglasClave.intentosRestantes(1))
        assertEquals(1, ReglasClave.intentosRestantes(4))
        assertEquals(5, ReglasClave.intentosRestantes(5))
    }
}
