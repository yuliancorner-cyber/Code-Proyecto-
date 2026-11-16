package com.jarvis.app.ui

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.jarvis.app.alarm.AlarmScheduler
import com.jarvis.app.alarm.AlarmService
import com.jarvis.app.alarm.Speaker
import com.jarvis.app.alarm.VoiceStyle
import com.jarvis.app.data.AppData
import com.jarvis.app.data.AppGraph
import com.jarvis.app.data.BackupCodec
import com.jarvis.app.data.Settings
import com.jarvis.app.logic.City
import com.jarvis.app.logic.Formats
import com.jarvis.app.logic.VoiceOption
import com.jarvis.app.logic.VoicePicker
import com.jarvis.app.reminders.Notifications
import com.jarvis.app.reminders.ReminderScheduler
import com.jarvis.app.ui.components.ChoiceRow
import com.jarvis.app.ui.components.ConfirmDialog
import com.jarvis.app.ui.components.DayPicker
import com.jarvis.app.ui.components.JarvisCard
import com.jarvis.app.ui.components.SectionLabel
import com.jarvis.app.ui.components.TimeDialog
import com.jarvis.app.ui.components.ValueRow
import com.jarvis.app.ui.components.is24Hour
import com.jarvis.app.ui.theme.Cyan
import com.jarvis.app.ui.theme.Hairline
import com.jarvis.app.ui.theme.TextPrimary
import com.jarvis.app.ui.theme.TextSecondary
import com.jarvis.app.ui.theme.Warning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Cambia los ajustes de la alarma y la vuelve a programar al momento. */
internal fun updateAlarm(context: Context, change: (Settings) -> Settings) {
    AppGraph.settings.update(change)
    AlarmScheduler.schedule(context)
}

