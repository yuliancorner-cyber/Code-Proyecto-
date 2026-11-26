package com.jarvis.app.android

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jarvis.app.data.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Al actualizar JARVIS, los datos que ya tenias en el celular deben seguir ahi.
 * Se crea un archivo con la base de datos de la version 1 (las mismas tablas
 * que genero Room para la v0.1) y se abre con la version actual.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val v1Schema = listOf(
        "CREATE TABLE IF NOT EXISTS `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `notes` TEXT NOT NULL, `dueDate` INTEGER, `dueMinutes` INTEGER, `location` TEXT NOT NULL, `firstStep` TEXT NOT NULL, `priority` INTEGER NOT NULL, `project` TEXT NOT NULL, `repeat` TEXT NOT NULL, `repeatDays` INTEGER NOT NULL, `reminderBefore` INTEGER, `done` INTEGER NOT NULL, `doneAt` INTEGER, `spawnedId` INTEGER, `createdAt` INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `subtasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `taskId` INTEGER NOT NULL, `title` TEXT NOT NULL, `done` INTEGER NOT NULL, `position` INTEGER NOT NULL, FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_subtasks_taskId` ON `subtasks` (`taskId`)",
        "CREATE TABLE IF NOT EXISTS `habits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `emoji` TEXT NOT NULL, `behavior` TEXT NOT NULL, `cue` TEXT NOT NULL, `timeMinutes` INTEGER, `location` TEXT NOT NULL, `anchor` TEXT NOT NULL, `tinyVersion` TEXT NOT NULL, `days` INTEGER NOT NULL, `target` INTEGER NOT NULL, `unit` TEXT NOT NULL, `reminderMinutes` INTEGER, `position` INTEGER NOT NULL, `archived` INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `habit_checks` (`habitId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `count` INTEGER NOT NULL, PRIMARY KEY(`habitId`, `date`), FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE TABLE IF NOT EXISTS `routine_steps` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `position` INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `routine_checks` (`stepId` INTEGER NOT NULL, `date` INTEGER NOT NULL, PRIMARY KEY(`stepId`, `date`), FOREIGN KEY(`stepId`) REFERENCES `routine_steps`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `body` TEXT NOT NULL, `takeaway` TEXT NOT NULL, `category` TEXT NOT NULL, `pinned` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
        "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'fff0d87cacf0d3b4349dd6a703bd044e')"
    )

    @Test
    fun `updating from version 1 keeps habits, checkmarks, tasks and notes`() = runBlocking {
        val name = "migration-test.db"
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            v1Schema.forEach { db.execSQL(it) }
            db.execSQL(
                "INSERT INTO habits (id, name, emoji, behavior, cue, timeMinutes, location, anchor, tinyVersion, days, target, unit, reminderMinutes, position, archived) " +
                    "VALUES (1, 'Reading', '📖', 'read 10 pages', 'TIME', 1260, 'my bedroom', '', 'one page', 127, 1, '', NULL, 0, 0)"
            )
            db.execSQL(
                "INSERT INTO habits (id, name, emoji, behavior, cue, timeMinutes, location, anchor, tinyVersion, days, target, unit, reminderMinutes, position, archived) " +
                    "VALUES (4, 'Water', '💧', '', 'TIME', NULL, '', '', '', 127, 8, 'glasses', 600, 3, 0)"
            )
            db.execSQL("INSERT INTO habit_checks (habitId, date, count) VALUES (1, 20773, 1), (4, 20773, 5)")
            db.execSQL(
                "INSERT INTO tasks (id, title, notes, dueDate, dueMinutes, location, firstStep, priority, project, repeat, repeatDays, reminderBefore, done, doneAt, spawnedId, createdAt) " +
                    "VALUES (7, 'Call the bank', '', 20773, 900, 'office', '', 3, 'Finances', 'NONE', 0, 30, 0, NULL, NULL, 1)"
            )
            db.execSQL("INSERT INTO notes (id, title, body, takeaway, category, pinned, createdAt, updatedAt) VALUES (9, 'Idea', 'Body', '', 'INBOX', 0, 1, 2)")
            db.version = 1
        }

        val db = AppDatabase.create(context, name)
        try {
            val habits = db.habitDao().all()
            assertEquals(listOf("Reading", "Water"), habits.map { it.name })
            assertTrue("old habits keep check-ins switched on", habits.all { it.askInCheckIns })
            assertEquals(1260, habits[0].timeMinutes)
            assertEquals(600, habits[1].reminderMinutes)
            assertEquals(setOf(1 to 1L, 5 to 4L), db.habitDao().allChecks().map { it.count to it.habitId }.toSet())
            assertEquals("Call the bank", db.taskDao().byId(7)?.title)
            assertEquals("Idea", db.noteDao().byId(9)?.title)
            // Y se puede seguir escribiendo con la columna nueva.
            db.habitDao().update(habits[0].copy(askInCheckIns = false))
            assertEquals(false, db.habitDao().byId(1)?.askInCheckIns)
        } finally {
            db.close()
        }
    }
}
