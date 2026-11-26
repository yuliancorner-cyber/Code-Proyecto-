package com.jarvis.app.data

import android.content.Context
import android.content.SharedPreferences
import com.jarvis.app.logic.CheckInLogic
import com.jarvis.app.logic.City
import com.jarvis.app.logic.Days
import com.jarvis.app.logic.VoicePicker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Los ajustes de la app. Los valores por defecto son los de la primera vez. */
data class Settings(
    /** false hasta completar la pantalla de bienvenida. */
    val setupDone: Boolean = false,
    val userName: String = "Julian",

    // Ciudad para el clima: se elige a mano, sin GPS.
    val cityName: String = "",
    val cityRegion: String = "",
    val cityCountry: String = "",
    val cityLat: Double? = null,
    val cityLon: Double? = null,
    val fahrenheit: Boolean = false,

    // Alarma de la manana.
    val alarmEnabled: Boolean = true,
    val alarmHour: Int = 5,
    val alarmMinute: Int = 15,
    val alarmDays: Int = Days.ALL,
    val snoozeMinutes: Int = 10,
    /** Segundos de tono antes de que JARVIS empiece a hablar. */
    val toneSeconds: Int = 20,
    val voiceEnabled: Boolean = true,
    /** Velocidad de la voz (1.0 = normal). */
    val speechRate: Float = 1.0f,
    /** Tono de la voz: por debajo de 1.0 suena mas grave. */
    val voicePitch: Float = VoicePicker.PITCH_DEEP,
    /** Voz elegida a mano en Ajustes; "" = automatica (la mas grave y masculina). */
    val voiceName: String = "",

    // Check-ins: JARVIS pregunta por tus habitos durante el dia.
    val checkInsEnabled: Boolean = true,
    /** Horas de las preguntas, en minutos desde medianoche. */
    val checkInTimes: List<Int> = CheckInLogic.DEFAULT_TIMES
) {
    val city: City?
        get() = if (cityLat != null && cityLon != null && cityName.isNotBlank()) {
            City(cityName, cityRegion, cityCountry, cityLat, cityLon)
        } else null

    val alarmMinutes: Int get() = alarmHour * 60 + alarmMinute
}

/**
 * Guarda los ajustes en SharedPreferences (un archivo pequeno clave-valor) y
 * los expone como StateFlow para que las pantallas se actualicen solas.
 */
class SettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val state = MutableStateFlow(read())
    val flow: StateFlow<Settings> = state

    val value: Settings get() = state.value

    /** Cambia los ajustes: update { it.copy(userName = "Julian") } */
    @Synchronized
    fun update(change: (Settings) -> Settings) {
        val next = change(state.value)
        write(next)
        state.value = next
    }

    /** Reemplaza todo (al importar una copia de seguridad). */
    fun replace(settings: Settings) = update { settings }

    private fun read(): Settings {
        val d = Settings()
        return Settings(
            setupDone = prefs.getBoolean("setupDone", d.setupDone),
            userName = prefs.getString("userName", d.userName) ?: d.userName,
            cityName = prefs.getString("cityName", "") ?: "",
            cityRegion = prefs.getString("cityRegion", "") ?: "",
            cityCountry = prefs.getString("cityCountry", "") ?: "",
            // Las coordenadas se guardan como texto para no perder decimales.
            cityLat = prefs.getString("cityLat", null)?.toDoubleOrNull(),
            cityLon = prefs.getString("cityLon", null)?.toDoubleOrNull(),
            fahrenheit = prefs.getBoolean("fahrenheit", d.fahrenheit),
            alarmEnabled = prefs.getBoolean("alarmEnabled", d.alarmEnabled),
            alarmHour = prefs.getInt("alarmHour", d.alarmHour),
            alarmMinute = prefs.getInt("alarmMinute", d.alarmMinute),
            alarmDays = prefs.getInt("alarmDays", d.alarmDays),
            snoozeMinutes = prefs.getInt("snoozeMinutes", d.snoozeMinutes),
            toneSeconds = prefs.getInt("toneSeconds", d.toneSeconds),
            voiceEnabled = prefs.getBoolean("voiceEnabled", d.voiceEnabled),
            speechRate = prefs.getFloat("speechRate", d.speechRate),
            voicePitch = prefs.getFloat("voicePitch", d.voicePitch),
            voiceName = prefs.getString("voiceName", "") ?: "",
            checkInsEnabled = prefs.getBoolean("checkInsEnabled", d.checkInsEnabled),
            // Se guardan como texto "720,1020,1260". Sin la clave = valores por defecto.
            checkInTimes = prefs.getString("checkInTimes", null)
                ?.let { text -> CheckInLogic.normalize(text.split(',').mapNotNull { it.trim().toIntOrNull() }) }
                ?: d.checkInTimes
        )
    }

    private fun write(s: Settings) {
        prefs.edit()
            .putBoolean("setupDone", s.setupDone)
            .putString("userName", s.userName)
            .putString("cityName", s.cityName)
            .putString("cityRegion", s.cityRegion)
            .putString("cityCountry", s.cityCountry)
            .putString("cityLat", s.cityLat?.toString())
            .putString("cityLon", s.cityLon?.toString())
            .putBoolean("fahrenheit", s.fahrenheit)
            .putBoolean("alarmEnabled", s.alarmEnabled)
            .putInt("alarmHour", s.alarmHour)
            .putInt("alarmMinute", s.alarmMinute)
            .putInt("alarmDays", s.alarmDays)
            .putInt("snoozeMinutes", s.snoozeMinutes)
            .putInt("toneSeconds", s.toneSeconds)
            .putBoolean("voiceEnabled", s.voiceEnabled)
            .putFloat("speechRate", s.speechRate)
            .putFloat("voicePitch", s.voicePitch)
            .putString("voiceName", s.voiceName)
            .putBoolean("checkInsEnabled", s.checkInsEnabled)
            .putString("checkInTimes", s.checkInTimes.joinToString(","))
            .apply()
    }
}
