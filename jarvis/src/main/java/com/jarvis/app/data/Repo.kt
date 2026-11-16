package com.jarvis.app.data

import android.content.Context
import androidx.room.withTransaction
import com.jarvis.app.logic.HabitStats
import com.jarvis.app.logic.Recurrence
import com.jarvis.app.logic.Repeat
import com.jarvis.app.reminders.ReminderScheduler
import java.time.LocalDate

/**
 * Todas las ACCIONES sobre los datos (crear una tarea, marcar un habito...).
 * Las pantallas y las notificaciones llaman aqui en vez de tocar las tablas
 * directamente, asi las reglas viven en un solo sitio; por ejemplo, que tras
 * cambiar una tarea haya que reprogramar los recordatorios.
 */
class Repo(private val context: Context, private val db: AppDatabase) {
    val tasks: TaskDao = db.taskDao()
    val habits: HabitDao = db.habitDao()
    val routine: RoutineDao = db.routineDao()
    val notes: NoteDao = db.noteDao()

    private fun now() = System.currentTimeMillis()
    private suspend fun syncReminders() = ReminderScheduler.sync(context)

    // ------------------------------------------------------------- Tareas

    /** Crea o actualiza una tarea junto con sus pasos. Devuelve su id. */
    suspend fun saveTask(task: Task, subtasks: List<Subtask>): Long {
        val id = db.withTransaction {
            val id = if (task.id == 0L) {
                tasks.insert(task.copy(createdAt = now()))
            } else {
                tasks.update(task)
                task.id
            }
            tasks.deleteSubtasksOf(id)
            tasks.insertSubtasks(
                subtasks.filter { it.title.isNotBlank() }
                    .mapIndexed { i, s -> s.copy(id = 0, taskId = id, position = i) }
            )
            id
        }
        syncReminders()
        return id
    }

    /** Captura rapida: una tarea solo con titulo (para hoy o sin fecha). */
    suspend fun quickTask(title: String, forToday: Boolean): Long =
        saveTask(
            Task(
                title = title.trim(),
                dueDate = if (forToday) LocalDate.now().toEpochDay() else null
            ),
            emptyList()
        )

    /**
     * Marca o desmarca una tarea. Si se repite, al completarla se crea la
     * siguiente repeticion; al desmarcarla, esa repeticion se borra.
     */
    suspend fun setTaskDone(id: Long, done: Boolean) {
        db.withTransaction {
            val task = tasks.byId(id) ?: return@withTransaction
            if (task.done == done) return@withTransaction
            if (done) {
                var spawned: Long? = null
                val repeat = Repeat.of(task.repeat)
                if (repeat != Repeat.NONE && task.dueDate != null) {
                    val next = Recurrence.next(
                        LocalDate.ofEpochDay(task.dueDate), repeat, task.repeatDays, LocalDate.now()
                    )
                    if (next != null) {
                        val newId = tasks.insert(
                            task.copy(
                                id = 0, dueDate = next.toEpochDay(),
                                done = false, doneAt = null, spawnedId = null, createdAt = now()
                            )
                        )
                        tasks.insertSubtasks(
                            tasks.subtasksOf(task.id).map { it.copy(id = 0, taskId = newId, done = false) }
                        )
                        spawned = newId
                    }
                }
                tasks.update(task.copy(done = true, doneAt = now(), spawnedId = spawned))
            } else {
                val spawnedId = task.spawnedId
                if (spawnedId != null && tasks.byId(spawnedId)?.done == false) {
                    tasks.delete(spawnedId)
                }
                tasks.update(task.copy(done = false, doneAt = null, spawnedId = null))
            }
        }
        syncReminders()
    }

    suspend fun setSubtaskDone(id: Long, done: Boolean) = tasks.setSubtaskDone(id, done)

    suspend fun deleteTask(id: Long) {
        tasks.delete(id)
        syncReminders()
    }

    // ------------------------------------------------------------ Habitos

    suspend fun saveHabit(habit: Habit): Long {
        val id = if (habit.id == 0L) {
            habits.insert(habit.copy(position = habits.nextPosition()))
        } else {
            // El orden se cambia aparte (moveHabit): se conserva el guardado.
            val position = habits.byId(habit.id)?.position ?: habit.position
            habits.update(habit.copy(position = position))
            habit.id
        }
        syncReminders()
        return id
    }

