package com.focuszone.app.datos

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una fila de la tabla "sesiones": el registro de UNA sesion terminada.
 *
 * "@Entity" le dice a Room que esta clase es una tabla de la base de datos.
 * Cada propiedad es una columna.
 *
 * Guardamos tanto las completadas como las canceladas: en la Fase 4 las
 * usaremos para las estadisticas (racha, porcentaje de exito, etc.).
 */
@Entity(tableName = "sesiones")
data class SesionRegistro(
    /** Identificador unico. autoGenerate = Room le asigna 1, 2, 3... solo. */
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** Fecha y hora de inicio, en milisegundos desde 1970 (formato estandar). */
    val inicioMillis: Long,

    /** Fecha y hora de fin. */
    val finMillis: Long,

    /** Meta elegida, en minutos (15, 25, 45 o 60). */
    val metaMin: Int,

    /** Segundos realmente estudiados con el celular boca abajo. */
    val estudiadoSeg: Long,

    /** true si se llego a la meta. */
    val completada: Boolean,

    /** Minutos de pantalla ganados (0 si se cancelo). */
    val creditosMin: Int
)
