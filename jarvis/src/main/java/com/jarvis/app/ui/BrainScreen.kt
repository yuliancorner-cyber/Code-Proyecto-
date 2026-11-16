package com.jarvis.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jarvis.app.data.AppData
import com.jarvis.app.data.AppGraph
import com.jarvis.app.data.Note
import com.jarvis.app.data.Task
import com.jarvis.app.logic.NoteCategory
import com.jarvis.app.logic.NoteLists
import com.jarvis.app.ui.components.ConfirmDialog
import com.jarvis.app.ui.components.EditorScaffold
import com.jarvis.app.ui.components.EmptyState
import com.jarvis.app.ui.components.FieldLabel
import com.jarvis.app.ui.components.JarvisCard
import com.jarvis.app.ui.components.Tag
import com.jarvis.app.ui.components.launchAction
import com.jarvis.app.ui.theme.Cyan
import com.jarvis.app.ui.theme.Danger
import com.jarvis.app.ui.theme.OnCyan
import com.jarvis.app.ui.theme.TextMuted
import com.jarvis.app.ui.theme.TextPrimary
import com.jarvis.app.ui.theme.TextSecondary

/**
 * El "segundo cerebro": notas organizadas con el metodo PARA (Projects, Areas,
 * Resources, Archive) mas una bandeja de entrada para lo recien capturado.
 */
@Composable
fun BrainScreen(
    data: AppData,
    onOpenNote: (Long) -> Unit,
    onNewNote: (NoteCategory) -> Unit
) {
    // null = todas las categorias.
    var categoryName by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val category = categoryName?.let { NoteCategory.of(it) }

    val notes = remember(data.notes, category, query) { NoteLists.filter(data.notes, category, query) }
    val counts = remember(data.notes) { data.notes.groupingBy { it.category }.eachCount() }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "header") {
                Column {
                    Text(
                        "Brain",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        "Capture now. Organize later.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search your notes") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = category == null,
                            onClick = { categoryName = null },
                            label = { Text("All") }
                        )
                        for (option in NoteCategory.entries) {
                            val count = counts[option.name] ?: 0
                            FilterChip(
                                selected = category == option,
                                onClick = { categoryName = option.name },
                                label = { Text(if (count > 0) "${option.label} $count" else option.label) }
                            )
                        }
                    }
                    if (category != null) {
                        Text(
                            category.hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (notes.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Rounded.Psychology,
                        title = if (query.isNotBlank()) "No notes match" else "Nothing here yet",
                        text = if (query.isNotBlank()) {
                            "Try a different search."
                        } else {
                            "Ideas, references, plans: write them down so your mind doesn't have to hold them."
                        }
                    )
                }
            }

            items(notes, key = { "note-${it.id}" }) { note ->
                JarvisCard(onClick = { onOpenNote(note.id) }, padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            NoteLists.displayTitle(note),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (note.pinned) {
                            Icon(
                                Icons.Rounded.PushPin,
                                contentDescription = "Pinned",
                                tint = Cyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    // La idea clave, si la tiene; si no, el comienzo del texto.
                    val preview = note.takeaway.ifBlank {
                        if (note.title.isBlank()) "" else note.body
                    }.trim()
                    if (preview.isNotEmpty()) {
                        Text(
                            preview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (category == null) {
                        Tag(NoteCategory.of(note.category).label, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { onNewNote(category ?: NoteCategory.INBOX) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Cyan,
            contentColor = OnCyan
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "New note")
        }
    }
}

/**
 * Editor de una nota. Se guarda sola al salir (como Google Keep): no hay
 * boton "Save". Una nota nueva que se deja vacia no se guarda.
 */
@Composable
fun NoteEditorScreen(noteId: Long?, initialCategory: NoteCategory, onClose: () -> Unit) {
    var loaded by remember { mutableStateOf(noteId == null) }
    var note by remember { mutableStateOf(Note(category = initialCategory.name)) }
    var original by remember { mutableStateOf(note) }

    LaunchedEffect(noteId) {
        if (noteId != null) {
            val stored = AppGraph.repo.notes.byId(noteId)
            if (stored == null) onClose() else {
                note = stored
                original = stored
                loaded = true
            }
        }
    }
    if (!loaded) return

    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    fun saveAndClose() {
        val current = note.copy(title = note.title.trim(), takeaway = note.takeaway.trim())
        val empty = current.title.isBlank() && current.body.isBlank() && current.takeaway.isBlank()
        if (current != original && !(noteId == null && empty)) {
            launchAction { AppGraph.repo.saveNote(current) }
        }
        onClose()
    }

    EditorScaffold(
        title = if (noteId == null) "New note" else "Note",
        onBack = { saveAndClose() },
        actions = {
            IconButton(onClick = { note = note.copy(pinned = !note.pinned) }) {
                Icon(
                    Icons.Rounded.PushPin,
                    contentDescription = if (note.pinned) "Unpin note" else "Pin note",
                    tint = if (note.pinned) Cyan else TextMuted
                )
            }
            // "Express": convertir la idea en una accion.
            IconButton(
                onClick = {
                    val title = NoteLists.displayTitle(note)
                    val body = note.body
                    launchAction { AppGraph.repo.saveTask(Task(title = title, notes = body), emptyList()) }
                    android.widget.Toast.makeText(context, "Task created", android.widget.Toast.LENGTH_SHORT).show()
                },
                enabled = note.title.isNotBlank() || note.body.isNotBlank()
            ) {
                Icon(Icons.Rounded.TaskAlt, contentDescription = "Turn into a task")
            }
            if (noteId != null) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete note", tint = Danger)
                }
            }
        }
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (option in NoteCategory.entries) {
                FilterChip(
                    selected = note.category == option.name,
                    onClick = { note = note.copy(category = option.name) },
                    label = { Text(option.label) }
                )
            }
        }
        Text(
            NoteCategory.of(note.category).hint,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        OutlinedTextField(
            value = note.title,
            onValueChange = { note = note.copy(title = it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Title") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            maxLines = 2
        )
        OutlinedTextField(
            value = note.body,
            onValueChange = { note = note.copy(body = it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Note") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            minLines = 8
        )

        FieldLabel("Key takeaway")
        OutlinedTextField(
            value = note.takeaway,
            onValueChange = { note = note.copy(takeaway = it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            placeholder = { Text("The one line future you needs to read") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            maxLines = 3
        )
    }

    if (confirmDelete && noteId != null) {
        ConfirmDialog(
            title = "Delete this note?",
            text = "It will be removed for good.",
            confirmLabel = "Delete",
            onConfirm = {
                launchAction { AppGraph.repo.deleteNote(noteId) }
                onClose()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}
