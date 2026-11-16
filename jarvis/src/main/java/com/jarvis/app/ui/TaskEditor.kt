package com.jarvis.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.jarvis.app.data.AppGraph
import com.jarvis.app.data.Subtask
import com.jarvis.app.data.Task
import com.jarvis.app.logic.Formats
import com.jarvis.app.logic.Intention
import com.jarvis.app.logic.Priority
import com.jarvis.app.logic.ReminderOptions
import com.jarvis.app.logic.Repeat
import com.jarvis.app.ui.components.CheckCircle
import com.jarvis.app.ui.components.ChoiceRow
import com.jarvis.app.ui.components.ConfirmDialog
import com.jarvis.app.ui.components.DateDialog
import com.jarvis.app.ui.components.DayPicker
import com.jarvis.app.ui.components.EditorScaffold
import com.jarvis.app.ui.components.FieldLabel
import com.jarvis.app.ui.components.JarvisCard
import com.jarvis.app.ui.components.TimeDialog
import com.jarvis.app.ui.components.ValueRow
import com.jarvis.app.ui.components.is24Hour
import com.jarvis.app.ui.components.launchAction
import com.jarvis.app.ui.theme.Cyan
import com.jarvis.app.ui.theme.Danger
import com.jarvis.app.ui.theme.TextPrimary
import com.jarvis.app.ui.theme.TextSecondary
import java.time.LocalDate

/**
 * Crear o editar una tarea.
 *
 * El formulario sigue la intencion de implementacion de James Clear:
 * QUE haras, CUANDO y DONDE, mas el primer paso de dos minutos para arrancar.
 * La tarjeta de arriba muestra la frase completa mientras escribes.
 *
 * @param taskId id de la tarea a editar, o null para una nueva.
 * @param presetToday si es nueva, empieza con la fecha de hoy.
 */
