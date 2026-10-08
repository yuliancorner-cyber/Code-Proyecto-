package com.focuszone.app.bloqueo

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.focuszone.app.MainActivity
import com.focuszone.app.R
import com.focuszone.app.sesion.SesionActual
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Servicio de accesibilidad que bloquea las apps distractoras.
 *
 * Que es un servicio de accesibilidad: una pieza que Android creo para ayudar
 * a personas con discapacidad (lectores de pantalla, por ejemplo). Recibe
 * avisos de lo que pasa en pantalla. Por eso es tan potente y Android exige
 * activarlo a mano.
 *
 * Que usamos de el: SOLO el aviso "cambio la ventana principal" y, de ese
 * aviso, SOLO el nombre del paquete de la app que se abrio. No leemos texto,
 * ni botones, ni lo que escribes (ver res/xml/config_bloqueo.xml:
 * canRetrieveWindowContent = false).
 *
 * Reglas, cuando abres una app de tu lista:
 *  1. Hay una sesion de estudio en marcha -> bloqueada siempre (vuelves a la sesion).
 *  2. Tienes una ventana de desbloqueo abierta -> se permite, y se programa
 *     una revision para cuando la ventana se acabe.
 *  3. Si no -> bloqueada; FocusZone te ofrece gastar saldo para desbloquear.
 */
class ServicioBloqueo : AccessibilityService() {

    companion object {
        private const val TAG = "ServicioBloqueo"

        /**
         * Espera entre "pulsar inicio" y abrir FocusZone. Android ejecuta el
         * "inicio" con un pequeno retraso: si abrimos FocusZone de inmediato,
         * a veces el inicio llega DESPUES y la tapa (solo se veia el escritorio).
         */
        private const val RETRASO_ABRIR_FOCUSZONE_MS = 400L

        private val _funcionando = MutableStateFlow(false)

        /**
         * true mientras Android tiene este servicio realmente en marcha.
         * Puede estar "activado" en Ajustes pero detenido (p. ej. si el sistema
         * lo mato para ahorrar bateria): asi la app lo detecta y te avisa.
         */
        val funcionando: StateFlow<Boolean> = _funcionando.asStateFlow()
    }

    private val manejador = Handler(Looper.getMainLooper())
    private var ultimoAvisoMs = 0L

    /** Ultima app "de verdad" en pantalla (sin contar teclado ni barra del sistema). */
    private var paqueteActual: String? = null

    /** Al acabarse la ventana de desbloqueo: si sigues en una app bloqueada, te saca. */
    private val revisarAlExpirar = Runnable {
        val paquete = paqueteActual ?: return@Runnable
        sinCaerse { revisar(paquete) }
    }

    private val abrirFocusZone = Runnable {
        sinCaerse {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _funcionando.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val paquete = event.packageName?.toString() ?: return
        if (esVentanaDelSistema(paquete)) return

        paqueteActual = paquete
        sinCaerse { revisar(paquete) }
    }

    /**
     * Un error aqui cerraria la app entera, y Android desactivaria el servicio
     * hasta que lo vuelvas a encender a mano. Mejor anotarlo y seguir vivos.
     */
    private inline fun sinCaerse(accion: () -> Unit) {
        try {
            accion()
        } catch (e: Exception) {
            Log.e(TAG, "Error en el servicio de bloqueo", e)
        }
    }

    private fun revisar(paquete: String) {
        if (AppsInstaladas.protegida(paquete)) return
        if (!AppsBloqueadas.contiene(this, paquete)) return

        when {
            SesionActual.bloqueoActivo -> bloquearPorSesion(paquete)
            Desbloqueo.activo(this) -> programarRevision()
            else -> bloquearPorSaldo(paquete)
        }
    }

    private fun bloquearPorSesion(paquete: String) {
        avisar(getString(R.string.bloqueo_aviso, nombreDe(paquete)))
        salirYMostrarFocusZone()
    }

    private fun bloquearPorSaldo(paquete: String) {
        // FocusZone lee esta solicitud y muestra "¿Desbloquear X min?".
        SolicitudDesbloqueo.pedir(paquete)
        salirYMostrarFocusZone()
    }

    /**
     * 1. "Pulsar inicio": lo que de verdad te saca de la app (siempre funciona).
     * 2. Un momento despues, abrir FocusZone encima. En Xiaomi esto requiere el
     *    permiso "Mostrar ventanas emergentes en segundo plano"; si falta, al
     *    menos quedas en el escritorio, fuera de la app bloqueada.
     */
    private fun salirYMostrarFocusZone() {
        performGlobalAction(GLOBAL_ACTION_HOME)
        manejador.removeCallbacks(abrirFocusZone)
        manejador.postDelayed(abrirFocusZone, RETRASO_ABRIR_FOCUSZONE_MS)
    }

    private fun programarRevision() {
        manejador.removeCallbacks(revisarAlExpirar)
        val espera = (Desbloqueo.hasta(this) - System.currentTimeMillis()).coerceAtLeast(0L)
        // +500 ms de margen para revisar cuando la ventana ya este cerrada seguro.
        manejador.postDelayed(revisarAlExpirar, espera + 500L)
    }

    /** Aviso breve, sin repetirlo si la app lanza varios eventos seguidos. */
    private fun avisar(texto: String) {
        val ahora = SystemClock.elapsedRealtime()
        if (ahora - ultimoAvisoMs > 2_000L) {
            Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()
            ultimoAvisoMs = ahora
        }
    }

    /**
     * La barra de notificaciones y el teclado tambien generan avisos de
     * "cambio de ventana", pero no son la app que estas usando: los ignoramos
     * para no perder de vista en que app estas.
     */
    private fun esVentanaDelSistema(paquete: String): Boolean {
        if (paquete == "com.android.systemui") return true
        val teclado = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')
        return paquete == teclado
    }

    private fun nombreDe(paquete: String): String = try {
        @Suppress("DEPRECATION")
        val info = packageManager.getApplicationInfo(paquete, 0)
        packageManager.getApplicationLabel(info).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        paquete
    }

    // Obligatorio: Android lo llama si tiene que interrumpir el servicio. No hay nada que parar.
    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        _funcionando.value = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        _funcionando.value = false
        manejador.removeCallbacks(revisarAlExpirar)
        manejador.removeCallbacks(abrirFocusZone)
        super.onDestroy()
    }
}
