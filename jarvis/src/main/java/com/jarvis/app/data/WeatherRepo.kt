package com.jarvis.app.data

import android.content.Context
import com.jarvis.app.logic.City
import com.jarvis.app.logic.Weather
import com.jarvis.app.logic.WeatherApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Descarga el clima de Open-Meteo y lo guarda en el celular ("cache"), para
 * no pedirlo a internet cada vez y para tener algo que mostrar sin conexion.
 *
 * No usa GPS ni ningun permiso de ubicacion: solo las coordenadas de la
 * ciudad que elegiste en los ajustes.
 */
class WeatherRepo(context: Context, private val settings: SettingsStore) {
    private val prefs = context.applicationContext.getSharedPreferences("weather", Context.MODE_PRIVATE)

    private val state = MutableStateFlow(readCache())

    /** El ultimo clima conocido de la ciudad actual (null si no hay). */
    val flow: StateFlow<Weather?> = state

    /**
     * Devuelve el clima de la ciudad elegida.
     *  - Si lo guardado tiene menos de [maxAgeMinutes], lo devuelve sin usar internet.
     *  - Si no, lo descarga; y si falla, devuelve lo guardado siempre que sea
     *    de las ultimas [staleHours] horas (mejor un dato de hace un rato que nada).
     */
    suspend fun get(maxAgeMinutes: Int = 30, staleHours: Int = 3): Weather? {
        val city = settings.value.city ?: return null
        val now = System.currentTimeMillis()
        val cached = readCache()
        if (cached != null && now - cached.fetchedAt < maxAgeMinutes * 60_000L) {
            state.value = cached
            return cached
        }
        val fresh = try {
            val json = download(WeatherApi.forecastUrl(city.lat, city.lon))
            WeatherApi.parseForecast(json, now).also { saveCache(json, now, city) }
        } catch (e: Exception) {
            null
        }
        val result = fresh ?: cached?.takeIf { now - it.fetchedAt < staleHours * 3_600_000L }
        state.value = result
        return result
    }

    /** Busca ciudades por nombre. Lanza una excepcion si no hay conexion. */
    suspend fun searchCities(query: String): List<City> {
        if (query.trim().length < 2) return emptyList()
        return WeatherApi.parseCities(download(WeatherApi.geocodeUrl(query)))
    }

    /** Al cambiar de ciudad lo guardado ya no sirve. */
    fun clear() {
        prefs.edit().clear().apply()
        state.value = null
    }

    private fun cacheKey(city: City) = "${city.lat},${city.lon}"

    private fun readCache(): Weather? {
        val city = settings.value.city ?: return null
        if (prefs.getString("city", null) != cacheKey(city)) return null
        val json = prefs.getString("json", null) ?: return null
        return try {
            WeatherApi.parseForecast(json, prefs.getLong("fetchedAt", 0))
        } catch (e: Exception) {
            null
        }
    }

    private fun saveCache(json: String, fetchedAt: Long, city: City) {
        prefs.edit()
            .putString("json", json)
            .putLong("fetchedAt", fetchedAt)
            .putString("city", cacheKey(city))
            .apply()
    }

    /** Descarga una direccion web y devuelve el texto. Con limites de tiempo cortos. */
    private suspend fun download(url: String): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 6_000
            connection.readTimeout = 6_000
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode !in 200..299) {
                throw java.io.IOException("HTTP ${connection.responseCode}")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
