package com.focuszone.app.logica

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Estadisticas de la Fase 4. Todo se calcula aqui, sin Android, a partir de
 * copias simples de lo guardado en la base de datos (probado en EstadisticasTest).
 */

/** Lo que necesitamos de una sesion guardada. */
data class SesionDato(
    val finMillis: Long,
    val metaMin: Int,
    val estudiadoSeg: Long,
    val completada: Boolean,
    val creditosMin: Int
)

/** Lo que necesitamos de un desbloqueo (gasto de saldo). */
data class GastoDato(val inicioMillis: Long, val minutos: Int)

/** Minutos de enfoque de un dia concreto (una barra de la grafica). */
data class DiaEstudio(val fecha: LocalDate, val minutos: Int)

/** Una fila del historial: o una sesion, o un gasto de saldo. */
sealed interface EntradaHistorial {
    val cuandoMillis: Long

    data class Sesion(val dato: SesionDato) : EntradaHistorial {
        override val cuandoMillis: Long get() = dato.finMillis
    }

    data class Gasto(val dato: GastoDato) : EntradaHistorial {
        override val cuandoMillis: Long get() = dato.inicioMillis
    }
}

data class Estadisticas(
    /** Ultimos 7 dias, del mas antiguo a hoy (siempre 7, con 0 los dias sin estudio). */
    val ultimosDias: List<DiaEstudio>,
    /** Dias seguidos con al menos una sesion completada. */
    val rachaDias: Int,
    val completadas: Int,
    val canceladas: Int,
    /** Minutos totales con el celular boca abajo (cuenta tambien lo estudiado en sesiones canceladas). */
    val minutosEnfoqueTotal: Int,
    val ganadoTotal: Int,
    val gastadoTotal: Int,
    val ganadoHoy: Int,
    val gastadoHoy: Int,
    /** Sesiones y desbloqueos mezclados, del mas reciente al mas antiguo. */
    val historial: List<EntradaHistorial>
) {
    val sesionesTotales: Int get() = completadas + canceladas

    /** Porcentaje de sesiones completadas, redondeado. null si aun no hay sesiones. */
    val porcentajeExito: Int?
        get() = if (sesionesTotales == 0) null else (completadas * 100 + sesionesTotales / 2) / sesionesTotales

    val minutosSemana: Int get() = ultimosDias.sumOf { it.minutos }

    /** true si todavia no hay nada que mostrar. */
    val vacio: Boolean get() = sesionesTotales == 0 && gastadoTotal == 0
}

object CalculoEstadisticas {

    const val DIAS_GRAFICA = 7
    const val LIMITE_HISTORIAL = 30

    fun calcular(
        sesiones: List<SesionDato>,
        gastos: List<GastoDato>,
        ahoraMillis: Long,
        zona: ZoneId = ZoneId.systemDefault()
    ): Estadisticas {
        fun fechaDe(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zona).toLocalDate()
        val hoy = fechaDe(ahoraMillis)

        // --- Grafica: minutos de enfoque por dia (por la fecha en que termino la sesion)
        val segundosPorDia: Map<LocalDate, Long> = sesiones
            .groupBy { fechaDe(it.finMillis) }
            .mapValues { (_, delDia) -> delDia.sumOf { it.estudiadoSeg } }
        val ultimosDias = (DIAS_GRAFICA - 1 downTo 0).map { diasAtras ->
            val fecha = hoy.minusDays(diasAtras.toLong())
            DiaEstudio(fecha, ((segundosPorDia[fecha] ?: 0L) / 60).toInt())
        }

        // --- Racha: dias seguidos con alguna sesion completada.
        // Si hoy aun no completaste ninguna, la racha cuenta hasta ayer (no se
        // rompe hasta que termina el dia).
        val diasConSesionCompletada = sesiones.filter { it.completada }.map { fechaDe(it.finMillis) }.toSet()
        var dia = if (hoy in diasConSesionCompletada) hoy else hoy.minusDays(1)
        var racha = 0
        while (dia in diasConSesionCompletada) {
            racha++
            dia = dia.minusDays(1)
        }

        val completadas = sesiones.filter { it.completada }

        val historial = (sesiones.map { EntradaHistorial.Sesion(it) } + gastos.map { EntradaHistorial.Gasto(it) })
            .sortedByDescending { it.cuandoMillis }
            .take(LIMITE_HISTORIAL)

        return Estadisticas(
            ultimosDias = ultimosDias,
            rachaDias = racha,
            completadas = completadas.size,
            canceladas = sesiones.size - completadas.size,
            minutosEnfoqueTotal = (sesiones.sumOf { it.estudiadoSeg } / 60).toInt(),
            ganadoTotal = completadas.sumOf { it.creditosMin },
            gastadoTotal = gastos.sumOf { it.minutos },
            ganadoHoy = completadas.filter { fechaDe(it.finMillis) == hoy }.sumOf { it.creditosMin },
            gastadoHoy = gastos.filter { fechaDe(it.inicioMillis) == hoy }.sumOf { it.minutos },
            historial = historial
        )
    }
}
