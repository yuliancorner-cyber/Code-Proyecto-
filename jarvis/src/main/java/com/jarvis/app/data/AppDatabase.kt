package com.jarvis.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * La base de datos de JARVIS: el archivo "jarvis.db" en el almacenamiento
 * privado del celular. Solo se borra si desinstalas la app o borras sus datos.
 *
 * Si algun dia cambian las tablas hay que subir [version] y escribir una
 * "migracion" (ver BaseDatos.kt de FocusZone para un ejemplo).
 */
@Database(
    entities = [
        Task::class, Subtask::class,
        Habit::class, HabitCheck::class,
        RoutineStep::class, RoutineCheck::class,
        Note::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun routineDao(): RoutineDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jarvis.db"
                )
                    .addCallback(SeedCallback)
                    .build()
                    .also { instance = it }
            }

        /**
         * Se ejecuta UNA sola vez, cuando la base de datos se crea (primera
         * vez que abres la app): rellena los habitos y la rutina iniciales.
         */
        private object SeedCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                Seed.HABITS.forEachIndexed { i, h ->
                    db.execSQL(
                        "INSERT INTO habits " +
                            "(name, emoji, behavior, cue, timeMinutes, location, anchor, tinyVersion, " +
                            "days, target, unit, reminderMinutes, position, archived) " +
                            "VALUES (?, ?, '', 'TIME', NULL, '', '', '', 127, ?, ?, NULL, ?, 0)",
                        arrayOf<Any>(h.name, h.emoji, h.target, h.unit, i)
                    )
                }
                Seed.ROUTINE.forEachIndexed { i, title ->
                    db.execSQL(
                        "INSERT INTO routine_steps (title, position) VALUES (?, ?)",
                        arrayOf<Any>(title, i)
                    )
                }
            }
        }
    }
}

/** Datos con los que arranca la app. Todo se puede editar despues desde la app. */
object Seed {
    class SeedHabit(val name: String, val emoji: String, val target: Int = 1, val unit: String = "")

    /**
     * Los 10 habitos de la hoja "Habit Tracker May 2026", en el mismo orden.
     * Water lleva una meta de 8 vasos al dia (se puede volver a 1 = casilla simple).
     */
    val HABITS = listOf(
        SeedHabit("Reading", "📖"),
        SeedHabit("Go to Sleep early", "😴"),
        SeedHabit("Diary", "📓"),
        SeedHabit("Water", "💧", target = 8, unit = "glasses"),
        SeedHabit("Skincare", "🧼"),
        SeedHabit("Stop Watching Porn", "🍆"),
        SeedHabit("Social Media Limit", "✋"),
        SeedHabit("Clean my Room", "🧹"),
        SeedHabit("Good Diet", "🍽️"),
        SeedHabit("Night gratitude", "🙏")
    )

    /** La rutina que JARVIS te recuerda al despertar. */
    val ROUTINE = listOf(
        "Make your bed",
        "Do your push-ups",
        "Organize your space",
        "Drink a big glass of water"
    )
}
