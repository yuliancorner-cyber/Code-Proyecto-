package com.jarvis.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.jarvis.app.alarm.AlarmScheduler
import com.jarvis.app.data.AppGraph
import com.jarvis.app.data.Settings
import com.jarvis.app.logic.Formats
import com.jarvis.app.ui.components.JarvisCard
import com.jarvis.app.ui.components.TimeDialog
import com.jarvis.app.ui.components.ValueRow
import com.jarvis.app.ui.components.is24Hour
import com.jarvis.app.ui.components.launchAction
import com.jarvis.app.reminders.ReminderScheduler
import com.jarvis.app.ui.theme.Cyan
import com.jarvis.app.ui.theme.Hairline
import com.jarvis.app.ui.theme.TextPrimary
import com.jarvis.app.ui.theme.TextSecondary

/**
 * Bienvenida: se muestra solo la primera vez. Pide lo minimo para que JARVIS
 * pueda saludarte por la manana: tu nombre, tu ciudad y la hora de la alarma.
 */
@Composable
fun SetupScreen(settings: Settings) {
    val context = LocalContext.current
    val is24 = is24Hour()
    var name by rememberSaveable { mutableStateOf(settings.userName) }
    var showCity by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    fun finish() {
        AppGraph.settings.update { it.copy(userName = name.trim(), setupDone = true) }
        AlarmScheduler.schedule(context)
        launchAction { ReminderScheduler.sync(context) }
    }

    // En Android 13+ las notificaciones se piden con un dialogo del sistema.
    // Se termine como se termine (permitir o no), la bienvenida acaba.
    val askNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { finish() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text("JARVIS", style = MaterialTheme.typography.labelMedium, color = Cyan)
        Text(
            "Good to meet you.",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary
        )
        Text(
            "I'll wake you up, tell you the weather and your plan for the day, " +
                "and keep track of your tasks, habits and ideas.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("What should I call you?") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            singleLine = true
        )

        JarvisCard(padding = 12.dp) {
            ValueRow(
                label = "City",
                value = settings.city?.label ?: "Choose",
                icon = Icons.Rounded.LocationCity,
                onClick = { showCity = true }
            )
            HorizontalDivider(color = Hairline)
            ValueRow(
                label = "Wake-up alarm",
                value = Formats.time(settings.alarmMinutes, is24),
                icon = Icons.Rounded.Alarm,
                onClick = { showTime = true }
            )
        }
        Text(
            "Your city is only used to look up the weather online. No GPS. " +
                "You can change all of this later in Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    finish()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get started", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 6.dp))
        }
    }

    if (showCity) {
        CityDialog(
            onPick = { city ->
                setCity(city)
                showCity = false
            },
            onDismiss = { showCity = false }
        )
    }
    if (showTime) {
        TimeDialog(
            initialMinutes = settings.alarmMinutes,
            title = "Wake-up time",
            onConfirm = { minutes ->
                AppGraph.settings.update { it.copy(alarmHour = minutes / 60, alarmMinute = minutes % 60) }
                showTime = false
            },
            onDismiss = { showTime = false }
        )
    }
}
