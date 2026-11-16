package com.jarvis.app.alarm

import com.jarvis.app.data.AppGraph
import com.jarvis.app.logic.Briefing
import com.jarvis.app.logic.BriefingBuilder
import com.jarvis.app.logic.BriefingInput
import com.jarvis.app.logic.BriefingTask
import com.jarvis.app.logic.HabitStats
import com.jarvis.app.logic.TaskLists
import com.jarvis.app.logic.Weather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDateTime

/** Reune los datos de HOY (clima, rutina, tareas, habitos) y arma el resumen. */
object BriefingLoader {

    /** Tiempo maximo esperando al servicio del clima antes de seguir sin el. */
    private const val WEATHER_TIMEOUT_MS = 9_000L

    suspend fun load(now: LocalDateTime = LocalDateTime.now()): Briefing = withContext(Dispatchers.IO) {
        val settings = AppGraph.settings.value
        val repo = AppGraph.repo
        val today = now.toLocalDate()
        val day = today.toEpochDay()

        val city = settings.city
        val weather: Weather? = if (city != null) {
            withTimeoutOrNull(WEATHER_TIMEOUT_MS) { AppGraph.weather.get(maxAgeMinutes = 20) }
        } else null

        val tasks = repo.tasks.all()
        val todayTasks = tasks
            .filter { !it.done && it.dueDate == day }
            .sortedWith(TaskLists.ORDER)
            .map { BriefingTask(it.title, it.dueMinutes, it.location) }
        val overdue = tasks.count { !it.done && it.dueDate != null && it.dueDate < day }

        val habits = repo.habits.all().count { !it.archived && HabitStats.isScheduled(it.days, today) }

        // La rutina solo se recuerda por la manana.
        val routine = if (now.hour < 12) repo.routine.steps().map { it.title } else emptyList()

        BriefingBuilder.build(
            BriefingInput(
                name = settings.userName,
                now = now,
                city = city?.name,
                weather = weather,
                fahrenheit = settings.fahrenheit,
                routine = routine,
                tasks = todayTasks,
                overdue = overdue,
                habits = habits
            )
        )
    }
}

/**
 * Reproduce el resumen cuando tocas "Briefing" en la pantalla de inicio
 * (fuera de la alarma). Usa el volumen multimedia.
 */
object BriefingPlayer {
    private val state = MutableStateFlow(false)

    /** true mientras JARVIS esta hablando. */
    val speaking: StateFlow<Boolean> = state

    /** El ultimo resumen leido, para mostrarlo en pantalla. */
    val last = MutableStateFlow<Briefing?>(null)

    private var speaker: Speaker? = null
    private var job: Job? = null

    /** Empieza a hablar; si ya estaba hablando, se calla. */
    fun toggle() {
        if (state.value) {
            stop()
            return
        }
        state.value = true
        job = AppGraph.scope.launch(Dispatchers.Main) {
            try {
                val briefing = BriefingLoader.load()
                last.value = briefing
                val voice = speaker ?: Speaker(AppGraph.appContext, asAlarm = false).also { speaker = it }
                voice.speak(briefing.all, AppGraph.settings.value.speechRate)
            } finally {
                state.value = false
            }
        }
    }

    fun stop() {
        job?.cancel()
        speaker?.stop()
        state.value = false
    }
}
