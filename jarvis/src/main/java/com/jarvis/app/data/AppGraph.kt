package com.jarvis.app.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * "Caja de herramientas" unica de la app: la base de datos, los ajustes, el
 * clima... Se crean una sola vez y se comparten entre pantallas, la alarma y
 * los recordatorios. Se prepara en JarvisApp.onCreate, antes que todo lo demas.
 */
object AppGraph {

    /** Las piezas que dependen del contexto de la app. Se crean juntas en [init]. */
    private class Graph(val appContext: Context) {
        val settings: SettingsStore by lazy { SettingsStore(appContext) }
        val db: AppDatabase by lazy { AppDatabase.create(appContext) }
        val weather: WeatherRepo by lazy { WeatherRepo(appContext, settings) }
        val repo: Repo by lazy { Repo(appContext, db) }

        val data: StateFlow<AppData?> by lazy {
            val tasks = combine(repo.tasks.observeAll(), repo.tasks.observeSubtasks(), ::Pair)
            val habits = combine(repo.habits.observeAll(), repo.habits.observeChecks(), ::Pair)
            val routine = combine(repo.routine.observeSteps(), repo.routine.observeAllChecks(), ::Pair)
            combine(tasks, habits, routine, repo.notes.observeAll()) { t, h, r, notes ->
                AppData(
                    tasks = t.first, subtasks = t.second,
                    habits = h.first, habitChecks = h.second,
                    routineSteps = r.first, routineChecks = r.second,
                    notes = notes
                )
            }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)
        }
    }

    @Volatile
    private lateinit var graph: Graph

    val appContext: Context get() = graph.appContext
    val settings: SettingsStore get() = graph.settings
    val db: AppDatabase get() = graph.db
    val weather: WeatherRepo get() = graph.weather
    val repo: Repo get() = graph.repo

    /**
     * TODOS los datos de la app en un solo paquete que se actualiza solo cada
     * vez que cambia cualquier tabla. Es null mientras se carga por primera vez.
     * Las pantallas leen de aqui y filtran con las funciones de logic/.
     */
    val data: StateFlow<AppData?> get() = graph.data

    /**
     * Para trabajos que deben terminar aunque se cierre la pantalla que los
     * inicio (guardar, reprogramar alarmas...).
     */
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Cola de acciones: se ejecutan de una en una y en el orden en que se
    // pidieron. Sin esto, dos guardados seguidos (p. ej. al escribir rapido)
    // podrian terminar en desorden y quedarse guardado el valor viejo.
    private val actions = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (action in actions) {
                try {
                    action()
                } catch (e: Exception) {
                    Log.e("Jarvis", "Action failed", e)
                }
            }
        }
    }

    /** Pone una accion (guardar, marcar, borrar...) en la cola. */
    fun run(action: suspend () -> Unit) {
        actions.trySend(action)
    }

    /**
     * Prepara todo. En el celular se llama una sola vez por arranque; en las
     * pruebas automaticas, una vez por prueba (cada una con datos limpios).
     */
    fun init(context: Context) {
        val app = context.applicationContext
        if (!::graph.isInitialized || graph.appContext !== app) graph = Graph(app)
    }
}

/** Una "foto" de todos los datos en un momento dado. */
data class AppData(
    val tasks: List<Task> = emptyList(),
    val subtasks: List<Subtask> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val habitChecks: List<HabitCheck> = emptyList(),
    val routineSteps: List<RoutineStep> = emptyList(),
    val routineChecks: List<RoutineCheck> = emptyList(),
    val notes: List<Note> = emptyList()
)
