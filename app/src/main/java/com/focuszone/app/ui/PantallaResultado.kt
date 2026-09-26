package com.focuszone.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.focuszone.app.R
import com.focuszone.app.logica.EstadoSesion
import com.focuszone.app.logica.MotivoCancelacion
import com.focuszone.app.ui.componentes.formatearTiempo
import com.focuszone.app.ui.theme.FocusZoneTheme

/** Resultado de una sesion: completada (con creditos) o cancelada (sin creditos). */
@Composable
fun PantallaResultado(
    estado: EstadoSesion,
    totalCreditosMin: Int,
    onVolver: () -> Unit
) {
    val colores = MaterialTheme.colorScheme

    val icono: ImageVector
    val colorIcono: Color
    val titulo: String
    val lineas: List<String>

    when (estado) {
        is EstadoSesion.Completada -> {
            icono = Icons.Filled.CheckCircle
            colorIcono = colores.primary
            titulo = stringResource(R.string.completada_titulo)
            lineas = listOf(
                stringResource(R.string.completada_ganaste, estado.creditosMin),
                stringResource(R.string.completada_total, totalCreditosMin)
            )
        }
        is EstadoSesion.Cancelada -> {
            icono = Icons.Filled.Cancel
            colorIcono = colores.error
            titulo = stringResource(R.string.cancelada_titulo)
            lineas = listOf(
                stringResource(
                    when (estado.motivo) {
                        MotivoCancelacion.LEVANTADO -> R.string.cancelada_levantado
                        MotivoCancelacion.ABANDONO -> R.string.cancelada_abandono
                    }
                ),
                stringResource(R.string.cancelada_detalle, formatearTiempo(estado.estudiadoSeg))
            )
        }
        // Esta pantalla solo se usa para los dos estados finales.
        else -> return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colores.background)
            .systemBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = colorIcono,
            modifier = Modifier.size(96.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineMedium,
            color = colores.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        lineas.forEach { linea ->
            Text(
                text = linea,
                style = MaterialTheme.typography.bodyLarge,
                color = colores.onBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.volver))
        }
    }
}

@Preview(showBackground = true, name = "Completada")
@Composable
private fun VistaPreviaCompletada() {
    FocusZoneTheme {
        PantallaResultado(EstadoSesion.Completada(25, 5), totalCreditosMin = 30, onVolver = {})
    }
}

@Preview(showBackground = true, name = "Cancelada")
@Composable
private fun VistaPreviaCancelada() {
    FocusZoneTheme(temaOscuro = true) {
        PantallaResultado(
            EstadoSesion.Cancelada(25, 312, MotivoCancelacion.LEVANTADO),
            totalCreditosMin = 30,
            onVolver = {}
        )
    }
}
