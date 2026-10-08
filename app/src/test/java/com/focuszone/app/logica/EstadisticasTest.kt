package com.focuszone.app.logica

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EstadisticasTest {

    private val zona = ZoneId.of("America/Bogota")
    private val hoy = LocalDate.of(2026, 10, 8)

    /** Milisegundos de [diasAtras] dias antes de hoy, a la hora dada. */
    private fun momento(diasAtras: Int, hora: Int = 18): Long =
        LocalDateTime.of(hoy.minusDays(diasAtras.toLong()), java.time.LocalTime.of(hora, 0))
            .atZone(zona).toInstant().toEpochMilli()

    private val ahora = momento(0, 20)

    private fun completada(diasAtras: Int, meta: Int = 25) =
        SesionDato(momento(diasAtras), meta, meta * 60L, completada = true, creditosMin = meta / 5)

    private fun cancelada(diasAtras: Int, estudiadoSeg: Long) =
        SesionDato(momento(diasAtras), 25, estudiadoSeg, completada = false, creditosMin = 0)

    private fun calcular(sesiones: List<SesionDato>, gastos: List<GastoDato> = emptyList()) =
        CalculoEstadisticas.calcular(sesiones, gastos, ahora, zona)

    @Test
    fun `sin datos todo esta vacio`() {
        val e = calcular(emptyList())
        assertTrue(e.vacio)
        assertEquals(7, e.ultimosDias.size)
        assertTrue(e.ultimosDias.all { it.minutos == 0 })
        assertEquals(0, e.rachaDias)
        assertNull(e.porcentajeExito)
    }

    @Test
    fun `la grafica va del dia mas antiguo a hoy`() {
        val e = calcular(emptyList())
        assertEquals(hoy.minusDays(6), e.ultimosDias.first().fecha)
        assertEquals(hoy, e.ultimosDias.last().fecha)
    }

    @Test
    fun `suma los minutos de cada dia incluidas las sesiones canceladas`() {
        val e = calcular(listOf(completada(0, 25), cancelada(0, 10 * 60L), completada(2, 15)))
        assertEquals(35, e.ultimosDias.last().minutos)       // hoy: 25 + 10
        assertEquals(15, e.ultimosDias[4].minutos)           // hace 2 dias
        assertEquals(50, e.minutosSemana)
    }

    @Test
    fun `las sesiones de hace mas de 7 dias no salen en la grafica pero si en el total`() {
        val e = calcular(listOf(completada(10, 60), completada(0, 15)))
        assertEquals(15, e.minutosSemana)
        assertEquals(75, e.minutosEnfoqueTotal)
    }

    @Test
    fun `la racha cuenta dias seguidos hasta hoy`() {
        val e = calcular(listOf(completada(0), completada(1), completada(2), completada(4)))
        assertEquals(3, e.rachaDias)
    }

    @Test
    fun `si hoy aun no estudiaste la racha cuenta hasta ayer`() {
        val e = calcular(listOf(completada(1), completada(2)))
        assertEquals(2, e.rachaDias)
    }

    @Test
    fun `un dia sin estudiar rompe la racha`() {
        val e = calcular(listOf(completada(2), completada(3)))
        assertEquals(0, e.rachaDias)
    }

    @Test
    fun `las sesiones canceladas no mantienen la racha`() {
        val e = calcular(listOf(completada(0), cancelada(1, 600)))
        assertEquals(1, e.rachaDias)
    }

    @Test
    fun `porcentaje de exito redondeado`() {
        val e = calcular(listOf(completada(0), completada(1), cancelada(1, 60)))
        assertEquals(2, e.completadas)
        assertEquals(1, e.canceladas)
        assertEquals(67, e.porcentajeExito)
    }

    @Test
    fun `ganado y gastado de hoy y totales`() {
        val gastos = listOf(GastoDato(momento(0, 19), 5), GastoDato(momento(1), 10))
        val e = calcular(listOf(completada(0, 25), completada(1, 60)), gastos)
        assertEquals(5, e.ganadoHoy)
        assertEquals(5, e.gastadoHoy)
        assertEquals(17, e.ganadoTotal)
        assertEquals(15, e.gastadoTotal)
    }

    @Test
    fun `el historial mezcla sesiones y gastos del mas reciente al mas antiguo`() {
        val gasto = GastoDato(momento(0, 19), 5)
        val e = calcular(listOf(completada(0), completada(1)), listOf(gasto))
        assertEquals(gasto.inicioMillis, e.historial[0].cuandoMillis)
        assertTrue(e.historial[0] is EntradaHistorial.Gasto)
        assertTrue(e.historial[1] is EntradaHistorial.Sesion)
        assertTrue(e.historial[1].cuandoMillis > e.historial[2].cuandoMillis)
    }

    @Test
    fun `el historial se limita a las 30 entradas mas recientes`() {
        val muchas = (0 until 40).map { completada(it % 7) }
        assertEquals(30, calcular(muchas).historial.size)
    }
}
