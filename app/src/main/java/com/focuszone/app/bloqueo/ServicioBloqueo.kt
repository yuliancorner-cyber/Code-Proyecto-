package com.focuszone.app.bloqueo

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
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
 * Que hace: si hay una sesion en marcha y abres una app de tu lista, te manda
 * a la pantalla de inicio y abre FocusZone (que te mostrara la alerta).
 */
class ServicioBloqueo : AccessibilityService() {

    private var ultimoAvisoMs = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val paquete = event.packageName?.toString() ?: return

        if (paquete == packageName) return
        if (paquete in AppsInstaladas.NUNCA_BLOQUEAR) return
        if (!SesionActual.bloqueoActivo) return
        if (!AppsBloqueadas.contiene(this, paquete)) return

        bloquear(paquete)
    }

    private fun bloquear(paquete: String) {
        // 1. Sacarte de la app: equivale a pulsar el boton de inicio.
        performGlobalAction(GLOBAL_ACTION_HOME)

        // 2. Aviso breve (sin repetirlo si la app lanza varios eventos seguidos).
        val ahora = SystemClock.elapsedRealtime()
        if (ahora - ultimoAvisoMs > 2_000L) {
            Toast.makeText(this, getString(R.string.bloqueo_aviso, nombreDe(paquete)), Toast.LENGTH_SHORT).show()
            ultimoAvisoMs = ahora
        }

        // 3. Traer FocusZone al frente: muestra la sesion o la alerta.
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
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
}
