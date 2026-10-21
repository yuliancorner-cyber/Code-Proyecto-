package com.focuszone.app.bloqueo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Arranca el guardian al encender el celular y despues de instalar una
 * version nueva de FocusZone, sin tener que abrir la app.
 */
class ReceptorArranque : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON" -> ServicioGuardian.iniciar(context)
        }
    }
}
