package com.focuszone.app.bloqueo

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.focuszone.app.MainActivity
import com.focuszone.app.R
import com.focuszone.app.sesion.SesionActual

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

    private val manejador = Handler(Looper.getMainLooper())
    private var ultimoAvisoMs = 0L

    /** Ultima app "de verdad" en pantalla (sin contar teclado ni barra del sistema). */
    private var paqueteActual: String? = null

    /** Al acabarse la ventana de desbloqueo: si sigues en una app bloqueada, te saca. */
    private val revisarAlExpirar = Runnable {
        val paquete = paqueteActual ?: return@Runnable
        revisar(paquete)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val paquete = event.packageName?.toString() ?: return
        if (esVentanaDelSistema(paquete)) return

        paqueteActual = paquete
        revisar(paquete)
    }

    private fun revisar(paquete: String) {
        if (paquete == packageName) return
        if (paquete in AppsInstaladas.NUNCA_BLOQUEAR) return
        if (!AppsBloqueadas.contiene(this, paquete)) return

        when {
            SesionActual.bloqueoActivo -> bloquearPorSesion(paquete)
            Desbloqueo.activo(this) -> programarRevision()
            else -> bloquearPorSaldo(paquete)
        }
    }

    private fun bloquearPorSesion(paquete: String) {
        salirDeLaApp()
        avisar(getString(R.string.bloqueo_aviso, nombreDe(paquete)))
        abrirFocusZone()
    }

    private fun bloquearPorSaldo(paquete: String) {
        salirDeLaApp()
        // FocusZone lee esta solicitud y muestra "¿Desbloquear X min?".
        SolicitudDesbloqueo.pedir(paquete)
        abrirFocusZone()
    }

    private fun programarRevision() {
        manejador.removeCallbacks(revisarAlExpirar)
        val espera = (Desbloqueo.hasta(this) - System.currentTimeMillis()).coerceAtLeast(0L)
        // +500 ms de margen para revisar cuando la ventana ya este cerrada seguro.
        manejador.postDelayed(revisarAlExpirar, espera + 500L)
    }

    /** Equivale a pulsar el boton de inicio. */
    private fun salirDeLaApp() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun abrirFocusZone() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
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

    override fun onDestroy() {
        manejador.removeCallbacks(revisarAlExpirar)
        super.onDestroy()
    }
}
