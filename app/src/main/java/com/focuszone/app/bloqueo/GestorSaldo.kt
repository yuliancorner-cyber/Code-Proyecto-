package com.focuszone.app.bloqueo

import android.content.Context
import com.focuszone.app.datos.BaseDatos
import com.focuszone.app.datos.DesbloqueoRegistro
import com.focuszone.app.logica.ReglasSaldo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * Calcula el saldo del dia y gasta minutos.
 * Las reglas (como se calcula, que opciones hay) estan en ReglasSaldo.
 */
object GestorSaldo {

    /** Saldo que se actualiza solo: baja al gastar y sube al completar una sesion. */
    fun saldoDesde(context: Context, inicioDelDiaMillis: Long): Flow<Int> {
        val bd = BaseDatos.obtener(context)
        return combine(
            bd.sesionDao().creditosDesde(inicioDelDiaMillis),
            bd.desbloqueoDao().minutosGastadosDesde(inicioDelDiaMillis)
        ) { ganados, gastados -> ReglasSaldo.saldo(ganados, gastados) }
    }

    /**
     * Gasta [minutos] del saldo de hoy y abre la ventana de desbloqueo.
     *
     * Vuelve a comprobar el saldo justo antes de gastar (por si acaso, p. ej.
     * un doble toque), y devuelve false si no alcanza.
     */
    suspend fun desbloquear(context: Context, minutos: Int): Boolean {
        val ahora = System.currentTimeMillis()
        val saldo = saldoDesde(context, ReglasSaldo.inicioDelDia(ahora)).first()
        if (minutos <= 0 || minutos > saldo) return false

        // Primero la ventana (lectura instantanea para el servicio de bloqueo),
        // despues el registro en la base de datos.
        Desbloqueo.abrir(context, minutos, ahora)
        BaseDatos.obtener(context).desbloqueoDao()
            .insertar(DesbloqueoRegistro(inicioMillis = ahora, minutos = minutos))
        return true
    }
}
