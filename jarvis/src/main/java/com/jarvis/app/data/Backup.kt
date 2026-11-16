package com.jarvis.app.data

import org.json.JSONArray
import org.json.JSONObject

/** Todo lo que contiene una copia de seguridad. */
data class BackupData(
    val settings: Settings,
    val tasks: List<Task>,
    val subtasks: List<Subtask>,
    val habits: List<Habit>,
    val habitChecks: List<HabitCheck>,
    val routineSteps: List<RoutineStep>,
    val routineChecks: List<RoutineCheck>,
    val notes: List<Note>
)

/**
 * Convierte los datos de la app a un archivo de texto JSON y al reves.
 * Sirve para guardar una copia (en Drive, en el PC...) y para pasar los datos
 * de "JARVIS Dev" a "JARVIS" o a otro celular.
 *
 * No depende de Android: se prueba con tests en el PC (ida y vuelta).
 */
object BackupCodec {
    const val FORMAT = "jarvis-backup"
    const val VERSION = 1

    fun encode(data: BackupData): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", VERSION)
        root.put("settings", settings(data.settings))
        root.put("tasks", JSONArray(data.tasks.map(::task)))
        root.put("subtasks", JSONArray(data.subtasks.map(::subtask)))
        root.put("habits", JSONArray(data.habits.map(::habit)))
        root.put("habitChecks", JSONArray(data.habitChecks.map {
            JSONObject().put("habitId", it.habitId).put("date", it.date).put("count", it.count)
        }))
        root.put("routineSteps", JSONArray(data.routineSteps.map {
            JSONObject().put("id", it.id).put("title", it.title).put("position", it.position)
        }))
        root.put("routineChecks", JSONArray(data.routineChecks.map {
            JSONObject().put("stepId", it.stepId).put("date", it.date)
        }))
        root.put("notes", JSONArray(data.notes.map(::note)))
        return root.toString(2)
    }

    /** Lanza IllegalArgumentException si el archivo no es una copia de JARVIS. */
    fun decode(json: String): BackupData {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw IllegalArgumentException("This file is not a JARVIS backup.")
        }
        require(root.optString("format") == FORMAT) { "This file is not a JARVIS backup." }
        require(root.optInt("version", 0) in 1..VERSION) {
            "This backup was made with a newer version of JARVIS."
        }
        return BackupData(
            settings = settings(root.getJSONObject("settings")),
            tasks = root.getJSONArray("tasks").objects().map(::task),
            subtasks = root.getJSONArray("subtasks").objects().map(::subtask),
            habits = root.getJSONArray("habits").objects().map(::habit),
            habitChecks = root.getJSONArray("habitChecks").objects().map {
                HabitCheck(it.getLong("habitId"), it.getLong("date"), it.optInt("count", 1))
            },
            routineSteps = root.getJSONArray("routineSteps").objects().map {
                RoutineStep(it.getLong("id"), it.getString("title"), it.optInt("position", 0))
            },
            routineChecks = root.getJSONArray("routineChecks").objects().map {
                RoutineCheck(it.getLong("stepId"), it.getLong("date"))
            },
            notes = root.getJSONArray("notes").objects().map(::note)
        )
    }

    // ---- Ayudas: en JSON un valor ausente equivale a null.

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)
    private fun JSONObject.intOrNull(key: String): Int? = if (isNull(key)) null else getInt(key)
    private fun JSONObject.doubleOrNull(key: String): Double? = if (isNull(key)) null else getDouble(key)

    private fun settings(s: Settings): JSONObject = JSONObject()
        .put("userName", s.userName)
        .put("cityName", s.cityName)
        .put("cityRegion", s.cityRegion)
        .put("cityCountry", s.cityCountry)
        .putOpt("cityLat", s.cityLat)
        .putOpt("cityLon", s.cityLon)
        .put("fahrenheit", s.fahrenheit)
        .put("alarmEnabled", s.alarmEnabled)
        .put("alarmHour", s.alarmHour)
        .put("alarmMinute", s.alarmMinute)
        .put("alarmDays", s.alarmDays)
        .put("snoozeMinutes", s.snoozeMinutes)
        .put("toneSeconds", s.toneSeconds)
        .put("voiceEnabled", s.voiceEnabled)
        .put("speechRate", s.speechRate.toDouble())
        .put("voicePitch", s.voicePitch.toDouble())
        .put("voiceName", s.voiceName)

    private fun settings(o: JSONObject): Settings {
        val d = Settings()
        return Settings(
            // Quien importa una copia ya paso por la bienvenida.
            setupDone = true,
            userName = o.optString("userName", d.userName),
            cityName = o.optString("cityName", ""),
            cityRegion = o.optString("cityRegion", ""),
            cityCountry = o.optString("cityCountry", ""),
            cityLat = o.doubleOrNull("cityLat"),
            cityLon = o.doubleOrNull("cityLon"),
            fahrenheit = o.optBoolean("fahrenheit", d.fahrenheit),
            alarmEnabled = o.optBoolean("alarmEnabled", d.alarmEnabled),
            alarmHour = o.optInt("alarmHour", d.alarmHour).coerceIn(0, 23),
            alarmMinute = o.optInt("alarmMinute", d.alarmMinute).coerceIn(0, 59),
            alarmDays = o.optInt("alarmDays", d.alarmDays),
            snoozeMinutes = o.optInt("snoozeMinutes", d.snoozeMinutes),
            toneSeconds = o.optInt("toneSeconds", d.toneSeconds),
            voiceEnabled = o.optBoolean("voiceEnabled", d.voiceEnabled),
            speechRate = o.optDouble("speechRate", d.speechRate.toDouble()).toFloat(),
            voicePitch = o.optDouble("voicePitch", d.voicePitch.toDouble()).toFloat(),
            voiceName = o.optString("voiceName", "")
        )
    }

    private fun task(t: Task): JSONObject = JSONObject()
        .put("id", t.id)
        .put("title", t.title)
        .put("notes", t.notes)
        .putOpt("dueDate", t.dueDate)
        .putOpt("dueMinutes", t.dueMinutes)
        .put("location", t.location)
        .put("firstStep", t.firstStep)
        .put("priority", t.priority)
        .put("project", t.project)
        .put("repeat", t.repeat)
        .put("repeatDays", t.repeatDays)
        .putOpt("reminderBefore", t.reminderBefore)
        .put("done", t.done)
        .putOpt("doneAt", t.doneAt)
        .putOpt("spawnedId", t.spawnedId)
        .put("createdAt", t.createdAt)

    private fun task(o: JSONObject): Task = Task(
        id = o.getLong("id"),
        title = o.getString("title"),
        notes = o.optString("notes", ""),
        dueDate = o.longOrNull("dueDate"),
        dueMinutes = o.intOrNull("dueMinutes"),
        location = o.optString("location", ""),
        firstStep = o.optString("firstStep", ""),
        priority = o.optInt("priority", 0),
        project = o.optString("project", ""),
        repeat = o.optString("repeat", "NONE"),
        repeatDays = o.optInt("repeatDays", 0),
        reminderBefore = o.intOrNull("reminderBefore"),
        done = o.optBoolean("done", false),
        doneAt = o.longOrNull("doneAt"),
        spawnedId = o.longOrNull("spawnedId"),
        createdAt = o.optLong("createdAt", 0)
    )

    private fun subtask(s: Subtask): JSONObject = JSONObject()
        .put("id", s.id).put("taskId", s.taskId).put("title", s.title)
        .put("done", s.done).put("position", s.position)

    private fun subtask(o: JSONObject): Subtask = Subtask(
        id = o.getLong("id"),
        taskId = o.getLong("taskId"),
        title = o.getString("title"),
        done = o.optBoolean("done", false),
        position = o.optInt("position", 0)
    )

    private fun habit(h: Habit): JSONObject = JSONObject()
        .put("id", h.id)
        .put("name", h.name)
        .put("emoji", h.emoji)
        .put("behavior", h.behavior)
        .put("cue", h.cue)
        .putOpt("timeMinutes", h.timeMinutes)
        .put("location", h.location)
        .put("anchor", h.anchor)
        .put("tinyVersion", h.tinyVersion)
        .put("days", h.days)
        .put("target", h.target)
        .put("unit", h.unit)
        .putOpt("reminderMinutes", h.reminderMinutes)
        .put("position", h.position)
        .put("archived", h.archived)

    private fun habit(o: JSONObject): Habit = Habit(
        id = o.getLong("id"),
        name = o.getString("name"),
        emoji = o.optString("emoji", ""),
        behavior = o.optString("behavior", ""),
        cue = o.optString("cue", Habit.CUE_TIME),
        timeMinutes = o.intOrNull("timeMinutes"),
        location = o.optString("location", ""),
        anchor = o.optString("anchor", ""),
        tinyVersion = o.optString("tinyVersion", ""),
        days = o.optInt("days", 127),
        target = o.optInt("target", 1),
        unit = o.optString("unit", ""),
        reminderMinutes = o.intOrNull("reminderMinutes"),
        position = o.optInt("position", 0),
        archived = o.optBoolean("archived", false)
    )

    private fun note(n: Note): JSONObject = JSONObject()
        .put("id", n.id)
        .put("title", n.title)
        .put("body", n.body)
        .put("takeaway", n.takeaway)
        .put("category", n.category)
        .put("pinned", n.pinned)
        .put("createdAt", n.createdAt)
        .put("updatedAt", n.updatedAt)

    private fun note(o: JSONObject): Note = Note(
        id = o.getLong("id"),
        title = o.optString("title", ""),
        body = o.optString("body", ""),
        takeaway = o.optString("takeaway", ""),
        category = o.optString("category", "INBOX"),
        pinned = o.optBoolean("pinned", false),
        createdAt = o.optLong("createdAt", 0),
        updatedAt = o.optLong("updatedAt", 0)
    )
}
