package com.jarvis.app.alarm

import com.jarvis.app.logic.Briefing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** En que momento esta la alarma. */
enum class AlarmPhase {
    /** No esta sonando. */
    IDLE,

    /** Sonando: tono, vibracion y (tras unos segundos) la voz. */
    RINGING,

    /** Ya la apagaste: JARVIS repite el resumen y se muestra en pantalla. */
    BRIEFING
}

data class AlarmUi(
    val phase: AlarmPhase = AlarmPhase.IDLE,
    val briefing: Briefing? = null,
    val speaking: Boolean = false
)

/** Puente entre el servicio de la alarma y su pantalla. */
object AlarmState {
    private val state = MutableStateFlow(AlarmUi())
    val flow: StateFlow<AlarmUi> = state

    val value: AlarmUi get() = state.value

    fun update(change: (AlarmUi) -> AlarmUi) = state.update(change)

    fun reset() {
        state.value = AlarmUi()
    }
}
