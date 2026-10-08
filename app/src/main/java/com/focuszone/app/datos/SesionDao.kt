package com.focuszone.app.datos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO = "Data Access Object": la lista de operaciones que se pueden hacer con
 * la tabla de sesiones. Room escribe el codigo real de cada una por nosotros.
 *
 * - "suspend" = la operacion se ejecuta sin congelar la pantalla.
 * - "Flow" = un valor que se actualiza solo: si se guarda una sesion nueva,
 *   la pantalla que muestra el total de creditos se refresca automaticamente.
 */
@Dao
interface SesionDao {

    @Insert
    suspend fun insertar(sesion: SesionRegistro): Long

    /** Total de minutos de pantalla ganados en toda la historia. */
    @Query("SELECT COALESCE(SUM(creditosMin), 0) FROM sesiones WHERE completada = 1")
    fun totalCreditos(): Flow<Int>

    /** Minutos de pantalla ganados desde [desdeMillis] (normalmente, desde las 00:00 de hoy). */
    @Query(
        "SELECT COALESCE(SUM(creditosMin), 0) FROM sesiones " +
            "WHERE completada = 1 AND finMillis >= :desdeMillis"
    )
    fun creditosDesde(desdeMillis: Long): Flow<Int>

    /** Numero de sesiones completadas. */
    @Query("SELECT COUNT(*) FROM sesiones WHERE completada = 1")
    fun totalCompletadas(): Flow<Int>
}
