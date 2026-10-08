package com.focuszone.app.logica

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ReglasSaldoTest {

    @Test
    fun `el saldo es lo ganado menos lo gastado y nunca negativo`() {
        assertEquals(7, ReglasSaldo.saldo(ganadosHoy = 12, gastadosHoy = 5))
        assertEquals(0, ReglasSaldo.saldo(ganadosHoy = 5, gastadosHoy = 5))
        assertEquals(0, ReglasSaldo.saldo(ganadosHoy = 3, gastadosHoy = 10))
    }

    @Test
    fun `sin saldo no hay opciones`() {
        assertEquals(emptyList<Int>(), ReglasSaldo.opciones(0))
    }

    @Test
    fun `con poco saldo se ofrece lo que queda`() {
        assertEquals(listOf(3), ReglasSaldo.opciones(3))
        assertEquals(listOf(5, 7), ReglasSaldo.opciones(7))
        assertEquals(listOf(5, 10, 12), ReglasSaldo.opciones(12))
    }

    @Test
    fun `si el saldo coincide con una opcion no se repite`() {
        assertEquals(listOf(5), ReglasSaldo.opciones(5))
        assertEquals(listOf(5, 10), ReglasSaldo.opciones(10))
    }

    @Test
    fun `con saldo de sobra se ofrecen las tres opciones normales`() {
        assertEquals(listOf(5, 10, 15), ReglasSaldo.opciones(15))
        assertEquals(listOf(5, 10, 15), ReglasSaldo.opciones(40))
    }

    @Test
    fun `el dia empieza a medianoche en la zona horaria local`() {
        val bogota = ZoneId.of("America/Bogota")
        val tarde = LocalDateTime.of(2026, 10, 8, 18, 45).atZone(bogota).toInstant().toEpochMilli()
        val medianoche = LocalDateTime.of(2026, 10, 8, 0, 0).atZone(bogota).toInstant().toEpochMilli()
        assertEquals(medianoche, ReglasSaldo.inicioDelDia(tarde, bogota))
    }

    @Test
    fun `un minuto antes de medianoche sigue siendo el mismo dia`() {
        val bogota = ZoneId.of("America/Bogota")
        val casi = LocalDateTime.of(2026, 10, 8, 23, 59).atZone(bogota).toInstant().toEpochMilli()
        val inicio = LocalDateTime.of(2026, 10, 8, 0, 0).atZone(bogota).toInstant().toEpochMilli()
        assertEquals(inicio, ReglasSaldo.inicioDelDia(casi, bogota))
    }
}
