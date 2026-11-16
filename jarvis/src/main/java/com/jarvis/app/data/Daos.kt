package com.jarvis.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Un "DAO" es la lista de consultas que se pueden hacer a cada tabla.
// Las funciones que devuelven Flow "avisan" a la pantalla cada vez que los
// datos cambian; las "suspend" se ejecutan una vez, fuera del hilo principal.
//
// Los datos de una persona son pocos (cientos de filas), asi que se leen
// enteros y se filtran/ordenan en Kotlin (carpeta logic/, con tests).

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY id")
    fun observeAll(): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY id")
    suspend fun all(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun byId(id: Long): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<Task>)

    @Update
    suspend fun update(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Query("SELECT * FROM subtasks ORDER BY position, id")
    fun observeSubtasks(): Flow<List<Subtask>>

    @Query("SELECT * FROM subtasks ORDER BY position, id")
    suspend fun allSubtasks(): List<Subtask>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY position, id")
    suspend fun subtasksOf(taskId: Long): List<Subtask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtasks(subtasks: List<Subtask>)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteSubtasksOf(taskId: Long)

    @Query("UPDATE subtasks SET done = :done WHERE id = :id")
    suspend fun setSubtaskDone(id: Long, done: Boolean)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY position, id")
    fun observeAll(): Flow<List<Habit>>

    @Query("SELECT * FROM habits ORDER BY position, id")
    suspend fun all(): List<Habit>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun byId(id: Long): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: Habit): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(habits: List<Habit>)

    @Update
    suspend fun update(habit: Habit)

    @Update
    suspend fun updateAll(habits: List<Habit>)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM habits")
    suspend fun deleteAll()

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM habits")
    suspend fun nextPosition(): Int

    @Query("SELECT * FROM habit_checks")
    fun observeChecks(): Flow<List<HabitCheck>>

    @Query("SELECT * FROM habit_checks")
    suspend fun allChecks(): List<HabitCheck>

    @Query("SELECT * FROM habit_checks WHERE habitId = :habitId AND date = :date")
    suspend fun check(habitId: Long, date: Long): HabitCheck?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCheck(check: HabitCheck)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putChecks(checks: List<HabitCheck>)

    @Query("DELETE FROM habit_checks WHERE habitId = :habitId AND date = :date")
    suspend fun deleteCheck(habitId: Long, date: Long)
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routine_steps ORDER BY position, id")
    fun observeSteps(): Flow<List<RoutineStep>>

    @Query("SELECT * FROM routine_steps ORDER BY position, id")
    suspend fun steps(): List<RoutineStep>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: RoutineStep): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<RoutineStep>)

    @Update
    suspend fun updateSteps(steps: List<RoutineStep>)

    @Query("DELETE FROM routine_steps WHERE id = :id")
    suspend fun deleteStep(id: Long)

    @Query("DELETE FROM routine_steps")
    suspend fun deleteAllSteps()

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM routine_steps")
    suspend fun nextPosition(): Int

    @Query("SELECT * FROM routine_checks WHERE date = :date")
    fun observeChecks(date: Long): Flow<List<RoutineCheck>>

    @Query("SELECT * FROM routine_checks")
    fun observeAllChecks(): Flow<List<RoutineCheck>>

    @Query("SELECT * FROM routine_checks")
    suspend fun allChecks(): List<RoutineCheck>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCheck(check: RoutineCheck)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putChecks(checks: List<RoutineCheck>)

    @Query("DELETE FROM routine_checks WHERE stepId = :stepId AND date = :date")
    suspend fun deleteCheck(stepId: Long, date: Long)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY id")
    fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes ORDER BY id")
    suspend fun all(): List<Note>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun byId(id: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<Note>)

    @Update
    suspend fun update(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM notes")
    suspend fun deleteAll()
}
