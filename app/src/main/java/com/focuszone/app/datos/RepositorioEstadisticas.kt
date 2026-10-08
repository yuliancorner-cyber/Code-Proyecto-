package com.focuszone.app.datos

import android.content.Context
import com.focuszone.app.logica.CalculoEstadisticas
import com.focuszone.app.logica.Estadisticas
import com.focuszone.app.logica.GastoDato
import com.focuszone.app.logica.SesionDato
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

/**
 * Puente entre la base de datos y el calculo de estadisticas:
 * lee sesiones y desbloqueos, los convierte a los datos simples que entiende
 * CalculoEstadisticas y recalcula cada vez que se guarda algo nuevo.
 */
object RepositorioEstadisticas {

    fun observar(context: Context): Flow<Estadisticas> {
        val bd = BaseDatos.obtener(context)
        return combine(bd.sesionDao().todas(), bd.desbloqueoDao().todos()) { sesiones, desbloqueos ->
            CalculoEstadisticas.calcular(
                sesiones = sesiones.map {
                    SesionDato(
                        finMillis = it.finMillis,
                        metaMin = it.metaMin,
                        estudiadoSeg = it.estudiadoSeg,
                        completada = it.completada,
                        creditosMin = it.creditosMin
                    )
                },
                gastos = desbloqueos.map { GastoDato(inicioMillis = it.inicioMillis, minutos = it.minutos) },
                ahoraMillis = System.currentTimeMillis()
            )
        }.flowOn(Dispatchers.Default) // el calculo se hace fuera del hilo de la pantalla
    }
}
