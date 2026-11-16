package com.jarvis.app.logic

import com.jarvis.app.data.Habit
import com.jarvis.app.data.HabitCheck

/**
 * Indice de las casillas de habitos para consultarlas rapido:
 * "¿cuantas veces se hizo el habito X el dia Y?".
 */
class CheckIndex(checks: List<HabitCheck>) {
    // habitId -> (dia -> veces)
    private val byHabit: Map<Long, Map<Long, Int>> =
        checks.groupBy { it.habitId }.mapValues { (_, list) -> list.associate { it.date to it.count } }

    fun count(habitId: Long, date: Long): Int = byHabit[habitId]?.get(date) ?: 0

    fun isDone(habit: Habit, date: Long): Boolean = HabitStats.isDone(count(habit.id, date), habit.target)

    /** Dias (epochDay) en los que el habito llego a su meta. */
    fun doneDays(habit: Habit): Set<Long> =
        byHabit[habit.id]?.filterValues { HabitStats.isDone(it, habit.target) }?.keys ?: emptySet()
}

/** Avance del dia: cuanto llevas hecho de rutina, tareas y habitos. */
data class DayProgress(
    val routineDone: Int,
    val routineTotal: Int,
    val tasksDone: Int,
    val tasksTotal: Int,
    val habitsDone: Int,
    val habitsTotal: Int
) {
    val done: Int get() = routineDone + tasksDone + habitsDone
    val total: Int get() = routineTotal + tasksTotal + habitsTotal

    /** 0..100. Con nada planeado el dia cuenta como 0 %. */
    val percent: Int get() = if (total == 0) 0 else done * 100 / total
}
