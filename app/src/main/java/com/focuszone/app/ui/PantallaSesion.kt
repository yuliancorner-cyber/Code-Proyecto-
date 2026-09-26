package com.focuszone.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.focuszone.app.R
import com.focuszone.app.logica.EstadoSesion
import com.focuszone.app.logica.Recompensa
import com.focuszone.app.ui.componentes.AnilloProgreso
import com.focuszone.app.ui.componentes.MantenerPantallaEncendida
import com.focuszone.app.ui.componentes.formatearTiempo
import com.focuszone.app.ui.theme.FocusZoneTheme

/**
 * Pantalla de "bloqueo activo": se ve mientras el celular esta boca abajo.
 * Candado con anillo de progreso, cronometro y el texto "Ganando tiempo de pantalla".
 */
@Composable
fun PantallaSesion(estado: EstadoSesion.EnCurso, onAbandonar: () -> Unit) {
    MantenerPantallaEncendida()

    val metaSeg = estado.metaMin * 60L
    val colores = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colores.background)
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnilloProgreso(
            progreso = estado.estudiadoSeg.toFloat() / metaSeg,
            colorAvance = colores.primary,
            colorFondo = colores.onBackground.copy(alpha = 0.1f),
            modifier = Modifier.size(240.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = colores.primary,
                modifier = Modifier.size(72.dp)
            )
        }

        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.sesion_activa),
            style = MaterialTheme.typography.headlineMedium,
            color = colores.onBackground
        )

        Spacer(Modifier.height(8.dp))
        Text(
            text = formatearTiempo(estado.estudiadoSeg),
            style = MaterialTheme.typography.displayLarge,
            color = colores.onBackground
        )
        Text(
            text = stringResource(R.string.de_meta, formatearTiempo(metaSeg)),
            style = MaterialTheme.typography.bodyMedium,
            color = colores.onBackground.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.ganando_tiempo),
            style = MaterialTheme.typography.bodyLarge,
            color = colores.primary
        )
        Text(
            text = stringResource(R.string.al_completar, Recompensa.creditosPor(estado.metaMin)),
            style = MaterialTheme.typography.bodyMedium,
            color = colores.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))
        OutlinedButton(onClick = onAbandonar) {
            Text(stringResource(R.string.abandonar))
        }
    }
}

@Preview(showBackground = true, name = "Sesion en curso")
@Composable
private fun VistaPreviaSesion() {
    FocusZoneTheme(temaOscuro = true) {
        PantallaSesion(EstadoSesion.EnCurso(metaMin = 25, estudiadoSeg = 600), onAbandonar = {})
    }
}
