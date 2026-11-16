package com.jarvis.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jarvis.app.data.AppData
import com.jarvis.app.data.AppGraph
import com.jarvis.app.data.Subtask
import com.jarvis.app.data.Task
import com.jarvis.app.logic.Days
import com.jarvis.app.logic.Formats
import com.jarvis.app.logic.Priority
import com.jarvis.app.logic.Repeat
import com.jarvis.app.logic.TaskLists
import com.jarvis.app.logic.TaskSection
import com.jarvis.app.ui.components.CheckCircle
import com.jarvis.app.ui.components.EmptyState
import com.jarvis.app.ui.components.JarvisCard
import com.jarvis.app.ui.components.Tag
import com.jarvis.app.ui.components.is24Hour
import com.jarvis.app.ui.components.launchAction
import com.jarvis.app.ui.components.priorityColor
import com.jarvis.app.ui.theme.Cyan
import com.jarvis.app.ui.theme.Danger
import com.jarvis.app.ui.theme.OnCyan
import com.jarvis.app.ui.theme.TextMuted
import com.jarvis.app.ui.theme.TextPrimary
import com.jarvis.app.ui.theme.TextSecondary
import java.time.LocalDate

/** Lista completa de tareas, agrupada por fecha, con busqueda y filtro por proyecto. */
@Composable
fun TasksScreen(
    data: AppData,
    today: LocalDate,
    onOpenTask: (Long) -> Unit,
    onNewTask: () -> Unit
) {
    var showDone by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var project by rememberSaveable { mutableStateOf<String?>(null) }

    val projects = remember(data.tasks) {
        data.tasks.filter { !it.done && it.project.isNotBlank() }.map { it.project }.distinct().sorted()
    }
    // Si el proyecto elegido ya no tiene tareas, se quita el filtro.
    val activeProject = project?.takeIf { it in projects }

    val groups = remember(data.tasks, today, showDone, query, activeProject) {
        val visible = data.tasks.filter {
            it.done == showDone &&
                TaskLists.matches(it, query) &&
                (activeProject == null || it.project == activeProject)
        }
        TaskLists.grouped(visible, today, includeDone = showDone)
    }
    val subtasksByTask = remember(data.subtasks) { data.subtasks.groupBy { it.taskId } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "header") {
                Column {
                    Text(
                        "Tasks",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        "Say what, when and where.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search tasks") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !showDone && activeProject == null,
                            onClick = {
                                showDone = false
                                project = null
                            },
                            label = { Text("Active") }
                        )
                        FilterChip(
                            selected = showDone,
                            onClick = {
                                showDone = true
                                project = null
                            },
                            label = { Text("Completed") }
                        )
                        for (name in projects) {
                            FilterChip(
                                selected = activeProject == name,
                                onClick = {
                                    showDone = false
                                    project = if (activeProject == name) null else name
                                },
                                label = { Text(name) }
                            )
                        }
                    }
                }
            }

            if (groups.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Rounded.TaskAlt,
                        title = when {
                            query.isNotBlank() -> "No tasks match"
                            showDone -> "Nothing completed yet"
                            else -> "All clear"
                        },
                        text = when {
                            query.isNotBlank() -> "Try a different search."
                            showDone -> "Tasks you finish will show up here."
                            else -> "Add a task and give it a time and a place."
                        }
                    )
                }
            }

            for ((section, tasks) in groups) {
                item(key = "section-${section.name}") {
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            section.title.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (section == TaskSection.OVERDUE) Danger else TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            tasks.size.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                    }
                }
                items(tasks, key = { "task-${it.id}" }) { task ->
                    TaskRow(
                        task = task,
                        subtasks = subtasksByTask[task.id].orEmpty(),
                        today = today,
                        onToggle = { launchAction { AppGraph.repo.setTaskDone(task.id, it) } },
                        onClick = { onOpenTask(task.id) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onNewTask,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Cyan,
            contentColor = OnCyan
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "New task")
        }
    }
}

/**
 * Una tarea en una lista: casilla, titulo, "cuando · donde" y etiquetas
 * (prioridad, proyecto, repeticion, pasos).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TaskRow(
    task: Task,
    subtasks: List<Subtask>,
    today: LocalDate,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val is24 = is24Hour()
    val overdue = !task.done && task.dueDate != null && task.dueDate < today.toEpochDay()

    JarvisCard(onClick = onClick, padding = 6.dp) {
        Row(verticalAlignment = Alignment.Top) {
            CheckCircle(checked = task.done, onCheckedChange = onToggle, label = task.title)
            Column(
                Modifier
                    .weight(1f)
                    .padding(top = 10.dp, bottom = 10.dp, end = 10.dp)
            ) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (task.done) TextMuted else TextPrimary,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                // "Cuando · donde": el corazon de la intencion de implementacion.
                val whenParts = mutableListOf<String>()
                if (overdue) whenParts += "Overdue"
                task.dueDate?.let { whenParts += Formats.relativeDate(LocalDate.ofEpochDay(it), today) }
                task.dueMinutes?.let { whenParts += Formats.time(it, is24) }
                if (task.location.isNotBlank()) whenParts += task.location.trim()
                if (whenParts.isNotEmpty()) {
                    Text(
                        whenParts.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (overdue) Danger else TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val priority = Priority.of(task.priority)
                val repeat = Repeat.of(task.repeat)
                val hasTags = priority != Priority.NONE || task.project.isNotBlank() ||
                    repeat != Repeat.NONE || subtasks.isNotEmpty()
                if (hasTags && !task.done) {
                    FlowRow(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (priority != Priority.NONE) Tag("${priority.label} priority", dot = priorityColor(priority))
                        if (task.project.isNotBlank()) Tag(task.project.trim())
                        if (repeat != Repeat.NONE) Tag(repeatLabel(repeat, task.repeatDays))
                        if (subtasks.isNotEmpty()) Tag("${subtasks.count { it.done }}/${subtasks.size} steps")
                    }
                }
            }
        }
    }
}

/** "Repeats daily", "Repeats Mon, Wed"... */
internal fun repeatLabel(repeat: Repeat, days: Int): String = when (repeat) {
    Repeat.NONE -> "Does not repeat"
    Repeat.DAILY -> "Repeats daily"
    Repeat.WEEKDAYS -> "Repeats on weekdays"
    Repeat.WEEKLY -> if (days and Days.ALL == 0) "Repeats weekly" else "Repeats ${Days.label(days)}"
    Repeat.MONTHLY -> "Repeats monthly"
}
