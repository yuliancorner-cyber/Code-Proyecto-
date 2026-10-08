package com.focuszone.app.datos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DesbloqueoDao {

    @Insert
    suspend fun insertar(desbloqueo: DesbloqueoRegistro): Long

    /** Todos los desbloqueos, del mas reciente al mas antiguo (para estadisticas). */
    @Query("SELECT * FROM desbloqueos ORDER BY inicioMillis DESC")
    fun todos(): Flow<List<DesbloqueoRegistro>>

    /** Minutos gastados desde [desdeMillis] (normalmente, desde las 00:00 de hoy). */
    @Query("SELECT COALESCE(SUM(minutos), 0) FROM desbloqueos WHERE inicioMillis >= :desdeMillis")
    fun minutosGastadosDesde(desdeMillis: Long): Flow<Int>
}
