package com.focuszone.app.bloqueo

import android.app.NotificationManager
import android.content.Context

/**
 * Activa y restaura el "No molestar" de Android durante la sesion.
 *
 * Usamos el modo "solo alarmas": las notificaciones llegan pero sin sonido
 * ni vibracion, y las ves al terminar. La alerta de FocusZone sigue vibrando
 * porque la marcamos como alarma.
 *
 * Reglas para no estropear tu configuracion:
 *  - Si ya tenias No molestar puesto por tu cuenta, no lo tocamos (ni al
 *    empezar ni al terminar).
 *  - Guardamos una marca en disco cuando LO ACTIVAMOS NOSOTROS. Si la app
 *    muriera a mitad de sesion, al volver a abrirla vemos la marca y lo
 *    restauramos ([restaurar]), para que tu celular no quede en silencio.
 */
object ModoSilencio {

    private const val ARCHIVO = "bloqueo"
    private const val CLAVE_ACTIVADO_POR_APP = "no_molestar_activado_por_focuszone"

    fun tienePermiso(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

    fun activar(context: Context) {
        val gestor = context.getSystemService(NotificationManager::class.java)
        if (!gestor.isNotificationPolicyAccessGranted) return
        if (gestor.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return

        gestor.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
        preferencias(context).edit().putBoolean(CLAVE_ACTIVADO_POR_APP, true).apply()
    }

    /** Devuelve el sonido normal, solo si fuimos nosotros quienes lo quitamos. */
    fun restaurar(context: Context) {
        val prefs = preferencias(context)
        if (!prefs.getBoolean(CLAVE_ACTIVADO_POR_APP, false)) return

        val gestor = context.getSystemService(NotificationManager::class.java)
        if (gestor.isNotificationPolicyAccessGranted) {
            gestor.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
        prefs.edit().putBoolean(CLAVE_ACTIVADO_POR_APP, false).apply()
    }

    private fun preferencias(context: Context) =
        context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
}
