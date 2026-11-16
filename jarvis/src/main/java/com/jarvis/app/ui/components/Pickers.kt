package com.jarvis.app.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

// Dialogos para elegir hora y fecha (los relojes y calendarios de Material 3).

/** ¿El celular usa formato de 24 horas? */
@Composable
fun is24Hour(): Boolean = DateFormat.is24HourFormat(LocalContext.current)

/**
 * Reloj para elegir una hora.
 * @param initialMinutes hora inicial en minutos desde medianoche.
 * @param onClear si no es null, aparece un boton para quitar la hora.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select time",
    onClear: (() -> Unit)? = null
) {
    val state = rememberTimePickerState(
        initialHour = (initialMinutes / 60) % 24,
        initialMinute = initialMinutes % 60,
        is24Hour = is24Hour()
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("OK") }
        },
        dismissButton = {
            if (onClear != null) {
                TextButton(onClick = onClear) { Text("Clear") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

/**
 * Calendario para elegir un dia.
 * @param initialEpochDay dia inicial (epochDay), o null para hoy.
 * @param onClear si no es null, aparece un boton para quitar la fecha.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDialog(
    initialEpochDay: Long?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
    onClear: (() -> Unit)? = null
) {
    // El calendario de Material trabaja en milisegundos UTC.
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initialEpochDay ?: java.time.LocalDate.now().toEpochDay()) * MILLIS_PER_DAY
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onConfirm(it / MILLIS_PER_DAY) } },
                enabled = state.selectedDateMillis != null
            ) { Text("OK") }
        },
        dismissButton = {
            if (onClear != null) {
                TextButton(onClick = onClear) { Text("Clear") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    ) {
        DatePicker(state = state)
    }
}

private const val MILLIS_PER_DAY = 86_400_000L
