package com.jarvis.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.app.alarm.AlarmScheduler
import com.jarvis.app.alarm.BriefingPlayer
import com.jarvis.app.data.AppData
import com.jarvis.app.data.AppGraph
import com.jarvis.app.data.Settings
import com.jarvis.app.logic.BriefingBuilder
import com.jarvis.app.logic.CheckIndex
import com.jarvis.app.logic.DayProgress
import com.jarvis.app.logic.Formats
import com.jarvis.app.logic.HabitStats
import com.jarvis.app.logic.TaskLists
import com.jarvis.app.logic.Weather
import com.jarvis.app.logic.WeatherApi
import com.jarvis.app.ui.components.CheckCircle
import com.jarvis.app.ui.components.JarvisCard
import com.jarvis.app.ui.components.Meter
import com.jarvis.app.ui.components.SectionLabel
import com.jarvis.app.ui.components.is24Hour
import com.jarvis.app.ui.components.launchAction
import com.jarvis.app.ui.theme.Cyan
import com.jarvis.app.ui.theme.Hairline
import com.jarvis.app.ui.theme.OnCyan
import com.jarvis.app.ui.theme.TextMuted
import com.jarvis.app.ui.theme.TextPrimary
import com.jarvis.app.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

/**
 * Pantalla de inicio: el dia de un vistazo. Saludo, avance del dia, clima,
 * resumen hablado, rutina de la manana, tareas de hoy y habitos.
 */
