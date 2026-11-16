package com.jarvis.app.logic

import com.jarvis.app.data.Task
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Prioridad de una tarea. Se guarda su [level] (0..3). */
enum class Priority(val level: Int, val label: String) {
    NONE(0, "None"),
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High");

    companion object {
        fun of(level: Int): Priority = entries.firstOrNull { it.level == level } ?: NONE
    }
}

/** Grupos en que se muestra la lista de tareas. */
enum class TaskSection(val title: String) {
    OVERDUE("Overdue"),
    TODAY("Today"),
    TOMORROW("Tomorrow"),
    WEEK("Next 7 days"),
    LATER("Later"),
    SOMEDAY("No date"),
    DONE("Completed")
}

/** Opciones de recordatorio: minutos antes de la hora de la tarea. */
object ReminderOptions {
    val ALL: List<Pair<Int?, String>> = listOf(
        null to "No reminder",
        0 to "At time of task",
        5 to "5 minutes before",
        10 to "10 minutes before",
        30 to "30 minutes before",
        60 to "1 hour before",
        1440 to "1 day before"
    )

    fun label(minutes: Int?): String =
        ALL.firstOrNull { it.first == minutes }?.second ?: "$minutes minutes before"
}

/** Ordenar, agrupar y filtrar tareas. Logica pura, probada con tests. */
object TaskLists {

    /** Si una tarea tiene fecha pero no hora, su recordatorio usa las 9:00 AM. */
    val DEFAULT_TIME: LocalTime = LocalTime.of(9, 0)

    /** Orden: primero por fecha y hora (sin fecha al final), luego prioridad alta primero. */
    val ORDER: Comparator<Task> = compareBy<Task>(
        { it.dueDate ?: Long.MAX_VALUE },
        { it.dueMinutes ?: Int.MAX_VALUE },
        { -it.priority },
        { it.createdAt },
        { it.id }
    )

    fun sectionOf(task: Task, today: LocalDate): TaskSection {
        if (task.done) return TaskSection.DONE
        val due = task.dueDate ?: return TaskSection.SOMEDAY
        val t = today.toEpochDay()
        return when {
            due < t -> TaskSection.OVERDUE
            due == t -> TaskSection.TODAY
            due == t + 1 -> TaskSection.TOMORROW
            due <= t + 7 -> TaskSection.WEEK
            else -> TaskSection.LATER
        }
    }

    /** Tareas agrupadas por seccion (solo secciones con algo), ya ordenadas. */
    fun grouped(tasks: List<Task>, today: LocalDate, includeDone: Boolean = false): List<Pair<TaskSection, List<Task>>> {
        val bySection = tasks.groupBy { sectionOf(it, today) }
        return TaskSection.entries
            .filter { includeDone || it != TaskSection.DONE }
            .mapNotNull { section ->
                val list = bySection[section] ?: return@mapNotNull null
                val sorted = if (section == TaskSection.DONE) {
                    list.sortedByDescending { it.doneAt ?: 0 }
                } else {
                    list.sortedWith(ORDER)
                }
                section to sorted
            }
    }

    /** Pendientes de hoy: lo de hoy y lo atrasado. */
    fun pendingToday(tasks: List<Task>, today: LocalDate): List<Task> =
        tasks.filter { !it.done && it.dueDate != null && it.dueDate <= today.toEpochDay() }
            .sortedWith(ORDER)

    /** Tareas completadas hoy (para el progreso del dia). */
    fun doneToday(tasks: List<Task>, today: LocalDate, zone: ZoneId): List<Task> =
        tasks.filter { task ->
            task.done && task.doneAt != null &&
                Instant.ofEpochMilli(task.doneAt).atZone(zone).toLocalDate() == today
        }.sortedBy { it.doneAt }

    /** Momento en que debe sonar el recordatorio de una tarea, o null si no tiene. */
    fun reminderAt(task: Task): LocalDateTime? {
        if (task.done) return null
        val before = task.reminderBefore ?: return null
        val date = task.dueDate ?: return null
        val time = task.dueMinutes?.let { LocalTime.of(it / 60, it % 60) } ?: DEFAULT_TIME
        return LocalDate.ofEpochDay(date).atTime(time).minusMinutes(before.toLong())
    }

    /** Busca en titulo, notas, lugar y proyecto. */
    fun matches(task: Task, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return listOf(task.title, task.notes, task.location, task.project, task.firstStep)
            .any { it.contains(q, ignoreCase = true) }
    }
}