    suspend fun deleteHabit(id: Long) {
        habits.delete(id)
        syncReminders()
    }

    /** Sube ([delta] = -1) o baja (+1) un habito en la lista. */
    suspend fun moveHabit(id: Long, delta: Int) {
        val list = habits.all().toMutableList()
        val from = list.indexOfFirst { it.id == id }
        val to = from + delta
        if (from < 0 || to !in list.indices) return
        list.add(to, list.removeAt(from))
        habits.updateAll(list.mapIndexed { i, h -> h.copy(position = i) })
    }

    /** Fija cuantas veces se hizo el habito ese dia (0 = casilla vacia). */
    suspend fun setHabitCount(habitId: Long, date: Long, count: Int) {
        if (count <= 0) habits.deleteCheck(habitId, date) else habits.putCheck(HabitCheck(habitId, date, count))
        syncReminders()
    }

    /** Casilla: si estaba hecho lo vacia; si no, lo da por hecho (llega a la meta). */
    suspend fun toggleHabit(habit: Habit, date: Long) {
        val count = habits.check(habit.id, date)?.count ?: 0
        setHabitCount(habit.id, date, if (HabitStats.isDone(count, habit.target)) 0 else maxOf(1, habit.target))
    }

    /** Contador (+1 vaso de agua), sin pasar de la meta. */
    suspend fun incrementHabit(habit: Habit, date: Long, delta: Int = 1) {
        val count = habits.check(habit.id, date)?.count ?: 0
        setHabitCount(habit.id, date, (count + delta).coerceIn(0, maxOf(1, habit.target)))
    }

    // ------------------------------------------------- Rutina de la manana

    suspend fun setRoutineDone(stepId: Long, date: Long, done: Boolean) {
        if (done) routine.putCheck(RoutineCheck(stepId, date)) else routine.deleteCheck(stepId, date)
    }

    suspend fun addRoutineStep(title: String) {
        if (title.isBlank()) return
        routine.insertStep(RoutineStep(title = title.trim(), position = routine.nextPosition()))
    }

    suspend fun renameRoutineStep(step: RoutineStep, title: String) {
        if (title.isBlank()) return
        routine.updateSteps(listOf(step.copy(title = title.trim())))
    }

    suspend fun deleteRoutineStep(id: Long) = routine.deleteStep(id)

    suspend fun moveRoutineStep(id: Long, delta: Int) {
        val list = routine.steps().toMutableList()
        val from = list.indexOfFirst { it.id == id }
        val to = from + delta
        if (from < 0 || to !in list.indices) return
        list.add(to, list.removeAt(from))
        routine.updateSteps(list.mapIndexed { i, s -> s.copy(position = i) })
    }

    // -------------------------------------------------------------- Notas

    suspend fun saveNote(note: Note): Long =
        if (note.id == 0L) {
            notes.insert(note.copy(createdAt = now(), updatedAt = now()))
        } else {
            notes.update(note.copy(updatedAt = now()))
            note.id
        }

    /** Captura rapida: una nota a la bandeja de entrada. */
    suspend fun quickNote(text: String): Long = saveNote(Note(body = text.trim()))

    suspend fun deleteNote(id: Long) = notes.delete(id)

    // --------------------------------------------------- Copia de seguridad

    suspend fun exportData(settings: Settings): BackupData = db.withTransaction {
        BackupData(
            settings = settings,
            tasks = tasks.all(),
            subtasks = tasks.allSubtasks(),
            habits = habits.all(),
            habitChecks = habits.allChecks(),
            routineSteps = routine.steps(),
            routineChecks = routine.allChecks(),
            notes = notes.all()
        )
    }

    /** Sustituye TODOS los datos por los de la copia. */
    suspend fun importData(data: BackupData) {
        db.withTransaction {
            // Borrar tareas/habitos/pasos borra en cascada sus pasos y casillas.
            tasks.deleteAll()
            habits.deleteAll()
            routine.deleteAllSteps()
            notes.deleteAll()

            tasks.insertAll(data.tasks)
            tasks.insertSubtasks(data.subtasks)
            habits.insertAll(data.habits)
            habits.putChecks(data.habitChecks)
            routine.insertSteps(data.routineSteps)
            routine.putChecks(data.routineChecks)
            notes.insertAll(data.notes)
        }
        syncReminders()
    }
}