@Composable
fun TodayScreen(
    data: AppData,
    settings: Settings,
    today: LocalDate,
    onOpenTask: (Long) -> Unit,
    onNewTask: () -> Unit,
    onOpenHabit: (Long) -> Unit,
    onEditRoutine: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val day = today.toEpochDay()
    val is24 = is24Hour()

    // --- Datos de hoy, calculados con la logica pura de logic/ ---
    val index = remember(data.habitChecks) { CheckIndex(data.habitChecks) }
    val pending = remember(data.tasks, today) { TaskLists.pendingToday(data.tasks, today) }
    val doneToday = remember(data.tasks, today) {
        TaskLists.doneToday(data.tasks, today, ZoneId.systemDefault())
    }
    val habitsToday = remember(data.habits, data.habitChecks, today) {
        data.habits.filter {
            !it.archived && (HabitStats.isScheduled(it.days, today) || index.count(it.id, day) > 0)
        }
    }
    val routineDone = remember(data.routineChecks, data.routineSteps, today) {
        val ids = data.routineSteps.map { it.id }.toSet()
        data.routineChecks.filter { it.date == day && it.stepId in ids }.map { it.stepId }.toSet()
    }
    val progress = DayProgress(
        routineDone = routineDone.size,
        routineTotal = data.routineSteps.size,
        tasksDone = doneToday.size,
        tasksTotal = pending.size + doneToday.size,
        habitsDone = habitsToday.count { index.isDone(it, day) },
        habitsTotal = habitsToday.size
    )

    // --- Clima: se refresca al abrir la pantalla y al cambiar de ciudad ---
    val weather by AppGraph.weather.flow.collectAsStateWithLifecycle()
    var weatherLoading by remember { mutableStateOf(false) }
    var weatherRefresh by remember { mutableStateOf(0) }
    LaunchedEffect(settings.cityLat, settings.cityLon, weatherRefresh) {
        if (settings.city != null) {
            weatherLoading = true
            AppGraph.weather.get(maxAgeMinutes = if (weatherRefresh > 0) 0 else 30)
            weatherLoading = false
        }
    }

    val speaking by BriefingPlayer.speaking.collectAsStateWithLifecycle()
    val lastBriefing by BriefingPlayer.last.collectAsStateWithLifecycle()
    var showCapture by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                    Text(
                        text = BriefingBuilder.greetingWord(LocalTime.now().hour).uppercase(Locale.US),
                        style = MaterialTheme.typography.labelMedium,
                        color = Cyan
                    )
                    Text(
                        text = settings.userName.ifBlank { "Welcome" },
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = Formats.fullDate(today),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            item(key = "progress") { ProgressCard(progress) }

            item(key = "weather") {
                WeatherCard(
                    settings = settings,
                    weather = weather,
                    loading = weatherLoading,
                    onClick = { if (settings.city == null) onOpenSettings() else weatherRefresh++ }
                )
            }

            item(key = "briefing") {
                JarvisCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = if (speaking) Cyan else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Briefing", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                if (speaking) "JARVIS is speaking…" else "Hear your day out loud",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Button(onClick = { BriefingPlayer.toggle() }) {
                            Icon(
                                if (speaking) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (speaking) "Stop" else "Play")
                        }
                    }
                    val shown = lastBriefing
                    if (shown != null) {
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Hairline)
                        Text(
                            shown.spoken,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            item(key = "alarm") {
                val next = AlarmScheduler.nextAlarm(settings, LocalDateTime.now())
                JarvisCard(onClick = onOpenSettings, padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Alarm,
                            contentDescription = null,
                            tint = if (next != null) Cyan else TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (next != null) {
                                "Next alarm · ${Formats.relativeDate(next.toLocalDate(), today)}, " +
                                    Formats.time(next.hour * 60 + next.minute, is24)
                            } else {
                                "Alarm is off"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (next != null) TextPrimary else TextSecondary
                        )
                    }
                }
            }

            // ---------------------------------------------- Rutina de la manana
            item(key = "routine-label") {
                SectionLabel("Morning routine") {
                    Text(
                        "${progress.routineDone}/${progress.routineTotal}",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    IconButton(onClick = onEditRoutine) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit morning routine", tint = TextSecondary)
                    }
                }
            }
            item(key = "routine") {
                if (data.routineSteps.isEmpty()) {
                    HintCard("No steps yet. Tap the pencil to build your morning routine.", onEditRoutine)
                } else {
                    JarvisCard(padding = 6.dp) {
                        for (step in data.routineSteps) {
                            val done = step.id in routineDone
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { launchAction { AppGraph.repo.setRoutineDone(step.id, day, !done) } }
                                    .padding(end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CheckCircle(
                                    checked = done,
                                    onCheckedChange = { launchAction { AppGraph.repo.setRoutineDone(step.id, day, it) } },
                                    label = step.title
                                )
                                Text(
                                    step.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (done) TextMuted else TextPrimary,
                                    textDecoration = if (done) TextDecoration.LineThrough else null
                                )
                            }
                        }
                    }
                }
            }

            // ------------------------------------------------------ Tareas de hoy
            item(key = "tasks-label") {
                SectionLabel("Today's tasks") {
                    TextButton(onClick = onNewTask) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }
            if (pending.isEmpty() && doneToday.isEmpty()) {
                item(key = "tasks-empty") {
                    HintCard("Nothing planned for today. Add a task and say when and where you'll do it.", onNewTask)
                }
            }
            items(pending, key = { "task-${it.id}" }) { task ->
                TaskRow(
                    task = task,
                    subtasks = data.subtasks.filter { it.taskId == task.id },
                    today = today,
                    onToggle = { launchAction { AppGraph.repo.setTaskDone(task.id, it) } },
                    onClick = { onOpenTask(task.id) }
                )
            }
            items(doneToday, key = { "task-${it.id}" }) { task ->
                TaskRow(
                    task = task,
                    subtasks = emptyList(),
                    today = today,
                    onToggle = { launchAction { AppGraph.repo.setTaskDone(task.id, it) } },
                    onClick = { onOpenTask(task.id) }
                )
            }

            // ------------------------------------------------------------ Habitos
            item(key = "habits-label") {
                SectionLabel("Habits") {
                    Text(
                        "${progress.habitsDone}/${progress.habitsTotal}",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )
                }
            }
            if (habitsToday.isEmpty()) {
                item(key = "habits-empty") {
                    HintCard("No habits scheduled for today.", null)
                }
            }
            items(habitsToday, key = { "habit-${it.id}" }) { habit ->
                val doneDays = index.doneDays(habit)
                HabitRow(
                    habit = habit,
                    count = index.count(habit.id, day),
                    streak = HabitStats.currentStreak(habit.days, doneDays, today),
                    missedLastTime = HabitStats.missedLastTime(habit.days, doneDays, today),
                    onToggle = { launchAction { AppGraph.repo.toggleHabit(habit, day) } },
                    onIncrement = { launchAction { AppGraph.repo.incrementHabit(habit, day) } },
                    onClick = { onOpenHabit(habit.id) }
                )
            }
        }

        FloatingActionButton(
            onClick = { showCapture = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Cyan,
            contentColor = OnCyan
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Quick capture")
        }
    }

    if (showCapture) {
        QuickCaptureSheet(onDismiss = { showCapture = false })
    }
}

