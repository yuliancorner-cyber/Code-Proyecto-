package com.jarvis.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// TABLAS de la base de datos. Cada "data class" con @Entity es una tabla y
// cada propiedad una columna.
//
// Convenciones:
//  - Fechas: "epochDay" (Long) = dias desde 1970. LocalDate.ofEpochDay(n).
//  - Horas:  minutos desde medianoche (Int). 5:15 AM = 315.
//  - Instantes exactos: milisegundos desde 1970 (Long).

/**
 * Una tarea. Sigue la "intencion de implementacion" de James Clear:
 * "I will [BEHAVIOR] at [TIME] in [LOCATION]" -> titulo + fecha/hora + lugar.
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** QUE voy a hacer ("Call the bank"). */
    val title: String,
    val notes: String = "",
    /** CUANDO: dia (epochDay) y hora (minutos). Ambos opcionales. */
    val dueDate: Long? = null,
    val dueMinutes: Int? = null,
    /** DONDE lo voy a hacer. */
    val location: String = "",
    /** Regla de los dos minutos: el primer paso ridiculamente pequeno. */
    val firstStep: String = "",
    /** 0 = sin prioridad, 1 = baja, 2 = media, 3 = alta. */
    val priority: Int = 0,
    /** Proyecto o area a la que pertenece (texto libre). */
    val project: String = "",
    /** Nombre de [com.jarvis.app.logic.Repeat]. */
    val repeat: String = "NONE",
    /** Dias de la semana para la repeticion semanal (mascara de bits). */
    val repeatDays: Int = 0,
    /** Recordatorio: minutos ANTES de la hora. null = sin recordatorio. */
    val reminderBefore: Int? = null,
    val done: Boolean = false,
    val doneAt: Long? = null,
    /** Si al completarla se creo la siguiente repeticion, su id (para deshacer). */
    val spawnedId: Long? = null,
    val createdAt: Long = 0
)

/** Un paso dentro de una tarea (lista de comprobacion). */
@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            // CASCADE: al borrar la tarea se borran solos sus pasos.
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId")]
)
data class Subtask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val title: String,
    val done: Boolean = false,
    val position: Int = 0
)

/**
 * Un habito de la cuadricula mensual (como tu hoja de calculo).
 *
 * Tambien guarda su intencion de implementacion. Hay dos formas ([cue]):
 *  - "TIME":  "I will [behavior] at [time] in [location]".
 *  - "STACK": "After [anchor], I will [behavior]" (apilar habitos).
 */
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Nombre corto para la cuadricula ("Reading"). */
    val name: String,
    val emoji: String = "",
    /** La accion concreta ("read 10 pages"). Opcional. */
    val behavior: String = "",
    val cue: String = CUE_TIME,
    val timeMinutes: Int? = null,
    val location: String = "",
    /** Habito ancla para apilar ("I brush my teeth"). */
    val anchor: String = "",
    /** Version de dos minutos ("read one page"). */
    val tinyVersion: String = "",
    /** Dias en que toca (mascara de bits, ver Days). */
    val days: Int = 127,
    /** Veces al dia para darlo por hecho (1 = casilla normal; agua = 8 vasos). */
    val target: Int = 1,
    val unit: String = "",
    /** Hora del recordatorio diario (minutos). null = sin recordatorio. */
    val reminderMinutes: Int? = null,
    val position: Int = 0,
    val archived: Boolean = false,
    /**
     * Si JARVIS pregunta por este habito en los check-ins del dia.
     * Columna nueva en la version 2 de la base de datos (ver AppDatabase).
     */
    @ColumnInfo(defaultValue = "1")
    val askInCheckIns: Boolean = true
) {
    companion object {
        const val CUE_TIME = "TIME"
        const val CUE_STACK = "STACK"
    }
}

/** Una casilla marcada: el habito [habitId] se hizo [count] veces el dia [date]. */
@Entity(
    tableName = "habit_checks",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HabitCheck(
    val habitId: Long,
    val date: Long,
    val count: Int = 1
)

/** Un paso de la rutina de la manana ("Make your bed"). */
@Entity(tableName = "routine_steps")
data class RoutineStep(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val position: Int = 0
)

/** El paso [stepId] de la rutina se hizo el dia [date]. */
@Entity(
    tableName = "routine_checks",
    primaryKeys = ["stepId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = RoutineStep::class,
            parentColumns = ["id"],
            childColumns = ["stepId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RoutineCheck(
    val stepId: Long,
    val date: Long
)

/**
 * Una nota del "segundo cerebro". [category] sigue el metodo PARA de Tiago
 * Forte (ver [com.jarvis.app.logic.NoteCategory]); lo recien capturado entra
 * en INBOX y se organiza despues.
 */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val body: String = "",
    /** La idea clave en una linea (el paso "Distill" del metodo CODE). */
    val takeaway: String = "",
    val category: String = "INBOX",
    val pinned: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)
