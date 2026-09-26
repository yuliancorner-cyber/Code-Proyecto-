package com.focuszone.app.datos

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * La base de datos de la app: un archivo en el almacenamiento privado del
 * celular ("focuszone.db"). Sobrevive a cerrar la app y a reiniciar el celular;
 * solo se borra si desinstalas la app o borras sus datos.
 *
 * version = 1: si en el futuro cambiamos las tablas, subiremos este numero y
 * escribiremos una "migracion" para no perder tus datos.
 */
@Database(entities = [SesionRegistro::class], version = 1, exportSchema = false)
abstract class BaseDatos : RoomDatabase() {

    abstract fun sesionDao(): SesionDao

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
                ).build().also { instancia = it }
            }
    }
}
