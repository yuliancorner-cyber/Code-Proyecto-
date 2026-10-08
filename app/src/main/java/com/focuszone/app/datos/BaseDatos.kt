package com.focuszone.app.datos

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * La base de datos de la app: un archivo en el almacenamiento privado del
 * celular ("focuszone.db"). Sobrevive a cerrar la app y a reiniciar el celular;
 * solo se borra si desinstalas la app o borras sus datos.
 *
 * Versiones:
 *  1 - tabla "sesiones" (Fase 1).
 *  2 - nueva tabla "desbloqueos" (Fase 3b).
 *
 * Cada vez que cambian las tablas se sube la version y se escribe una
 * "migracion": instrucciones para pasar los datos de la version vieja a la
 * nueva sin perder nada (tus sesiones guardadas siguen ahi).
 */
@Database(
    entities = [SesionRegistro::class, DesbloqueoRegistro::class],
    version = 2,
    exportSchema = false
)
abstract class BaseDatos : RoomDatabase() {

    abstract fun sesionDao(): SesionDao
    abstract fun desbloqueoDao(): DesbloqueoDao

    companion object {
        // @Volatile: garantiza que todos los hilos vean la misma instancia.
        @Volatile
        private var instancia: BaseDatos? = null

        /**
         * Devuelve LA base de datos (siempre la misma, se crea una sola vez).
         * Abrir varias a la vez desperdicia memoria y puede dar conflictos.
         */
        fun obtener(context: Context): BaseDatos =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    BaseDatos::class.java,
                    "focuszone.db"
                )
                    .addMigrations(MIGRACION_1_2)
                    .build()
                    .also { instancia = it }
            }

        /**
         * De la version 1 a la 2: solo se crea la tabla nueva. Las columnas deben
         * coincidir EXACTAMENTE con DesbloqueoRegistro, o Room se niega a abrir
         * la base de datos (lo comprueba al arrancar).
         */
        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `desbloqueos` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`inicioMillis` INTEGER NOT NULL, " +
                        "`minutos` INTEGER NOT NULL)"
                )
            }
        }
    }
}
