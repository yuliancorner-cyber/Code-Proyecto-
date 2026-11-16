package com.jarvis.app.alarm

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.app.MainActivity
import com.jarvis.app.data.AppGraph
import com.jarvis.app.logic.BriefingBuilder
import com.jarvis.app.logic.Formats
import com.jarvis.app.ui.components.CheckCircle
import com.jarvis.app.ui.components.JarvisCard
import com.jarvis.app.ui.components.is24Hour
import com.jarvis.app.ui.components.launchAction
import com.jarvis.app.ui.components.rememberFlowState
import com.jarvis.app.ui.theme.Black
import com.jarvis.app.ui.theme.Cyan
import com.jarvis.app.ui.theme.JarvisTheme
import com.jarvis.app.ui.theme.TextMuted
import com.jarvis.app.ui.theme.TextPrimary
import com.jarvis.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime

/**
 * La pantalla de la alarma. Se muestra encima de la pantalla de bloqueo y
 * enciende la pantalla. Tiene dos momentos:
 *  - Sonando: la hora, el saludo y los botones "Snooze" y "I'm up".
 *  - Despues de "I'm up": el resumen del dia y la rutina de la manana.
 */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(transparent),
            navigationBarStyle = SystemBarStyle.dark(transparent)
        )

        setContent {
            JarvisTheme {
                val state by AlarmState.flow.collectAsStateWithLifecycle()

                // Si la alarma ya no esta activa (pospuesta, detenida), se cierra sola.
                LaunchedEffect(state.phase) {
                    if (state.phase == AlarmPhase.IDLE) finish()
                }
                // Mientras suena, el boton "atras" no hace nada: hay que elegir.
                BackHandler(enabled = state.phase == AlarmPhase.RINGING) { }

                Surface(modifier = Modifier.fillMaxSize(), color = Black) {
                    when (state.phase) {
                        AlarmPhase.RINGING -> RingingScreen(
                            speaking = state.speaking,
                            onSnooze = { AlarmService.snooze(this) },
                            onDismiss = { AlarmService.dismiss(this) }
                        )
                        AlarmPhase.BRIEFING -> BriefingScreen(
                            state = state,
                            onStopVoice = { AlarmService.stop(this) },
                            onDone = { startMyDay() }
                        )
                        AlarmPhase.IDLE -> Unit
                    }
                }
            }
        }
    }

    /** Cierra la alarma y abre la app en la pantalla de inicio. */
    private fun startMyDay() {
        AlarmService.stop(this)
        AlarmState.reset()
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        // Que la pantalla no se apague sola mientras la alarma esta a la vista.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, AlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }
}

/** La hora actual en minutos, actualizada cada pocos segundos. */
@Composable
private fun rememberNowMinutes(): Int {
    val minutes by produceState(initialValue = LocalTime.now().let { it.hour * 60 + it.minute }) {
        while (true) {
            delay(5_000)
            value = LocalTime.now().let { it.hour * 60 + it.minute }
        }
    }
    return minutes
}

@Composable
internal fun RingingScreen(speaking: Boolean, onSnooze: () -> Unit, onDismiss: () -> Unit) {
    val settings by AppGraph.settings.flow.collectAsStateWithLifecycle()
    val now = rememberNowMinutes()
    val name = settings.userName.trim()
    val hello = BriefingBuilder.greetingWord(now / 60)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Text("JARVIS", style = MaterialTheme.typography.labelMedium, color = Cyan)
        Spacer(Modifier.height(12.dp))
        Text(
            Formats.time(now, is24Hour()),
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary
        )
        Text(
            Formats.fullDate(LocalDate.now()),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
        Spacer(Modifier.height(32.dp))
        Text(
            if (name.isEmpty()) "$hello." else "$hello, $name.",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint = if (speaking) Cyan else TextMuted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (speaking) "Reading your briefing…" else "Time to get up.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }
        Spacer(Modifier.weight(1f))

        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(
                "I'm up",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onSnooze, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Snooze ${settings.snoozeMinutes} min",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
internal fun BriefingScreen(state: AlarmUi, onStopVoice: () -> Unit, onDone: () -> Unit) {
    val today = LocalDate.now().toEpochDay()
    val steps by rememberFlowState(emptyList()) { AppGraph.repo.routine.observeSteps() }
    val checks by rememberFlowState(emptyList(), today) { AppGraph.repo.routine.observeChecks(today) }
    val doneIds = checks.map { it.stepId }.toSet()
    val briefing = state.briefing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text("JARVIS", style = MaterialTheme.typography.labelMedium, color = Cyan)
            Text(
                briefing?.greeting ?: "Good morning.",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary
            )

            if (briefing == null) {
                Text("Preparing your briefing…", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
            } else {
                JarvisCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (line in briefing.lines) {
                            Text(line, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                        }
                    }
                }
            }

            if (steps.isNotEmpty()) {
                Text(
                    "MORNING ROUTINE",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                JarvisCard(padding = 6.dp) {
                    for (step in steps) {
                        val done = step.id in doneIds
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CheckCircle(
                                checked = done,
                                onCheckedChange = { launchAction { AppGraph.repo.setRoutineDone(step.id, today, it) } },
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
            Spacer(Modifier.height(8.dp))
        }

        if (state.speaking) {
            OutlinedButton(onClick = onStopVoice, modifier = Modifier.fillMaxWidth()) {
                Text("Stop the voice", modifier = Modifier.padding(vertical = 6.dp))
            }
            Spacer(Modifier.height(10.dp))
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Start my day",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}
