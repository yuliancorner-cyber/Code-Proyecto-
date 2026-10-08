package com.focuszone.app.bloqueo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Estado de los permisos especiales que necesita el bloqueo. */
data class EstadoPermisos(
    /** Activado por ti en Ajustes > Accesibilidad. */
    val accesibilidad: Boolean,
    val noMolestar: Boolean,
    /** Ademas de activado, Android lo tiene en marcha ahora mismo. */
    val accesibilidadFuncionando: Boolean = false
) {
    /** Activado en Ajustes pero detenido por el sistema (ahorro de bateria, error...). */
    val accesibilidadDetenida: Boolean get() = accesibilidad && !accesibilidadFuncionando
}

/**
 * Consulta y abre los ajustes de los permisos especiales.
 *
 * Estos permisos no se pueden pedir con un dialogo normal: Android obliga a
 * que el usuario los active a mano en Ajustes. Lo unico que puede hacer la
 * app es llevarte a la pantalla correcta.
 */
object PermisosBloqueo {

    fun leer(context: Context): EstadoPermisos {
        val activada = accesibilidadActiva(context)
        return EstadoPermisos(
            accesibilidad = activada,
            noMolestar = ModoSilencio.tienePermiso(context),
            accesibilidadFuncionando = activada && ServicioBloqueo.funcionando.value
        )
    }

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

    /** Pantalla "Info de la app" de FocusZone (en Xiaomi: ahi estan "Otros permisos"). */
    fun abrirAjustesDeLaApp(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun abrir(context: Context, accion: String) {
        context.startActivity(Intent(accion).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
