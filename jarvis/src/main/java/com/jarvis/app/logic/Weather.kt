package com.jarvis.app.logic

import org.json.JSONObject
import java.net.URLEncoder

/** El clima de hoy. Las temperaturas se guardan siempre en grados Celsius. */
data class Weather(
    val tempC: Double,
    /** Codigo WMO del estado actual (0 = despejado, 61 = lluvia...). */
    val code: Int,
    val maxC: Double,
    val minC: Double,
    /** Codigo WMO que resume el dia entero. */
    val dayCode: Int,
    /** Probabilidad maxima de lluvia del dia (0..100), si el servicio la da. */
    val rainProbability: Int?,
    /** Cuando se descargo (milisegundos). */
    val fetchedAt: Long
)

/** Una ciudad elegida a mano (sin GPS). */
data class City(
    val name: String,
    val region: String,
    val country: String,
    val lat: Double,
    val lon: Double
) {
    /** "Bogotá, Bogota D.C., Colombia" */
    val label: String get() = listOf(name, region, country).filter { it.isNotBlank() }.distinct().joinToString(", ")
}

/**
 * Open-Meteo (https://open-meteo.com): servicio de clima gratuito para uso no
 * comercial y SIN clave. Aqui solo se construyen las direcciones y se lee la
 * respuesta; la descarga en si esta en data/WeatherRepo.
 */
object WeatherApi {

    fun forecastUrl(lat: Double, lon: Double): String =
        "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,weather_code" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
            "&timezone=auto&forecast_days=1"

    fun geocodeUrl(query: String): String =
        "https://geocoding-api.open-meteo.com/v1/search" +
            "?name=${URLEncoder.encode(query.trim(), "UTF-8")}&count=10&language=en&format=json"

    /** Lee la respuesta del pronostico. Lanza una excepcion si no tiene el formato esperado. */
    fun parseForecast(json: String, fetchedAt: Long): Weather {
        val root = JSONObject(json)
        val current = root.getJSONObject("current")
        val daily = root.getJSONObject("daily")
        val rain = daily.optJSONArray("precipitation_probability_max")
        return Weather(
            tempC = current.getDouble("temperature_2m"),
            code = current.getInt("weather_code"),
            maxC = daily.getJSONArray("temperature_2m_max").getDouble(0),
            minC = daily.getJSONArray("temperature_2m_min").getDouble(0),
            dayCode = daily.optJSONArray("weather_code")?.optInt(0, current.getInt("weather_code"))
                ?: current.getInt("weather_code"),
            rainProbability = if (rain != null && !rain.isNull(0)) rain.getInt(0) else null,
            fetchedAt = fetchedAt
        )
    }

    /** Lee la respuesta del buscador de ciudades (lista vacia si no hay resultados). */
    fun parseCities(json: String): List<City> {
        val results = JSONObject(json).optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).map { i ->
            val item = results.getJSONObject(i)
            City(
                name = item.getString("name"),
                region = item.optString("admin1", ""),
                country = item.optString("country", ""),
                lat = item.getDouble("latitude"),
                lon = item.getDouble("longitude")
            )
        }
    }

    /** Descripcion en ingles de un codigo WMO, en minusculas ("light rain"). */
    fun describe(code: Int): String = when (code) {
        0 -> "clear sky"
        1 -> "mostly clear"
        2 -> "partly cloudy"
        3 -> "overcast"
        45, 48 -> "foggy"
        51 -> "light drizzle"
        53 -> "drizzle"
        55 -> "heavy drizzle"
        56, 57 -> "freezing drizzle"
        61 -> "light rain"
        63 -> "rain"
        65 -> "heavy rain"
        66, 67 -> "freezing rain"
        71 -> "light snow"
        73 -> "snow"
        75 -> "heavy snow"
        77 -> "snow grains"
        80 -> "light showers"
        81 -> "showers"
        82 -> "violent showers"
        85, 86 -> "snow showers"
        95 -> "thunderstorm"
        96, 99 -> "thunderstorm with hail"
        else -> "unsettled weather"
    }

    /** Un emoji para la tarjeta del clima. */
    fun emoji(code: Int): String = when (code) {
        0, 1 -> "☀️"
        2 -> "⛅"
        3 -> "☁️"
        45, 48 -> "🌫️"
        in 51..67, in 80..82 -> "🌧️"
        in 71..77, 85, 86 -> "❄️"
        95, 96, 99 -> "⛈️"
        else -> "🌡️"
    }

    /** ¿El codigo implica que cae agua del cielo? */
    fun isWet(code: Int): Boolean = code in 51..67 || code in 80..82 || code in 95..99

    /** Un consejo corto segun el clima del dia, o null si no hay nada que decir. */
    fun advice(weather: Weather): String? {
        val rain = weather.rainProbability ?: 0
        return when {
            rain >= 50 || isWet(weather.dayCode) -> "Take an umbrella."
            weather.maxC >= 30 -> "It will be hot, so keep your water close."
            weather.minC <= 5 -> "It will be cold, so dress warmly."
            else -> null
        }
    }
}
