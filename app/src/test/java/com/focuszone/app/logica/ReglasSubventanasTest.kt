package com.focuszone.app.logica

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasSubventanasTest {

    private val todas = Subventana.entries.toSet()

    private fun vista(
        paquete: String,
        clase: String? = null,
        ids: Set<String> = emptySet(),
        seleccionados: Set<String> = emptySet()
    ) = VistaApp(paquete, clase, ids, seleccionados)

    @Test
    fun soloVigilaWhatsAppEInstagram() {
        assertTrue(ReglasSubventanas.vigila("com.whatsapp"))
        assertTrue(ReglasSubventanas.vigila("com.whatsapp.w4b"))
        assertTrue(ReglasSubventanas.vigila("com.instagram.android"))
        assertFalse(ReglasSubventanas.vigila("com.android.chrome"))
    }

    @Test
    fun whatsAppPestanaNovedadesSeleccionada() {
        val v = vista("com.whatsapp", "com.whatsapp.HomeActivity", seleccionados = setOf("Novedades"))
        assertEquals(Subventana.WHATSAPP_NOVEDADES, ReglasSubventanas.detectar(v, todas))
    }

    @Test
    fun whatsAppEnInglesYConContador() {
        val v = vista("com.whatsapp", seleccionados = setOf("Updates, 3 new updates"))
        assertEquals(Subventana.WHATSAPP_NOVEDADES, ReglasSubventanas.detectar(v, todas))
    }

    @Test
    fun whatsAppPestanaChatsNoSeBloquea() {
        val v = vista("com.whatsapp", "com.whatsapp.HomeActivity", setOf("conversations_row"), setOf("Chats"))
        assertNull(ReglasSubventanas.detectar(v, todas))
    }

    @Test
    fun whatsAppVisorDeEstadosPorClase() {
        val v = vista("com.whatsapp", "com.whatsapp.status.playback.StatusPlaybackActivity")
        assertEquals(Subventana.WHATSAPP_NOVEDADES, ReglasSubventanas.detectar(v, todas))
    }

    @Test
    fun whatsAppCanalPorClase() {
        val v = vista("com.whatsapp", "com.whatsapp.newsletter.NewsletterDirectoryActivity")
        assertEquals(Subventana.WHATSAPP_NOVEDADES, ReglasSubventanas.detectar(v, todas))
    }

    @Test
    fun instagramHistorias() {
        val v = vista("com.instagram.android", ids = setOf("reel_viewer_root", "reel_viewer_header"))
        assertEquals(Subventana.INSTAGRAM_HISTORIAS, ReglasSubventanas.detectar(v, todas))
    }

    @Test
    fun instagramFilaDeHistoriasDelInicioNoSeBloquea() {
        val v = vista("com.instagram.android", ids = setOf("reels_tray_container", "feed_tab"), seleccionados = setOf("feed_tab", "Inicio"))
        assertNull(ReglasSubventanas.detectar(v, todas))
    }

    @Test
    fun instagramPestanaReels() {
        val v = vista("com.instagram.android", seleccionados = setOf("clips_tab", "Reels"))
        assertEquals(Subventana.INSTAGRAM_REELS, ReglasSubventanas.detectar(v, todas))
    }

    @Test
    fun instagramReelAbiertoDesdeUnChat() {
        val v = vista("com.instagram.android", ids = setOf("clips_viewer_view_pager"))
        assertEquals(Subventana.INSTAGRAM_REELS, ReglasSubventanas.detectar(v, todas))
    }

    @Test
    fun soloBloqueaLasSeccionesMarcadas() {
        val reels = vista("com.instagram.android", seleccionados = setOf("Reels"))
        assertNull(ReglasSubventanas.detectar(reels, setOf(Subventana.INSTAGRAM_HISTORIAS)))
        val novedades = vista("com.whatsapp", seleccionados = setOf("Novedades"))
        assertNull(ReglasSubventanas.detectar(novedades, emptySet()))
    }

    @Test
    fun noMezclaApps() {
        // Un id de Instagram dentro de WhatsApp no significa nada.
        val v = vista("com.whatsapp", ids = setOf("reel_viewer_root"))
        assertNull(ReglasSubventanas.detectar(v, todas))
    }

    @Test
    fun idCortoQuitaElPaquete() {
        assertEquals("clips_tab", ReglasSubventanas.idCorto("com.instagram.android:id/clips_tab"))
        assertEquals("sin_prefijo", ReglasSubventanas.idCorto("sin_prefijo"))
    }
}
