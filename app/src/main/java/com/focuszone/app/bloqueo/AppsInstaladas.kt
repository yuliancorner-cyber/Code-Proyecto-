package com.focuszone.app.bloqueo

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Una app instalada que se puede marcar como distractora. */
data class AppInstalada(
    val paquete: String,
    val nombre: String,
    val icono: ImageBitmap,
    val sugerida: Boolean
)

object AppsInstaladas {

    /**
     * Apps de distraccion conocidas: salen primero en la lista con la etiqueta
     * "Sugerida". No se marcan solas: decides tu.
     */
    private val SUGERIDAS = setOf(
        "com.instagram.android",
        "com.zhiliaoapp.musically",   // TikTok
        "com.ss.android.ugc.trill",   // TikTok (otras regiones)
        "com.facebook.katana",
        "com.facebook.orca",          // Messenger
        "com.twitter.android",        // X
        "com.google.android.youtube",
        "com.snapchat.android",
        "com.reddit.frontpage",
        "com.pinterest",
        "com.netflix.mediaclient",
        "com.whatsapp",
        "org.telegram.messenger",
        "com.discord",
        "tv.twitch.android.app",
        "com.spotify.music"
    )

    /**
     * Apps que NUNCA se pueden bloquear, por seguridad: la propia FocusZone,
     * los Ajustes (para poder desactivar cualquier cosa) y el telefono
     * (para poder llamar a emergencias siempre).
     */
    val NUNCA_BLOQUEAR = setOf(
        "com.focuszone.app",
        "com.android.settings",
        "com.android.phone",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.contacts",
        "com.android.incallui",
        "com.android.emergency"
    )

    /**
     * Lee las apps que aparecen en el menu de aplicaciones.
     * Tarda un poco (hay que cargar cada icono), por eso se hace fuera del
     * hilo principal con withContext(Dispatchers.Default).
     */
    suspend fun cargar(context: Context): List<AppInstalada> = withContext(Dispatchers.Default) {
        val pm = context.packageManager
        val menuDeApps = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        @Suppress("DEPRECATION")
        val encontradas = pm.queryIntentActivities(menuDeApps, 0)

        encontradas
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName !in NUNCA_BLOQUEAR }
            .map { info ->
                val paquete = info.activityInfo.packageName
                AppInstalada(
                    paquete = paquete,
                    nombre = info.loadLabel(pm).toString(),
                    icono = info.loadIcon(pm).toBitmap(96, 96).asImageBitmap(),
                    sugerida = paquete in SUGERIDAS
                )
            }
            // Primero las sugeridas, luego el resto en orden alfabetico.
            .sortedWith(compareByDescending<AppInstalada> { it.sugerida }.thenBy { it.nombre.lowercase() })
    }
}
