package com.jarvis.app.alarm

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.jarvis.app.data.Settings
import com.jarvis.app.logic.VoiceOption
import com.jarvis.app.logic.VoicePicker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Como debe sonar JARVIS: velocidad, tono y voz. */
data class VoiceStyle(
    val rate: Float = 1.0f,
    /** Por debajo de 1.0 la voz suena mas grave. */
    val pitch: Float = VoicePicker.PITCH_DEEP,
    /** Nombre de la voz elegida, o "" para la automatica (grave y masculina). */
    val voiceName: String = ""
) {
    companion object {
        fun from(settings: Settings) = VoiceStyle(settings.speechRate, settings.voicePitch, settings.voiceName)
    }
}

/**
 * La voz de JARVIS: envuelve el "texto a voz" (TextToSpeech) de Android.
 * Funciona sin internet y habla en ingles con voz grave de hombre:
 *  - usa el motor de voz de Google si esta instalado (es el que trae voces
 *    masculinas en ingles); si no, el motor por defecto del celular;
 *  - elige la voz masculina disponible (ver VoicePicker) o la que hayas
 *    escogido en Ajustes;
 *  - baja el tono ("pitch") para que suene grave.
 *
 * @param asAlarm true = suena por el volumen de ALARMA (para el despertador,
 *        se oye aunque el volumen multimedia este en cero); false = volumen
 *        multimedia normal.
 */
class Speaker(context: Context, private val asAlarm: Boolean) {

    private val appContext = context.applicationContext

    // El motor tarda un momento en arrancar: aqui se anota cuando esta listo.
    private val ready = CompletableDeferred<Boolean>()

    // Frases en curso: id -> "promesa" que se cumple cuando termina de decirla.
    private val pending = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    @Volatile
    private var configured = false

    // Si el motor pedido no esta disponible, Android usa solo el del sistema.
    private val tts: TextToSpeech = TextToSpeech(
        appContext,
        { status -> ready.complete(status == TextToSpeech.SUCCESS) },
        preferredEngine()
    )

    /** El motor de Google, si esta instalado en el celular. */
    private fun preferredEngine(): String? =
        try {
            appContext.packageManager.getPackageInfo(GOOGLE_TTS, 0)
            GOOGLE_TTS
        } catch (e: Exception) {
            null
        }

    /**
     * Dice las frases una tras otra y espera a que termine.
     * @return false si el celular no tiene motor de voz disponible.
     */
    suspend fun speak(sentences: List<String>, style: VoiceStyle = VoiceStyle()): Boolean {
        if (!awaitReady()) return false
        configure()
        applyStyle(style)
        try {
            for (sentence in sentences) {
                if (sentence.isBlank()) continue
                val id = UUID.randomUUID().toString()
                val done = CompletableDeferred<Unit>()
                pending[id] = done
                val params = Bundle()
                val result = tts.speak(sentence, TextToSpeech.QUEUE_ADD, params, id)
                if (result != TextToSpeech.SUCCESS) {
                    pending.remove(id)
                    return false
                }
                // Tope por frase, por si el motor de voz deja de responder.
                withTimeoutOrNull(SENTENCE_TIMEOUT_MS) { done.await() }
                pending.remove(id)
            }
        } finally {
            // Si cancelan a mitad (p. ej. al apagar la alarma), que se calle ya.
            if (pending.isNotEmpty()) stop()
        }
        return true
    }

    /** Las voces en ingles disponibles, de mas a menos "grave masculina". */
    suspend fun englishVoices(): List<VoiceOption> {
        if (!awaitReady()) return emptyList()
        return VoicePicker.ranked(allVoices())
    }

    /** Se calla inmediatamente. */
    fun stop() {
        try {
            tts.stop()
        } catch (e: Exception) {
            // El motor ya estaba cerrado.
        }
        val waiting = pending.values.toList()
        pending.clear()
        waiting.forEach { it.complete(Unit) }
    }

    /** Libera el motor de voz. Llamar cuando ya no se va a usar. */
    fun shutdown() {
        stop()
        try {
            tts.shutdown()
        } catch (e: Exception) {
            // Nada que hacer.
        }
    }

    private suspend fun awaitReady(): Boolean =
        withTimeoutOrNull(INIT_TIMEOUT_MS) { ready.await() } ?: false

    /** Lo que el motor instalado ofrece, traducido a datos simples. */
    private fun allVoices(): List<VoiceOption> =
        try {
            tts.voices.orEmpty().map { voice ->
                VoiceOption(
                    name = voice.name,
                    language = voice.locale.language,
                    country = voice.locale.country,
                    needsNetwork = voice.isNetworkConnectionRequired,
                    installed = voice.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true
                )
            }
        } catch (e: Exception) {
            // Algunos motores fallan al listar sus voces.
            emptyList()
        }

    /** Velocidad, tono grave y voz masculina (o la elegida). Se aplica en cada lectura. */
    private fun applyStyle(style: VoiceStyle) {
        tts.setSpeechRate(style.rate.coerceIn(0.5f, 2.0f))
        tts.setPitch(style.pitch.coerceIn(0.5f, 1.5f))

        val choice = VoicePicker.choose(allVoices(), style.voiceName)
        val voice = if (choice == null) null else try {
            tts.voices?.firstOrNull { it.name == choice.name }
        } catch (e: Exception) {
            null
        }
        val applied = voice != null && tts.setVoice(voice) == TextToSpeech.SUCCESS
        if (!applied) {
            // Sin lista de voces: ingles de EE. UU.; si el celular no lo tiene,
            // cualquier ingles; y si tampoco, la voz por defecto.
            for (locale in listOf(Locale.US, Locale.UK, Locale.ENGLISH)) {
                val result = tts.setLanguage(locale)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) break
            }
        }
    }

    private fun configure() {
        if (configured) return
        configured = true

        tts.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(if (asAlarm) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = finish(utteranceId)

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finish(utteranceId)
            override fun onError(utteranceId: String?, errorCode: Int) = finish(utteranceId)
            override fun onStop(utteranceId: String?, interrupted: Boolean) = finish(utteranceId)

            private fun finish(id: String?) {
                if (id != null) pending.remove(id)?.complete(Unit)
            }
        })
    }

    private companion object {
        const val GOOGLE_TTS = "com.google.android.tts"
        const val INIT_TIMEOUT_MS = 8_000L
        const val SENTENCE_TIMEOUT_MS = 60_000L
    }
}
