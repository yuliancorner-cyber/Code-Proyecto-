package com.jarvis.app.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Eleccion de la voz grave y masculina entre las que tenga el celular. */
class VoicePickerTest {

    private fun voice(name: String, country: String, network: Boolean = false, installed: Boolean = true, language: String = "en") =
        VoiceOption(name, language, country, network, installed)

    // Un surtido parecido al del motor de voz de Google.
    private val google = listOf(
        voice("en-us-x-sfg-local", "US"),
        voice("en-us-x-iom-local", "US"),
        voice("en-us-x-iom-network", "US", network = true),
        voice("en-gb-x-fis-local", "GB"),
        voice("en-gb-x-rjs-local", "GB"),
        voice("en-gb-x-rjs-network", "GB", network = true),
        voice("en-au-x-aud-local", "AU"),
        voice("es-us-x-sfb-local", "US", language = "es")
    )

    @Test
    fun `short ids and gender from the name`() {
        assertEquals("rjs", VoicePicker.shortId("en-gb-x-rjs-local"))
        assertEquals("sfg", VoicePicker.shortId("en-us-x-sfg#male_1-local"))
        assertEquals("", VoicePicker.shortId("English United States"))

        assertEquals(VoiceGender.MALE, VoicePicker.gender("en-gb-x-rjs-local"))
        assertEquals(VoiceGender.FEMALE, VoicePicker.gender("en-us-x-sfg-local"))
        // El nombre manda sobre el codigo, y "female" no cuenta como "male".
        assertEquals(VoiceGender.MALE, VoicePicker.gender("en-us-x-sfg#male_1-local"))
        assertEquals(VoiceGender.FEMALE, VoicePicker.gender("en-gb-x-rjs#female_2-local"))
        assertEquals(VoiceGender.UNKNOWN, VoicePicker.gender("en-us-x-iom-local"))
        assertEquals(VoiceGender.UNKNOWN, VoicePicker.gender("Samantha"))
    }

    @Test
    fun `automatic picks the confirmed british male voice that works offline`() {
        assertEquals("en-gb-x-rjs-local", VoicePicker.choose(google, "")?.name)
    }

    @Test
    fun `without the british voice it falls back to another male voice`() {
        val noBritish = google.filterNot { it.country == "GB" }
        assertEquals("en-us-x-iom-local", VoicePicker.choose(noBritish, "")?.name)
    }

    @Test
    fun `a voice tagged male beats untagged ones`() {
        val voices = listOf(voice("en-us-x-sfg-local", "US"), voice("en-in-x-abc#male_1-local", "IN"))
        assertEquals("en-in-x-abc#male_1-local", VoicePicker.choose(voices, "")?.name)
    }

    @Test
    fun `female voices are the last resort but still better than silence`() {
        val onlyFemale = listOf(voice("en-us-x-sfg-local", "US"), voice("en-gb-x-fis-local", "GB"))
        assertEquals("en-gb-x-fis-local", VoicePicker.choose(onlyFemale, "")?.name)
    }

    @Test
    fun `offline voices come before online ones`() {
        val voices = listOf(voice("en-gb-x-rjs-network", "GB", network = true), voice("en-us-x-sfg-local", "US"))
        assertEquals("en-us-x-sfg-local", VoicePicker.choose(voices, "")?.name)
        // Si solo hay voces con internet, mejor esa que ninguna.
        assertEquals("en-gb-x-rjs-network", VoicePicker.choose(voices.take(1), "")?.name)
    }

    @Test
    fun `the voice chosen by the user wins while it exists`() {
        assertEquals("en-us-x-sfg-local", VoicePicker.choose(google, "en-us-x-sfg-local")?.name)
        // Si esa voz ya no esta (se desinstalo), vuelve a la automatica.
        assertEquals("en-gb-x-rjs-local", VoicePicker.choose(google, "en-xx-x-gone-local")?.name)
    }

    @Test
    fun `voices that are not installed or not english are ignored`() {
        val voices = listOf(
            voice("en-gb-x-rjs-local", "GB", installed = false),
            voice("es-es-x-eed-local", "ES", language = "es")
        )
        assertNull(VoicePicker.choose(voices, ""))
        assertNull(VoicePicker.choose(voices, "en-gb-x-rjs-local"))
        assertTrue(VoicePicker.ranked(voices).isEmpty())
        assertNull(VoicePicker.choose(emptyList(), ""))
    }

    @Test
    fun `three letter language codes also count as english`() {
        val voices = listOf(voice("eng-USA-male", "USA", language = "eng"))
        assertEquals("eng-USA-male", VoicePicker.choose(voices, "")?.name)
    }

    @Test
    fun `ranking lists every usable english voice once`() {
        val ranked = VoicePicker.ranked(google)
        assertEquals(7, ranked.size)
        assertEquals("en-gb-x-rjs-local", ranked.first().name)
        assertTrue(ranked.takeLast(2).all { it.needsNetwork })
    }

    @Test
    fun `labels for the settings list`() {
        assertEquals("British · Male · rjs", VoicePicker.label(voice("en-gb-x-rjs-local", "GB")))
        assertEquals("American · Female · sfg", VoicePicker.label(voice("en-us-x-sfg-local", "US")))
        assertEquals("American · iom · needs internet", VoicePicker.label(voice("en-us-x-iom-network", "US", network = true)))
        assertEquals("English (NG) · tfn", VoicePicker.label(voice("en-ng-x-tfn-local", "NG")))
        assertEquals("English · Custom Voice", VoicePicker.label(voice("Custom Voice", "")))
    }

    @Test
    fun `the default tone is deep`() {
        assertEquals("Deep", VoicePicker.pitchLabel(com.jarvis.app.data.Settings().voicePitch))
        assertEquals("Normal", VoicePicker.pitchLabel(VoicePicker.PITCH_NORMAL))
        assertEquals("Very deep", VoicePicker.pitchLabel(VoicePicker.PITCH_VERY_DEEP))
        assertTrue(VoicePicker.PITCH_VERY_DEEP < VoicePicker.PITCH_DEEP && VoicePicker.PITCH_DEEP < VoicePicker.PITCH_NORMAL)
    }
}
