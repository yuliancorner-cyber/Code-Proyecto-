package com.focuszone.app.bloqueo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Estado de los dos permisos especiales que necesita el bloqueo. */
data class EstadoPermisos(
    val accesibilidad: Boolean,
    val noMolestar: Boolean
)

/**
 * Consulta y abre los ajustes de los permisos especiales.
 *
 * Estos permisos no se pueden pedir con un dialogo normal: Android obliga a
 * que el usuario los active a mano en Ajustes. Lo unico que puede hacer la
 * app es llevarte a la pantalla correcta.
 */
object PermisosBloqueo {

    fun leer(context: Context) = EstadoPermisos(
        accesibilidad = accesibilidadActiva(context),
        noMolestar = ModoSilencio.tienePermiso(context)
    )

    /** true si nuestro servicio aparece en la lista de servicios de accesibilidad activos. */
    fun accesibilidadActiva(context: Context): Boolean {
        val activos = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val propio = ComponentName(context, ServicioBloqueo::class.java)
        // La lista viene como "paquete/Clase:paquete/Clase:..."
        return activos.split(':').any { ComponentName.unflattenFromString(it) == propio }
    }

    fun abrirAjustesAccesibilidad(context: Context) = abrir(context, Settings.ACTION_ACCESSIBILITY_SETTINGS)

    fun abrirAjustesNoMolestar(context: Context) =
        abrir(context, Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    private fun abrir(context: Context, accion: String) {
        context.startActivity(Intent(accion).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
