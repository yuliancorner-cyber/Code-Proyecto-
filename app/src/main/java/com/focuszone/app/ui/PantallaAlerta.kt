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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuszone.app.R
import com.focuszone.app.logica.EstadoSesion
import com.focuszone.app.ui.componentes.MantenerPantallaEncendida
import com.focuszone.app.ui.componentes.formatearTiempo
import com.focuszone.app.ui.theme.AmbarAlerta
import com.focuszone.app.ui.theme.AzulNoche
import com.focuszone.app.ui.theme.FocusZoneTheme

/**
 * Alerta de trampa: levantaste o moviste el celular antes de terminar.
 * Pantalla ambar con cuenta atras; si no lo vuelves a dejar, se cancela.
 */
@Composable
fun PantallaAlerta(estado: EstadoSesion.Alerta, onAbandonar: () -> Unit) {
    MantenerPantallaEncendida()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmbarAlerta)
            .systemBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = AzulNoche,
            modifier = Modifier.size(88.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.alerta_trampa),
            style = MaterialTheme.typography.headlineMedium,
            color = AzulNoche,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))
        // Cuenta atras grande
        Text(
            text = estado.restanteSeg.toString(),
            fontSize = 96.sp,
            fontWeight = FontWeight.Bold,
            color = AzulNoche
        )
        Text(
            text = stringResource(R.string.alerta_reanudar),
            style = MaterialTheme.typography.bodyLarge,
            color = AzulNoche,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.alerta_llevas, formatearTiempo(estado.estudiadoSeg)),
            style = MaterialTheme.typography.bodyMedium,
            color = AzulNoche.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(40.dp))
        OutlinedButton(
            onClick = onAbandonar,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AzulNoche)
        ) {
            Text(stringResource(R.string.abandonar))
        }
    }
}

@Preview(showBackground = true, name = "Alerta")
@Composable
private fun VistaPreviaAlerta() {
    FocusZoneTheme {
        PantallaAlerta(EstadoSesion.Alerta(metaMin = 25, estudiadoSeg = 754, restanteSeg = 4), onAbandonar = {})
    }
}
