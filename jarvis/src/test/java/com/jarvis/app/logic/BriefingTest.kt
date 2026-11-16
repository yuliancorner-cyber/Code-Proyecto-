package com.jarvis.app.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/** El resumen hablado de la manana y la lectura de la respuesta del clima. */
class BriefingTest {

    private val morning = LocalDateTime.of(2026, 11, 16, 5, 15)

    private val weather = Weather(
        tempC = 14.0, code = 3, maxC = 21.0, minC = 9.7, dayCode = 3, rainProbability = 35, fetchedAt = 0
    )

    private fun input(
        tasks: List<BriefingTask> = emptyList(),
        weather: Weather? = this.weather,
        city: String? = "Bogotá",
        routine: List<String> = listOf("Make your bed", "Do your push-ups", "Organize your space", "Drink a big glass of water"),
        overdue: Int = 0,
        habits: Int = 10,
        name: String = "Julian",
        now: LocalDateTime = morning,
        fahrenheit: Boolean = false
    ) = BriefingInput(name, now, city, weather, fahrenheit, routine, tasks, overdue, habits)

    @Test
    fun `greeting uses the name and the time of day`() {
        assertEquals("Good morning, Julian.", BriefingBuilder.build(input()).greeting)
        assertEquals("Good afternoon, Julian.", BriefingBuilder.build(input(now = morning.withHour(14))).greeting)
        assertEquals("Good evening, Julian.", BriefingBuilder.build(input(now = morning.withHour(21))).greeting)
        assertEquals("Good morning.", BriefingBuilder.build(input(name = "  ")).greeting)
    }

    @Test
    fun `the full morning briefing`() {
        val briefing = BriefingBuilder.build(input())
        assertEquals("It's 5:15 AM on Monday, November 16.", briefing.lines[0])
        assertEquals(
            "Right now it's 14 degrees and overcast in Bogotá. " +
                "Today's high is 21 and the low is 10, with a 35 percent chance of rain.",
            briefing.lines[1]
        )
        assertEquals(
            "First things first: make your bed, do your push-ups, organize your space, and drink a big glass of water.",
            briefing.lines[2]
        )
        assertEquals("You have no tasks scheduled for today.", briefing.lines[3])
        assertEquals("You have 10 habits to keep today.", briefing.lines[4])
        assertEquals(6, briefing.lines.size) // + la frase de cierre
        assertTrue(briefing.spoken.startsWith("Good morning, Julian. It's 5:15 AM"))
    }

    @Test
    fun `weather advice for rain, heat and cold`() {
        assertEquals("Take an umbrella.", WeatherApi.advice(weather.copy(rainProbability = 70)))
        assertEquals("Take an umbrella.", WeatherApi.advice(weather.copy(dayCode = 63, rainProbability = null)))
        assertTrue(WeatherApi.advice(weather.copy(maxC = 33.0))!!.contains("hot"))
        assertTrue(WeatherApi.advice(weather.copy(minC = 2.0))!!.contains("cold"))
        assertNull(WeatherApi.advice(weather))
        assertTrue(BriefingBuilder.build(input(weather = weather.copy(rainProbability = 80))).lines[1].endsWith("Take an umbrella."))
    }

    @Test
    fun `negative temperatures are spoken as minus`() {
        val cold = weather.copy(tempC = -3.2, maxC = 1.0, minC = -6.0, rainProbability = null)
        val line = BriefingBuilder.build(input(weather = cold)).lines[1]
        assertTrue(line, line.startsWith("Right now it's minus 3 degrees"))
        assertTrue(line, line.contains("the low is minus 6."))
    }

    @Test
    fun `fahrenheit conversion and singular degree`() {
        val line = BriefingBuilder.build(input(fahrenheit = true)).lines[1]
        assertTrue(line, line.startsWith("Right now it's 57 degrees"))
        val one = BriefingBuilder.build(input(weather = weather.copy(tempC = 1.0))).lines[1]
        assertTrue(one, one.startsWith("Right now it's 1 degree and"))
    }

    @Test
    fun `weather is skipped without a city and apologised for without data`() {
        val noCity = BriefingBuilder.build(input(city = null))
        assertFalse(noCity.spoken.contains("degrees"))
        val noData = BriefingBuilder.build(input(weather = null))
        assertEquals("I couldn't reach the weather service for Bogotá this time.", noData.lines[1])
    }

    @Test
    fun `no routine line when the routine is empty`() {
        val briefing = BriefingBuilder.build(input(routine = emptyList()))
        assertFalse(briefing.spoken.contains("First things first"))
    }

    @Test
    fun `one task is read in a single sentence`() {
        val briefing = BriefingBuilder.build(input(tasks = listOf(BriefingTask("Call the bank", 15 * 60, "the office"))))
        assertTrue(briefing.lines.contains("You have one task today: call the bank at 3:00 PM in the office."))
    }