@Composable
fun TaskEditorScreen(
    taskId: Long?,
    presetToday: Boolean,
    today: LocalDate,
    projects: List<String>,
    onClose: () -> Unit
) {
    var loaded by remember { mutableStateOf(taskId == null) }
    var task by remember {
        mutableStateOf(Task(title = "", dueDate = if (presetToday) today.toEpochDay() else null))
    }
    var subtasks by remember { mutableStateOf(listOf<Subtask>()) }

    // Al editar, se cargan los datos guardados una sola vez.
    LaunchedEffect(taskId) {
        if (taskId != null) {
            val stored = AppGraph.repo.tasks.byId(taskId)
            if (stored == null) {
                onClose()
            } else {
                task = stored
                subtasks = AppGraph.repo.tasks.subtasksOf(taskId)
                loaded = true
            }
        }
    }
    if (!loaded) return

    val is24 = is24Hour()
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var newStep by remember { mutableStateOf("") }

    fun addStep() {
        if (newStep.isNotBlank()) {
            subtasks = subtasks + Subtask(taskId = task.id, title = newStep.trim())
            newStep = ""
        }
    }

    fun save() {
        addStep()
        var toSave = task.copy(title = task.title.trim())
        // Repetir o recordar necesitan una fecha: si falta, se usa hoy.
        val needsDate = Repeat.of(toSave.repeat) != Repeat.NONE || toSave.reminderBefore != null
        if (needsDate && toSave.dueDate == null) toSave = toSave.copy(dueDate = today.toEpochDay())
        val steps = subtasks
        launchAction { AppGraph.repo.saveTask(toSave, steps) }
        onClose()
    }

    EditorScaffold(
        title = if (taskId == null) "New task" else "Edit task",
        onBack = onClose,
        actions = {
            if (taskId != null) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete task", tint = Danger)
                }
            }
            TextButton(onClick = { save() }, enabled = task.title.isNotBlank()) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
        }
    ) {
        OutlinedTextField(
            value = task.title,
            onValueChange = { task = task.copy(title = it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("What will you do?") },
            placeholder = { Text("Call the bank") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            maxLines = 3
        )

        // La frase de la intencion, en vivo.
        JarvisCard {
            Text("YOUR PLAN", style = MaterialTheme.typography.labelMedium, color = Cyan)
            val sentence = Intention.forTask(task, is24)
            Text(
                text = sentence.ifEmpty { "I will [what] at [time] in [place]." },
                style = MaterialTheme.typography.titleMedium,
                color = if (sentence.isEmpty()) TextSecondary else TextPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (task.dueMinutes == null || task.location.isBlank()) {
                Text(
                    "Plans with a time and a place are far more likely to happen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        FieldLabel("When")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val day = today.toEpochDay()
            FilterChip(
                selected = task.dueDate == day,
                onClick = { task = task.copy(dueDate = day) },
                label = { Text("Today") }
            )
            FilterChip(
                selected = task.dueDate == day + 1,
                onClick = { task = task.copy(dueDate = day + 1) },
                label = { Text("Tomorrow") }
            )
            FilterChip(
                selected = task.dueDate == null,
                onClick = { task = task.copy(dueDate = null) },
                label = { Text("No date") }
            )
        }
        ValueRow(
            label = "Date",
            value = task.dueDate?.let { Formats.relativeDate(LocalDate.ofEpochDay(it), today) } ?: "None",
            icon = Icons.Rounded.Event,
            onClick = { showDate = true }
        )
        ValueRow(
            label = "Time",
            value = task.dueMinutes?.let { Formats.time(it, is24) } ?: "None",
            icon = Icons.Rounded.Schedule,
            onClick = { showTime = true }
        )

        FieldLabel("Where")
        OutlinedTextField(
            value = task.location,
            onValueChange = { task = task.copy(location = it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("My desk, the office, the gym…") },
            singleLine = true
        )

        FieldLabel("Two-minute start")
        OutlinedTextField(
            value = task.firstStep,
            onValueChange = { task = task.copy(firstStep = it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("The tiny first step, e.g. open the document") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            singleLine = true
        )

        FieldLabel("Priority")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (priority in Priority.entries) {
                FilterChip(
                    selected = task.priority == priority.level,
                    onClick = { task = task.copy(priority = priority.level) },
                    label = { Text(priority.label) }
                )
            }
        }

        FieldLabel("Project or area")
        OutlinedTextField(
            value = task.project,
            onValueChange = { task = task.copy(project = it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("University, Health, Home…") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            singleLine = true
        )
        val suggestions = projects.filter { !it.equals(task.project.trim(), ignoreCase = true) }
        if (suggestions.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (name in suggestions) {
                    AssistChip(onClick = { task = task.copy(project = name) }, label = { Text(name) })
                }
            }
        }

        FieldLabel("Repeat and remind")
        ChoiceRow(
            label = "Repeat",
            value = repeatLabel(Repeat.of(task.repeat), task.repeatDays),
            options = Repeat.entries.map { it to it.label },
            onSelect = { task = task.copy(repeat = it.name) },
            icon = Icons.Rounded.Repeat
        )
        if (Repeat.of(task.repeat) == Repeat.WEEKLY) {
            DayPicker(mask = task.repeatDays, onChange = { task = task.copy(repeatDays = it) })
        }
        ChoiceRow(
            label = "Reminder",
            value = ReminderOptions.label(task.reminderBefore),
            options = ReminderOptions.ALL,
            onSelect = { task = task.copy(reminderBefore = it) },
            icon = Icons.Rounded.Notifications
        )
        if (task.reminderBefore != null && task.dueMinutes == null) {
            Text(
                "No time set, so the reminder counts from 9:00 AM.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        FieldLabel("Steps")
        subtasks.forEachIndexed { i, step ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                CheckCircle(
                    checked = step.done,
                    onCheckedChange = { done ->
                        subtasks = subtasks.toMutableList().also { it[i] = step.copy(done = done) }
                    },
                    label = step.title
                )
                OutlinedTextField(
                    value = step.title,
                    onValueChange = { text ->
                        subtasks = subtasks.toMutableList().also { it[i] = step.copy(title = text) }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = { subtasks = subtasks.toMutableList().also { it.removeAt(i) } }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Remove step", tint = TextSecondary)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newStep,
                onValueChange = { newStep = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add a step") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { addStep() }),
                singleLine = true
            )
            IconButton(onClick = { addStep() }, enabled = newStep.isNotBlank()) {
                Icon(Icons.Rounded.Add, contentDescription = "Add step")
            }
        }

        FieldLabel("Notes")
        OutlinedTextField(
            value = task.notes,
            onValueChange = { task = task.copy(notes = it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            placeholder = { Text("Anything else worth remembering") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            minLines = 3
        )
    }

    if (showDate) {
        DateDialog(
            initialEpochDay = task.dueDate,
            onConfirm = {
                task = task.copy(dueDate = it)
                showDate = false
            },
            onDismiss = { showDate = false },
            onClear = {
                task = task.copy(dueDate = null)
                showDate = false
            }
        )
    }
    if (showTime) {
        TimeDialog(
            initialMinutes = task.dueMinutes ?: 9 * 60,
            onConfirm = {
                // Una hora sin fecha no tiene sentido: se asume hoy.
                task = task.copy(dueMinutes = it, dueDate = task.dueDate ?: today.toEpochDay())
                showTime = false
            },
            onDismiss = { showTime = false },
            onClear = {
                task = task.copy(dueMinutes = null)
                showTime = false
            }
        )
    }
    if (confirmDelete && taskId != null) {
        ConfirmDialog(
            title = "Delete this task?",
            text = "It will be removed for good.",
            confirmLabel = "Delete",
            onConfirm = {
                launchAction { AppGraph.repo.deleteTask(taskId) }
                onClose()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}
