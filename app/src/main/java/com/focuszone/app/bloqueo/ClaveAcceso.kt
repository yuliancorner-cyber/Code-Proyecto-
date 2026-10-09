package com.focuszone.app.bloqueo

import android.content.Context
import com.focuszone.app.logica.ReglasClave
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Guarda y comprueba la clave (PIN) que protege la configuracion del bloqueo.
 *
 * Solo se guarda el hash (ver ReglasClave), los fallos seguidos y hasta cuando
 * hay que esperar. Si la olvidas no hay forma de recuperarla desde la app:
 * solo borrando los datos de FocusZone en Ajustes (y con ellos todo lo demas).
 */
object ClaveAcceso {

    private const val ARCHIVO = "seguridad"
    private const val CLAVE_HASH = "clave_configuracion"
    private const val CLAVE_FALLOS = "fallos_seguidos"
    private const val CLAVE_ESPERAR_HASTA = "esperar_hasta_millis"

    sealed interface Resultado {
        data object Correcta : Resultado
        data class Incorrecta(val intentosRestantes: Int) : Resultado
        data class Esperar(val segundos: Long) : Resultado
    }

    /** true si ya se creo una clave. */
    fun configurada(context: Context): Boolean = preferencias(context).contains(CLAVE_HASH)

    /** Guarda una clave nueva (al crearla o al cambiarla). */
    suspend fun guardar(context: Context, clave: String) {
        // El calculo del hash es lento a proposito: fuera del hilo de la pantalla.
        val protegida = withContext(Dispatchers.Default) { ReglasClave.proteger(clave) }
        preferencias(context).edit()
            .putString(CLAVE_HASH, protegida)
            .putInt(CLAVE_FALLOS, 0)
            .putLong(CLAVE_ESPERAR_HASTA, 0L)
            .apply()
    }

    suspend fun verificar(context: Context, clave: String): Resultado {
        val prefs = preferencias(context)
        val ahora = System.currentTimeMillis()

        val espera = prefs.getLong(CLAVE_ESPERAR_HASTA, 0L) - ahora
        if (espera > 0) return Resultado.Esperar(segundosRedondeados(espera))

        val guardada = prefs.getString(CLAVE_HASH, null) ?: return Resultado.Incorrecta(0)
        val correcta = withContext(Dispatchers.Default) { ReglasClave.coincide(clave, guardada) }

        if (correcta) {
            prefs.edit().putInt(CLAVE_FALLOS, 0).putLong(CLAVE_ESPERAR_HASTA, 0L).apply()
            return Resultado.Correcta
        }

        val fallos = prefs.getInt(CLAVE_FALLOS, 0) + 1
        val nuevaEspera = ReglasClave.esperaTrasFallos(fallos)
        prefs.edit()
            .putInt(CLAVE_FALLOS, fallos)
            .putLong(CLAVE_ESPERAR_HASTA, if (nuevaEspera > 0) ahora + nuevaEspera else 0L)
            .apply()
        return if (nuevaEspera > 0) {
            Resultado.Esperar(segundosRedondeados(nuevaEspera))
        } else {
            Resultado.Incorrecta(ReglasClave.intentosRestantes(fallos))
        }
    }

    private fun segundosRedondeados(ms: Long): Long = (ms + 999) / 1000

    private fun preferencias(context: Context) =
        context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
}