/** Avance del dia: una cifra grande, una barra y el desglose por tipo. */
@Composable
private fun ProgressCard(progress: DayProgress) {
    JarvisCard {
        Text("DAY PROGRESS", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${progress.percent}%",
                style = MaterialTheme.typography.displayMedium,
                color = TextPrimary
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "${progress.done} of ${progress.total} done",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Meter(
            fraction = if (progress.total == 0) 0f else progress.done.toFloat() / progress.total,
            height = 10.dp,
            description = "Day progress ${progress.percent} percent"
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniMeter("Routine", progress.routineDone, progress.routineTotal, Modifier.weight(1f))
            MiniMeter("Tasks", progress.tasksDone, progress.tasksTotal, Modifier.weight(1f))
            MiniMeter("Habits", progress.habitsDone, progress.habitsTotal, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniMeter(label: String, done: Int, total: Int, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row {
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f))
            Text("$done/$total", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        }
        Spacer(Modifier.height(4.dp))
        Meter(
            fraction = if (total == 0) 0f else done.toFloat() / total,
            height = 4.dp,
            description = "$label $done of $total"
        )
    }
}

@Composable
private fun WeatherCard(settings: Settings, weather: Weather?, loading: Boolean, onClick: () -> Unit) {
    JarvisCard(onClick = onClick) {
        val city = settings.city
        when {
            city == null -> {
                Text("Weather", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    "Choose your city in Settings to see today's weather.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            weather == null -> {
                Text(city.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    if (loading) "Checking the weather…" else "Weather unavailable. Check your connection and tap to retry.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            else -> {
                val unit = if (settings.fahrenheit) "°F" else "°C"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(WeatherApi.emoji(weather.code), style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "${Formats.degrees(weather.tempC, settings.fahrenheit)}$unit",
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            WeatherApi.describe(weather.code).replaceFirstChar { it.uppercase(Locale.US) },
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(city.name, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
                Spacer(Modifier.height(8.dp))
                val parts = mutableListOf(
                    "High ${Formats.degrees(weather.maxC, settings.fahrenheit)}°",
                    "Low ${Formats.degrees(weather.minC, settings.fahrenheit)}°"
                )
                weather.rainProbability?.let { parts += "Rain $it%" }
                Text(parts.joinToString("  ·  "), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                WeatherApi.advice(weather)?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }
    }
}

/** Tarjeta con un texto de ayuda para secciones vacias. */
@Composable
internal fun HintCard(text: String, onClick: (() -> Unit)?) {
    JarvisCard(onClick = onClick) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

/**
 * Captura rapida (el paso "Capture" del segundo cerebro): escribes una idea y
 * decides en un toque si es una tarea para hoy o una nota para la bandeja.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickCaptureSheet(onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text("Quick capture", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Text(
                "Get it out of your head. Organize it later.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
                placeholder = { Text("What's on your mind?") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                minLines = 2,
                maxLines = 6
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        val captured = text
                        launchAction { AppGraph.repo.quickNote(captured) }
                        onDismiss()
                    },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) { Text("Save as note") }
                Button(
                    onClick = {
                        val captured = text
                        launchAction { AppGraph.repo.quickTask(captured, forToday = true) }
                        onDismiss()
                    },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) { Text("Task for today", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
    LaunchedEffect(Unit) {
        try {
            focus.requestFocus()
        } catch (e: IllegalStateException) {
            // El campo aun no estaba en pantalla: el usuario puede tocarlo.
        }
    }
}
