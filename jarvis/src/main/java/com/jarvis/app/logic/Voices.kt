package com.jarvis.app.logic

/** Una voz del motor de texto a voz, con solo lo que hace falta para elegir. */
data class VoiceOption(
    /** Nombre interno, p. ej. "en-gb-x-rjs-local". */
    val name: String,
    /** Idioma ("en") y pais ("GB") de la voz. */
    val language: String,
    val country: String,
    /** true si solo funciona con internet. */
    val needsNetwork: Boolean,
    /** false si el paquete de voz aun no esta descargado en el celular. */
    val installed: Boolean
)

enum class VoiceGender { MALE, FEMALE, UNKNOWN }

/**
 * Elige la voz de JARVIS: grave y masculina.
 *
 * Android NO dice si una voz es de hombre o de mujer, asi que hay que
 * deducirlo del nombre:
 *  - algunos nombres lo dicen ("...#male_1-local");
 *  - para las voces de Google se usa su codigo corto ("rjs"). Solo algunos
 *    estan confirmados; el resto de la lista es lo mas probable segun listados
 *    de la comunidad, y por eso en Ajustes se puede escuchar y elegir a mano.
 *
 * El tono grave no depende de esto: se consigue bajando el "pitch" al hablar.
 * Logica pura, probada con tests.
 */
object VoicePicker {

    /** Tonos de voz que ofrece Ajustes (1.0 = el tono natural de la voz). */
    const val PITCH_NORMAL = 1.0f
    const val PITCH_DEEP = 0.8f
    const val PITCH_VERY_DEEP = 0.65f

    // Codigos de voces de Google en ingles.
    private val CONFIRMED_MALE = setOf("rjs")
    private val CONFIRMED_FEMALE = setOf("sfg", "fis")
    private val LIKELY_MALE = setOf("gbd", "gbb", "iom", "iol", "tpd", "aub", "aud", "ene", "end")
    private val LIKELY_FEMALE = setOf("gba", "gbc", "gbg", "iob", "iog", "tpc", "tpf", "aua", "auc", "ena", "enc", "ahp", "cxx")

    fun isEnglish(voice: VoiceOption): Boolean = voice.language.lowercase() in setOf("en", "eng")

    /** "en-gb-x-rjs-local" -> "rjs"; "" si el nombre no sigue ese formato. */
    fun shortId(name: String): String {
        val lower = name.lowercase()
        val start = lower.indexOf("-x-")
        if (start < 0) return ""
        return lower.substring(start + 3).takeWhile { it.isLetterOrDigit() }
    }

    /** Genero del que estamos seguros (lo dice el nombre o esta confirmado). */
    fun gender(name: String): VoiceGender {
        val lower = name.lowercase()
        // "female" contiene "male": hay que mirar primero el mas largo.
        if ("female" in lower) return VoiceGender.FEMALE
        if ("male" in lower) return VoiceGender.MALE
        return when (shortId(name)) {
            in CONFIRMED_MALE -> VoiceGender.MALE
            in CONFIRMED_FEMALE -> VoiceGender.FEMALE
            else -> VoiceGender.UNKNOWN
        }
    }

    /** Cuanto se parece la voz a "hombre con voz grave" (mas alto = mejor). */
    private fun score(voice: VoiceOption): Int {
        val id = shortId(voice.name)
        var score = when {
            gender(voice.name) == VoiceGender.MALE -> 100
            gender(voice.name) == VoiceGender.FEMALE -> 0
            id in LIKELY_MALE -> 80
            id in LIKELY_FEMALE -> 5
            else -> 20
        }
        // Entre iguales: acento britanico (el clasico de JARVIS), luego americano.
        score += when (voice.country.uppercase()) {
            "GB", "GBR" -> 9
            "US", "USA" -> 6
            "AU", "AUS" -> 3
            else -> 0
        }
        return score
    }

    /**
     * Las voces en ingles que se pueden usar, de mejor a peor candidata.
     * Las que necesitan internet van al final: la alarma debe hablar sin conexion.
     */
    fun ranked(voices: List<VoiceOption>): List<VoiceOption> =
        voices.filter { isEnglish(it) && it.installed }
            .sortedWith(
                compareBy<VoiceOption> { it.needsNetwork }
                    .thenByDescending { score(it) }
                    .thenBy { it.name }
            )

    /**
     * La voz a usar.
     * @param preferredName la que eligio el usuario en Ajustes, o "" para automatico.
     * @return null si el celular no tiene ninguna voz en ingles instalada.
     */
    fun choose(voices: List<VoiceOption>, preferredName: String): VoiceOption? {
        val usable = ranked(voices)
        if (preferredName.isNotBlank()) {
            usable.firstOrNull { it.name == preferredName }?.let { return it }
        }
        return usable.firstOrNull()
    }

    /** Texto para la lista de Ajustes: "British · Male · rjs". */
    fun label(voice: VoiceOption): String {
        val accent = when (voice.country.uppercase()) {
            "GB", "GBR" -> "British"
            "US", "USA" -> "American"
            "AU", "AUS" -> "Australian"
            "IN", "IND" -> "Indian"
            "" -> "English"
            else -> "English (${voice.country.uppercase()})"
        }
        val parts = mutableListOf(accent)
        when (gender(voice.name)) {
            VoiceGender.MALE -> parts += "Male"
            VoiceGender.FEMALE -> parts += "Female"
            VoiceGender.UNKNOWN -> Unit
        }
        parts += shortId(voice.name).ifEmpty { voice.name }
        if (voice.needsNetwork) parts += "needs internet"
        return parts.joinToString(" · ")
    }

    fun pitchLabel(pitch: Float): String = when {
        pitch <= 0.7f -> "Very deep"
        pitch < 0.95f -> "Deep"
        else -> "Normal"
    }
}
