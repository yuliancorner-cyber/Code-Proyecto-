package com.jarvis.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.jarvis.app.data.AppGraph
import com.jarvis.app.data.RoutineStep
import com.jarvis.app.logic.BriefingBuilder
import com.jarvis.app.logic.Formats
import com.jarvis.app.ui.components.EditorScaffold
import com.jarvis.app.ui.components.FieldLabel
import com.jarvis.app.ui.components.JarvisCard
import com.jarvis.app.ui.components.launchAction
import com.jarvis.app.ui.theme.Danger
import com.jarvis.app.ui.theme.TextPrimary
import com.jarvis.app.ui.theme.TextSecondary

/**
 * Editar la rutina de la manana: los pasos que JARVIS te recuerda al
 * despertar. Cada cambio se guarda al momento.
 */
@Composable
fun RoutineEditorScreen(steps: List<RoutineStep>, onClose: () -> Unit) {
    var newStep by remember { mutableStateOf("") }

    fun add() {
        val title = newStep
        if (title.isNotBlank()) {
            launchAction { AppGraph.repo.addRoutineStep(title) }
            newStep = ""
        }
    }

    EditorScaffold(title = "Morning routine", onBack = onClose) {
        Text(
            "JARVIS reads these out loud right after your alarm, in this order.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        steps.forEachIndexed { i, step ->
            // key: si cambia el orden, cada campo conserva su propio texto.
            androidx.compose.runtime.key(step.id) {
                var text by remember { mutableStateOf(step.title) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it
                            launchAction { AppGraph.repo.renameRoutineStep(step, it) }
                        },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        singleLine = true
                    )
                    IconButton(
                        onClick = { launchAction { AppGraph.repo.moveRoutineStep(step.id, -1) } },
                        enabled = i > 0
                    ) { Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up") }
                    IconButton(
                        onClick = { launchAction { AppGraph.repo.moveRoutineStep(step.id, 1) } },
                        enabled = i < steps.lastIndex
                    ) { Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down") }
                    IconButton(onClick = { launchAction { AppGraph.repo.deleteRoutineStep(step.id) } }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete step", tint = Danger)
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newStep,
                onValueChange = { newStep = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add a step, e.g. Stretch for 5 minutes") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { add() }),
                singleLine = true
            )
            IconButton(onClick = { add() }, enabled = newStep.isNotBlank()) {
                Icon(Icons.Rounded.Add, contentDescription = "Add step")
            }
        }

        if (steps.isNotEmpty()) {
            FieldLabel("JARVIS will say")
            JarvisCard(modifier = Modifier.padding(bottom = 24.dp)) {
                val spoken = steps.map { Formats.lowerFirst(Formats.stripEndPunctuation(it.title)) }
                    .filter { it.isNotEmpty() }
                Text(
                    "First things first: ${BriefingBuilder.joinList(spoken)}.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
            }
        }
    }
}
