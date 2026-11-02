package com.focuszone.app.logica

/**
 * Secciones ("subventanas") de una app que se pueden bloquear sin bloquear la
 * app entera: puedes seguir chateando en WhatsApp o Instagram, pero no ver
 * estados, canales, historias ni reels.
 */
enum class Subventana {
    /** Pestaña "Novedades" de WhatsApp: estados y canales. */
    WHATSAPP_NOVEDADES,

    /** Historias de Instagram (el visor a pantalla completa). */
    INSTAGRAM_HISTORIAS,

    /** Reels de Instagram: la pestaña Reels y el visor de reels. */
    INSTAGRAM_REELS
}

/**
 * Lo que el servicio de accesibilidad "ve" de la pantalla en un momento dado.
 * Solo datos de estructura: NUNCA el texto de los mensajes.
 *
 * @param clase nombre interno de la ultima pantalla (Activity) abierta en la app.
 * @param ids identificadores internos de los elementos visibles, sin el
 *   prefijo del paquete (p. ej. "reel_viewer_root").
 * @param seleccionados id y etiqueta de los elementos marcados como
 *   "seleccionados" (p. ej. la pestaña activa: "Novedades", "clips_tab").
 */
data class VistaApp(
    val paquete: String,
    val clase: String?,
    val ids: Set<String>,
    val seleccionados: Set<String>
)

/**
 * Reconoce en que seccion de WhatsApp o Instagram estas.
 *
 * OJO: estas apps no publican como estan hechas por dentro; estas pistas
 * salen de como se ven hoy y pueden cambiar con sus actualizaciones. Si un
 * dia dejan de funcionar, el diagnostico de la pantalla de bloqueo muestra
 * lo que FocusZone ve para poder ajustarlas aqui.
 *
 * Sin nada de Android: probado en ReglasSubventanasTest.
 */
object ReglasSubventanas {

    val PAQUETES_WHATSAPP = setOf("com.whatsapp", "com.whatsapp.w4b")
    const val PAQUETE_INSTAGRAM = "com.instagram.android"

    // --- WhatsApp -----------------------------------------------------------
    // Nombre de la pestaña activa (español e inglés; versiones nuevas y viejas).
    private val PESTANAS_NOVEDADES = listOf("novedades", "actualizaciones", "updates", "estados", "status")

    // Pantallas internas: WhatsApp llama "newsletter" a los canales.
    private val CLASES_NOVEDADES = listOf("statusplayback", "newsletter")
    private val IDS_NOVEDADES = listOf("status_playback", "newsletter")

    // --- Instagram ----------------------------------------------------------
    // Instagram llama "reel" a las historias y "clips" a los reels. Ojo: la
    // fila de historias del inicio es "reels_tray", y esa NO se bloquea.
    private val IDS_HISTORIAS = listOf("reel_viewer")
    private val CLASES_HISTORIAS = listOf("reelviewer")
    private val IDS_REELS = listOf("clips_viewer")
    private val SELECCION_REELS = listOf("clips_tab", "reels")

    /** true si vale la pena mirar dentro de esta app (las demas se ignoran). */
    fun vigila(paquete: String): Boolean = paquete in PAQUETES_WHATSAPP || paquete == PAQUETE_INSTAGRAM

    /** Las secciones que pertenecen a [paquete]. */
    fun de(paquete: String): Set<Subventana> = when {
        paquete in PAQUETES_WHATSAPP -> setOf(Subventana.WHATSAPP_NOVEDADES)
        paquete == PAQUETE_INSTAGRAM -> setOf(Subventana.INSTAGRAM_HISTORIAS, Subventana.INSTAGRAM_REELS)
        else -> emptySet()
    }

    /**
     * La seccion bloqueada en la que estas, o null si ninguna.
     * Solo considera las que marcaste ([activas]).
     */
    fun detectar(vista: VistaApp, activas: Set<Subventana>): Subventana? {
        val candidatas = de(vista.paquete).filter { it in activas }
        return candidatas.firstOrNull { coincide(it, vista) }
    }

    private fun coincide(seccion: Subventana, vista: VistaApp): Boolean {
        val clase = vista.clase?.lowercase().orEmpty()
        val ids = vista.ids.map { it.lowercase() }
        val seleccionados = vista.seleccionados.map { it.trim().lowercase() }
        return when (seccion) {
            Subventana.WHATSAPP_NOVEDADES ->
                CLASES_NOVEDADES.any { it in clase } ||
                    ids.any { id -> IDS_NOVEDADES.any { it in id } } ||
                    seleccionados.any { sel -> PESTANAS_NOVEDADES.any { sel.startsWith(it) } }

            Subventana.INSTAGRAM_HISTORIAS ->
                CLASES_HISTORIAS.any { it in clase } ||
                    ids.any { id -> IDS_HISTORIAS.any { it in id } }

            Subventana.INSTAGRAM_REELS ->
                ids.any { id -> IDS_REELS.any { it in id } } ||
                    seleccionados.any { it in SELECCION_REELS }
        }
    }

    /** Quita el prefijo "com.instagram.android:id/" de un id de Android. */
    fun idCorto(idCompleto: String): String = idCompleto.substringAfter(":id/")
}