/** Ajustes: perfil, clima, alarma, rutina, permisos del celular y copia de seguridad. */
@Composable
fun SettingsScreen(
    data: AppData,
    settings: Settings,
    onEditRoutine: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val is24 = is24Hour()

    var showName by remember { mutableStateOf(false) }
    var showCity by remember { mutableStateOf(false) }
    var showAlarmTime by remember { mutableStateOf(false) }
    var showVoices by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<String?>(null) }

    // Voz para el boton "Test voice"; se libera al salir de la pantalla.
    val speaker = remember { Speaker(context, asAlarm = false) }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { speaker.shutdown() } }

    // --- Copia de seguridad: Android abre su selector de archivos ---
    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = try {
                    val json = BackupCodec.encode(AppGraph.repo.exportData(AppGraph.settings.value))
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                            ?: throw java.io.IOException("Cannot open file")
                    }
                    true
                } catch (e: Exception) {
                    false
                }
                Toast.makeText(context, if (ok) "Backup saved" else "Could not save the backup", Toast.LENGTH_LONG).show()
            }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val text = try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                    }
                } catch (e: Exception) {
                    null
                }
                if (text == null) {
                    Toast.makeText(context, "Could not read that file", Toast.LENGTH_LONG).show()
                } else {
                    pendingImport = text
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "header") {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // ------------------------------------------------------------ Perfil
        item(key = "profile") {
            SectionLabel("You")
            JarvisCard(padding = 12.dp, modifier = Modifier.padding(top = 8.dp)) {
                ValueRow("Name", settings.userName.ifBlank { "Not set" }, onClick = { showName = true })
            }
        }

        // ------------------------------------------------------------- Clima
        item(key = "weather") {
            SectionLabel("Weather")
            JarvisCard(padding = 12.dp, modifier = Modifier.padding(top = 8.dp)) {
                ValueRow("City", settings.city?.label ?: "Choose a city", onClick = { showCity = true })
                HorizontalDivider(color = Hairline)
                ChoiceRow(
                    label = "Units",
                    value = if (settings.fahrenheit) "Fahrenheit (°F)" else "Celsius (°C)",
                    options = listOf(false to "Celsius (°C)", true to "Fahrenheit (°F)"),
                    onSelect = { f -> AppGraph.settings.update { it.copy(fahrenheit = f) } }
                )
                Text(
                    "Weather data by Open-Meteo.com. No GPS is used.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
        }

        // ------------------------------------------------------------ Alarma
        item(key = "alarm") {
            SectionLabel("Morning alarm")
            JarvisCard(padding = 12.dp, modifier = Modifier.padding(top = 8.dp)) {
                SwitchRow("Alarm", settings.alarmEnabled) { on ->
                    updateAlarm(context) { it.copy(alarmEnabled = on) }
                }
                HorizontalDivider(color = Hairline)
                ValueRow(
                    "Time",
                    Formats.time(settings.alarmMinutes, is24),
                    onClick = { showAlarmTime = true }
                )
                DayPicker(
                    mask = settings.alarmDays,
                    onChange = { days -> updateAlarm(context) { it.copy(alarmDays = days) } },
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
                HorizontalDivider(color = Hairline)
                ChoiceRow(
                    label = "Snooze",
                    value = "${settings.snoozeMinutes} minutes",
                    options = listOf(5, 10, 15, 20).map { it to "$it minutes" },
                    onSelect = { m -> AppGraph.settings.update { it.copy(snoozeMinutes = m) } }
                )
                SwitchRow("JARVIS speaks the briefing", settings.voiceEnabled) { on ->
                    AppGraph.settings.update { it.copy(voiceEnabled = on) }
                }
                if (settings.voiceEnabled) {
                    ChoiceRow(
                        label = "Tone before the voice",
                        value = "${settings.toneSeconds} seconds",
                        options = listOf(10, 20, 30, 60).map { it to "$it seconds" },
                        onSelect = { s -> AppGraph.settings.update { it.copy(toneSeconds = s) } }
                    )
                    ValueRow(
                        label = "Voice",
                        value = if (settings.voiceName.isBlank()) "Automatic (deep male)" else "Chosen by you",
                        onClick = { showVoices = true }
                    )
                    ChoiceRow(
                        label = "Voice tone",
                        value = VoicePicker.pitchLabel(settings.voicePitch),
                        options = listOf(
                            VoicePicker.PITCH_NORMAL, VoicePicker.PITCH_DEEP, VoicePicker.PITCH_VERY_DEEP
                        ).map { it to VoicePicker.pitchLabel(it) },
                        onSelect = { p -> AppGraph.settings.update { it.copy(voicePitch = p) } }
                    )
                    ChoiceRow(
                        label = "Voice speed",
                        value = speedLabel(settings.speechRate),
                        options = listOf(0.85f, 1.0f, 1.15f).map { it to speedLabel(it) },
                        onSelect = { r -> AppGraph.settings.update { it.copy(speechRate = r) } }
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val name = settings.userName.trim()
                            scope.launch {
                                val ok = speaker.speak(
                                    listOf(
                                        if (name.isEmpty()) "Hello. JARVIS is online." else "Hello, $name. JARVIS is online.",
                                        "This is how I will sound every morning."
                                    ),
                                    VoiceStyle.from(settings)
                                )
                                if (!ok) {
                                    Toast.makeText(
                                        context,
                                        "No text-to-speech voice found. Open Voice settings below.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Test voice") }
                    OutlinedButton(
                        onClick = { AlarmService.fire(context) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Test alarm") }
                }
            }
        }

        // ------------------------------------------------------------ Rutina
        item(key = "routine") {
            SectionLabel("Morning routine")
            JarvisCard(padding = 12.dp, modifier = Modifier.padding(top = 8.dp)) {
                ValueRow(
                    "Steps",
                    if (data.routineSteps.size == 1) "1 step" else "${data.routineSteps.size} steps",
                    onClick = onEditRoutine
                )
            }
        }

        // ---------------------------------------------- Permisos del celular
        item(key = "device") {
            SectionLabel("Make the alarm reliable")
            DeviceSetupCard(modifier = Modifier.padding(top = 8.dp))
        }

        // ------------------------------------------------- Copia de seguridad
        item(key = "backup") {
            SectionLabel("Backup")
            JarvisCard(padding = 12.dp, modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    "Everything stays on this phone. Export a file to keep a copy or move to another phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    OutlinedButton(
                        onClick = { exporter.launch("jarvis-backup-${LocalDate.now()}.json") },
                        modifier = Modifier.weight(1f)
                    ) { Text("Export") }
                    OutlinedButton(
                        // Algunos gestores de archivos no reconocen el tipo JSON: se aceptan todos.
                        onClick = { importer.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Import") }
                }
            }
        }

        item(key = "about") {
            Text(
                "JARVIS ${appVersion(context)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    if (showName) {
        TextDialog(
            title = "Your name",
            initial = settings.userName,
            hint = "JARVIS greets you with this name",
            onConfirm = { name ->
                AppGraph.settings.update { it.copy(userName = name.trim()) }
                showName = false
            },
            onDismiss = { showName = false }
        )
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
    if (showVoices) {
        VoiceDialog(
            speaker = speaker,
            settings = settings,
            onDismiss = {
                speaker.stop()
                showVoices = false
            }
        )
    }
    if (showAlarmTime) {
        TimeDialog(
            initialMinutes = settings.alarmMinutes,
            title = "Alarm time",
            onConfirm = { minutes ->
                updateAlarm(context) { it.copy(alarmHour = minutes / 60, alarmMinute = minutes % 60, alarmEnabled = true) }
                showAlarmTime = false
            },
            onDismiss = { showAlarmTime = false }
        )
    }
    pendingImport?.let { json ->
        ConfirmDialog(
            title = "Replace everything?",
            text = "Importing a backup replaces all your current tasks, habits, notes and settings.",
            confirmLabel = "Import",
            onConfirm = {
                pendingImport = null
                scope.launch {
                    val message = try {
                        val backup = BackupCodec.decode(json)
                        AppGraph.repo.importData(backup)
                        AppGraph.settings.replace(backup.settings)
                        AppGraph.weather.clear()
                        AlarmScheduler.schedule(context)
                        ReminderScheduler.sync(context)
                        "Backup imported"
                    } catch (e: IllegalArgumentException) {
                        e.message ?: "This file is not a JARVIS backup."
                    } catch (e: Exception) {
                        "This file is not a valid JARVIS backup."
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            },
            onDismiss = { pendingImport = null }
        )
    }
}

/** Guarda la ciudad elegida y descarta el clima de la anterior. */
internal fun setCity(city: City) {
    AppGraph.weather.clear()
    AppGraph.settings.update {
        it.copy(
            cityName = city.name,
            cityRegion = city.region,
            cityCountry = city.country,
            cityLat = city.lat,
            cityLon = city.lon
        )
    }
}

/**
 * Elegir la voz de JARVIS. "Automatic" busca la voz masculina del celular;
 * debajo salen todas las voces en ingles instaladas. Al tocar una, se
 * selecciona y dice una frase de muestra con el tono elegido.
 */
@Composable
private fun VoiceDialog(speaker: Speaker, settings: Settings, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    // null = todavia cargando la lista del motor de voz.
    var voices by remember { mutableStateOf<List<VoiceOption>?>(null) }
    androidx.compose.runtime.LaunchedEffect(Unit) { voices = speaker.englishVoices() }

    fun pick(name: String) {
        AppGraph.settings.update { it.copy(voiceName = name) }
        val style = VoiceStyle.from(AppGraph.settings.value)
        scope.launch {
            speaker.stop()
            speaker.speak(listOf("Good morning. This is how I will sound."), style)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("JARVIS voice") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Tap a voice to hear it. The tone setting makes any voice deeper.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.height(8.dp))
                VoiceRow(
                    title = "Automatic",
                    detail = "The deepest male voice on this phone",
                    selected = settings.voiceName.isBlank(),
                    onClick = { pick("") }
                )
                val list = voices
                when {
                    list == null -> Text("Loading voices…", color = TextSecondary)
                    list.isEmpty() -> Text(
                        "No English voices found. Tap Voice settings below to install " +
                            "Google's speech engine and download English.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Warning
                    )
                    else -> for (voice in list) {
                        VoiceRow(
                            title = VoicePicker.label(voice),
                            detail = voice.name,
                            selected = settings.voiceName == voice.name,
                            onClick = { pick(voice.name) }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun VoiceRow(title: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (selected) Cyan else TextPrimary)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        if (selected) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = Cyan, modifier = Modifier.size(20.dp))
        }
    }
}

private fun speedLabel(rate: Float): String = when {
    rate < 0.95f -> "Slow"
    rate > 1.05f -> "Fast"
    else -> "Normal"
}

private fun appVersion(context: Context): String =
    try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    } catch (e: Exception) {
        ""
    }

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Dialogo con un solo campo de texto. */
@Composable
internal fun TextDialog(
    title: String,
    initial: String,
    hint: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                supportingText = { Text(hint) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onConfirm(text) }),
                singleLine = true
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * Buscador de ciudades (Open-Meteo). Escribes el nombre, tocas buscar y
 * eliges de la lista. No usa GPS.
 */
@Composable
internal fun CityDialog(onPick: (City) -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<City>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    fun search() {
        if (query.trim().length < 2 || loading) return
        loading = true
        error = false
        scope.launch {
            try {
                results = AppGraph.weather.searchCities(query)
            } catch (e: Exception) {
                error = true
                results = null
            }
            loading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose your city") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("City name") },
                    trailingIcon = {
                        IconButton(onClick = { search() }) {
                            Icon(Icons.Rounded.Search, contentDescription = "Search")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(onSearch = { search() }),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                val found = results
                when {
                    loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Searching…", color = TextSecondary)
                    }
                    error -> Text("Could not search. Check your internet connection.", color = Warning)
                    found == null -> Text("Type a city and tap search.", color = TextSecondary)
                    found.isEmpty() -> Text("No cities found with that name.", color = TextSecondary)
                    else -> Column(
                        Modifier
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        for (city in found) {
                            Text(
                                city.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(city) }
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * Lista de comprobacion de lo que el celular debe permitir para que la alarma
 * suene siempre. Cada fila muestra si esta bien y, si no, un boton para ir al
 * ajuste correcto. Se vuelve a comprobar cada vez que regresas a la app.
 */
@Composable
internal fun DeviceSetupCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    val askNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh++ }

    // "refresh" solo sirve para que Compose recalcule estas comprobaciones.
    val checks = remember(refresh) { DeviceChecks.read(context) }

    JarvisCard(modifier = modifier, padding = 12.dp) {
        CheckRow(
            ok = checks.notifications,
            title = "Notifications",
            detail = "Needed for reminders and for the alarm screen.",
            action = "Allow"
        ) {
            val canAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (canAsk) {
                askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                open(
                    context,
                    Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                )
            }
        }
        CheckRow(
            ok = checks.exactAlarms,
            title = "Exact alarms",
            detail = "Lets the alarm ring at the exact minute.",
            action = "Allow"
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                open(context, Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri(context)))
            }
        }
        CheckRow(
            ok = checks.fullScreen,
            title = "Full-screen alarm",
            detail = "Shows the alarm over the lock screen.",
            action = "Allow"
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                open(context, Intent(AndroidSettings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri(context)))
            }
        }
        CheckRow(
            ok = checks.batteryUnrestricted,
            title = "Battery: no restrictions",
            detail = "Stops the phone from putting JARVIS to sleep.",
            action = "Open"
        ) {
            open(context, Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Hairline)
        Text(
            "On Xiaomi (HyperOS/MIUI) also turn on Autostart, set Battery saver to " +
                "No restrictions, and under Other permissions allow \"Show on Lock screen\" " +
                "and \"Display pop-up windows while running in the background\".",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
            OutlinedButton(
                onClick = { open(context, Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context))) },
                modifier = Modifier.weight(1f)
            ) { Text("App settings") }
            OutlinedButton(
                onClick = { open(context, Intent("com.android.settings.TTS_SETTINGS")) },
                modifier = Modifier.weight(1f)
            ) { Text("Voice settings") }
        }
    }
}

@Composable
private fun CheckRow(ok: Boolean, title: String, detail: String, action: String, onFix: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
            contentDescription = if (ok) "OK" else "Needs attention",
            tint = if (ok) Cyan else Warning,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        if (!ok) {
            Button(onClick = onFix, contentPadding = PaddingValues(horizontal = 14.dp)) { Text(action) }
        }
    }
}

private fun packageUri(context: Context): Uri = Uri.parse("package:${context.packageName}")

/** Abre una pantalla de Ajustes; si ese celular no la tiene, abre la ficha de la app. */
private fun open(context: Context, intent: Intent) {
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        try {
            context.startActivity(
                Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e2: Exception) {
            Toast.makeText(context, "Could not open Settings", Toast.LENGTH_SHORT).show()
        }
    }
}

/** Estado de los permisos que afectan a la alarma. */
internal data class DeviceChecks(
    val notifications: Boolean,
    val exactAlarms: Boolean,
    val fullScreen: Boolean,
    val batteryUnrestricted: Boolean
) {
    companion object {
        fun read(context: Context): DeviceChecks {
            val alarms = context.getSystemService(AlarmManager::class.java)
            val notifications = context.getSystemService(NotificationManager::class.java)
            val power = context.getSystemService(PowerManager::class.java)
            return DeviceChecks(
                notifications = Notifications.enabled(context),
                exactAlarms = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms(),
                fullScreen = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                    notifications.canUseFullScreenIntent(),
                batteryUnrestricted = power.isIgnoringBatteryOptimizations(context.packageName)
            )
        }
    }
}
