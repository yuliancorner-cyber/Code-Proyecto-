package com.focuszone.app.datos

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una fila de la tabla "desbloqueos": cada vez que gastas saldo para usar
 * tus apps distractoras un rato.
 *
 * Sirve para calcular cuanto has gastado hoy, y en la Fase 4 para las
 * estadisticas (cuanto estudias vs. cuanto gastas).
 */
@Entity(tableName = "desbloqueos")
data class DesbloqueoRegistro(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** Cuando empezo el desbloqueo (milisegundos desde 1970). */
    val inicioMillis: Long,

    /** Minutos gastados. */
    val minutos: Int
)