    @Test
    fun `several tasks are read in order`() {
        val tasks = listOf(
            BriefingTask("Study calculus", 8 * 60, "the library"),
            BriefingTask("Call the bank", 15 * 60, ""),
            BriefingTask("Buy groceries", null, "")
        )
        val lines = BriefingBuilder.build(input(tasks = tasks)).lines
        val start = lines.indexOf("You have 3 tasks today.")
        assertTrue(start >= 0)
        assertEquals("First, study calculus at 8:00 AM in the library.", lines[start + 1])
        assertEquals("Then, call the bank at 3:00 PM.", lines[start + 2])
        assertEquals("And finally, buy groceries.", lines[start + 3])
    }

    @Test
    fun `long task lists are cut short`() {
        val tasks = (1..8).map { BriefingTask("Task number $it", null, "") }
        val lines = BriefingBuilder.build(input(tasks = tasks)).lines
        assertTrue(lines.contains("You have 8 tasks today."))
        assertTrue(lines.contains("Then, task number 5."))
        assertFalse(lines.any { it.contains("task number 6") })
        assertTrue(lines.contains("Plus 3 more on your list."))
    }

    @Test
    fun `overdue tasks and single habit wording`() {
        val one = BriefingBuilder.build(input(overdue = 1, habits = 1)).lines
        assertTrue(one.contains("You also have one overdue task waiting."))
        assertTrue(one.contains("You have one habit to keep today."))
        val many = BriefingBuilder.build(input(overdue = 4, habits = 0)).lines
        assertTrue(many.contains("You also have 4 overdue tasks waiting."))
        assertFalse(many.any { it.contains("habit") && it.startsWith("You have") })
    }

    @Test
    fun `joining lists with commas and and`() {
        assertEquals("", BriefingBuilder.joinList(emptyList()))
        assertEquals("a", BriefingBuilder.joinList(listOf("a")))
        assertEquals("a and b", BriefingBuilder.joinList(listOf("a", "b")))
        assertEquals("a, b, and c", BriefingBuilder.joinList(listOf("a", "b", "c")))
    }

    // -------------------------------------------------------- Open-Meteo

    @Test
    fun `parses a real forecast response`() {
        // Respuesta real del servicio (recortada).
        val json = """
            {"latitude":4.6045694,"longitude":-74.05252,"timezone":"America/Bogota",
             "current":{"time":"2026-11-15T19:30","interval":900,"temperature_2m":14.0,"weather_code":3},
             "daily":{"time":["2026-11-15"],"weather_code":[61],"temperature_2m_max":[21.0],
                      "temperature_2m_min":[9.7],"precipitation_probability_max":[35]}}
        """.trimIndent()
        val parsed = WeatherApi.parseForecast(json, fetchedAt = 123)
        assertEquals(14.0, parsed.tempC, 0.001)
        assertEquals(3, parsed.code)
        assertEquals(61, parsed.dayCode)
        assertEquals(21.0, parsed.maxC, 0.001)
        assertEquals(9.7, parsed.minC, 0.001)
        assertEquals(35, parsed.rainProbability)
        assertEquals(123L, parsed.fetchedAt)
    }

    @Test
    fun `forecast without rain probability`() {
        val json = """
            {"current":{"temperature_2m":20.5,"weather_code":0},
             "daily":{"temperature_2m_max":[25.0],"temperature_2m_min":[12.0],
                      "precipitation_probability_max":[null]}}
        """.trimIndent()
        val parsed = WeatherApi.parseForecast(json, 0)
        assertNull(parsed.rainProbability)
        assertEquals(0, parsed.dayCode) // sin codigo diario se usa el actual
    }

    @Test(expected = Exception::class)
    fun `a broken forecast response fails loudly`() {
        WeatherApi.parseForecast("""{"error":true,"reason":"bad request"}""", 0)
    }

    @Test
    fun `parses city search results`() {
        val json = """
            {"results":[
              {"name":"Bogotá","latitude":4.60971,"longitude":-74.08175,"country":"Colombia","admin1":"Bogota D.C."},
              {"name":"Bogota","latitude":36.16,"longitude":-89.44,"country":"United States"}
            ]}
        """.trimIndent()
        val cities = WeatherApi.parseCities(json)
        assertEquals(2, cities.size)
        assertEquals("Bogotá, Bogota D.C., Colombia", cities[0].label)
        assertEquals("Bogota, United States", cities[1].label)
        assertEquals(4.60971, cities[0].lat, 0.00001)
        assertTrue(WeatherApi.parseCities("""{"generationtime_ms":0.5}""").isEmpty())
    }

    @Test
    fun `urls are well formed`() {
        assertTrue(WeatherApi.forecastUrl(4.6, -74.08).startsWith("https://api.open-meteo.com/v1/forecast?latitude=4.6&longitude=-74.08&"))
        assertTrue(WeatherApi.geocodeUrl(" São Paulo ").contains("name=S%C3%A3o+Paulo&"))
    }

    @Test
    fun `weather descriptions`() {
        assertEquals("clear sky", WeatherApi.describe(0))
        assertEquals("thunderstorm", WeatherApi.describe(95))
        assertEquals("unsettled weather", WeatherApi.describe(1234))
        assertTrue(WeatherApi.isWet(61))
        assertFalse(WeatherApi.isWet(3))
    }
}
