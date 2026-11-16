package com.jarvis.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.jarvis.app.data.AppGraph
import com.jarvis.app.data.Habit
import com.jarvis.app.logic.Days
import com.jarvis.app.logic.Formats
import com.jarvis.app.logic.Intention
import com.jarvis.app.ui.components.ConfirmDialog
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

/**
 * Crear o editar un habito, con su intencion de implementacion:
 *  - "Time & place":         I will [behavior] at [time] in [location].
 *  - "After another habit":  After [anchor], I will [behavior].  (apilar)
 * y la version de dos minutos para los dias dificiles.
 */
@Composable
fun HabitEditorScreen(habitId: Long?, onClose: () -> Unit) {
    var loaded by remember { mutableStateOf(habitId == null) }
    var habit by remember { mutableStateOf(Habit(name = "")) }

    LaunchedEffect(habitId) {
        if (habitId != null) {
            val stored = AppGraph.repo.habits.byId(habitId)
            if (stored == null) onClose() else {
                habit = stored
                loaded = true
            }
        }
    }
    if (!loaded) return

    val is24 = is24Hour()
    var showTime by remember { mutableStateOf(false) }
    var showReminderTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun save() {
        val toSave = habit.copy(
            name = habit.name.trim(),
            emoji = habit.emoji.trim(),
            // Sin ningun dia marcado no tocaria nunca: se deja diario.
            days = if (habit.days and Days.ALL == 0) Days.ALL else habit.days,
            target = habit.target.coerceIn(1, 99)
        )
        launchAction { AppGraph.repo.saveHabit(toSave) }
        onClose()
    }

    EditorScaffold(
        title = if (habitId == null) "New habit" else "Edit habit",
        onBack = onClose,
        actions = {
            if (habitId != null) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete habit", tint = Danger)
                }
            }
            TextButton(onClick = { save() }, enabled = habit.name.isNotBlank()) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
        }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = habit.emoji,
                // Un emoji puede ocupar varios caracteres internos: se limita con margen.
                onValueChange = { if (it.length <= 8) habit = habit.copy(emoji = it) },
                modifier = Modifier.width(84.dp),
                label = { Text("Icon") },
                singleLine = true
            )
            OutlinedTextField(
                value = habit.name,
                onValueChange = { habit = habit.copy(name = it) },
                modifier = Modifier.weight(1f),
                label = { Text("Habit name") },
                placeholder = { Text("Reading") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                singleLine = true
            )
        }

        FieldLabel("Your plan")
        OutlinedTextField(
            value = habit.behavior,
            onValueChange = { habit = habit.copy(behavior = it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("I will…") },
            placeholder = { Text("read 10 pages") },
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = habit.cue == Habit.CUE_TIME,
                onClick = { habit = habit.copy(cue = Habit.CUE_TIME) },
                label = { Text("Time & place") }
            )
            FilterChip(
                selected = habit.cue == Habit.CUE_STACK,
                onClick = { habit = habit.copy(cue = Habit.CUE_STACK) },
                label = { Text("After another habit") }
            )
        }
        if (habit.cue == Habit.CUE_STACK) {
            OutlinedTextField(
                value = habit.anchor,
                onValueChange = { habit = habit.copy(anchor = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("After…") },
                placeholder = { Text("I brush my teeth") },
                singleLine = true
            )
        } else {
            ValueRow(
                label = "Time",
                value = habit.timeMinutes?.let { Formats.time(it, is24) } ?: "None",
                icon = Icons.Rounded.Schedule,
                onClick = { showTime = true }
            )
            OutlinedTextField(
                value = habit.location,
                onValueChange = { habit = habit.copy(location = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Where") },
                placeholder = { Text("My bedroom") },
                singleLine = true
            )
        }

        JarvisCard {
            Text("YOUR PLAN", style = MaterialTheme.typography.labelMedium, color = Cyan)
            val sentence = Intention.forHabit(habit, is24)
            Text(
                text = sentence.ifEmpty {
                    if (habit.cue == Habit.CUE_STACK) "After [current habit], I will [new habit]."
                    else "I will [behavior] at [time] in [location]."
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (sentence.isEmpty()) TextSecondary else TextPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        FieldLabel("Two-minute version")
        OutlinedTextField(
            value = habit.tinyVersion,
            onValueChange = { habit = habit.copy(tinyVersion = it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("The smallest version, e.g. read one page") },
            singleLine = true
        )

        FieldLabel("Days")
        DayPicker(mask = habit.days, onChange = { habit = habit.copy(days = it) })

        FieldLabel("Daily target")
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { habit = habit.copy(target = habit.target - 1) },
                enabled = habit.target > 1
            ) { Icon(Icons.Rounded.Remove, contentDescription = "Lower target") }
            Text(
                habit.target.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(
                onClick = { habit = habit.copy(target = habit.target + 1) },
                enabled = habit.target < 99
            ) { Icon(Icons.Rounded.Add, contentDescription = "Raise target") }
            Spacer(Modifier.width(8.dp))
            if (habit.target > 1) {
                OutlinedTextField(
                    value = habit.unit,
                    onValueChange = { habit = habit.copy(unit = it) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Unit") },
                    placeholder = { Text("glasses") },
                    singleLine = true
                )
            } else {
                Text(
                    "Once a day (a simple checkbox)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        FieldLabel("Reminder")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Remind me every scheduled day",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = habit.reminderMinutes != null,
                onCheckedChange = { on ->
                    habit = habit.copy(reminderMinutes = if (on) habit.timeMinutes ?: (20 * 60) else null)
                }
            )
        }
        if (habit.reminderMinutes != null) {
            ValueRow(
                label = "Reminder time",
                value = Formats.time(habit.reminderMinutes ?: 0, is24),
                icon = Icons.Rounded.Notifications,
                onClick = { showReminderTime = true }
            )
        }

        if (habitId != null) {
            FieldLabel("Order in the list")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 24.dp)) {
                OutlinedButton(onClick = { launchAction { AppGraph.repo.moveHabit(habitId, -1) } }) {
                    Icon(Icons.Rounded.ArrowUpward, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Move up")
                }
                OutlinedButton(onClick = { launchAction { AppGraph.repo.moveHabit(habitId, 1) } }) {
                    Icon(Icons.Rounded.ArrowDownward, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Move down")
                }
            }
        }
    }

    if (showTime) {
        TimeDialog(
            initialMinutes = habit.timeMinutes ?: (20 * 60),
            onConfirm = {
                habit = habit.copy(timeMinutes = it)
                showTime = false
            },
            onDismiss = { showTime = false },
            onClear = {
                habit = habit.copy(timeMinutes = null)
                showTime = false
            }
        )
    }
    if (showReminderTime) {
        TimeDialog(
            initialMinutes = habit.reminderMinutes ?: (20 * 60),
            onConfirm = {
                habit = habit.copy(reminderMinutes = it)
                showReminderTime = false
            },
            onDismiss = { showReminderTime = false }
        )
    }
    if (confirmDelete && habitId != null) {
        ConfirmDialog(
            title = "Delete this habit?",
            text = "Its whole history of checkmarks will be deleted too.",
            confirmLabel = "Delete",
            onConfirm = {
                launchAction { AppGraph.repo.deleteHabit(habitId) }
                onClose()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}
