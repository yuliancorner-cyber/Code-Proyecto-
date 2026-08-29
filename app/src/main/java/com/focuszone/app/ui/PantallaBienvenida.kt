package com.focuszone.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.focuszone.app.R
import com.focuszone.app.ui.theme.FocusZoneTheme

/**
 * Pantalla inicial de la Fase 0.
 *
 * Todavia no hace nada funcional: su unico proposito es confirmar que el
 * proyecto compila, se instala y dibuja correctamente en tu celular.
 * En la Fase 1 la reemplazaremos por la vista de camara + zona de enfoque.
 */
@Composable
fun PantallaBienvenida(modifier: Modifier = Modifier) {

    // Scaffold es el "andamio" de una pantalla Material: se encarga de dejar
    // espacio para barras del sistema, barra superior, botones flotantes, etc.
    Scaffold(modifier = modifier.fillMaxSize()) { espaciadoInterno ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(espaciadoInterno)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Circulo con el candado: sera el simbolo de "sesion activa".
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(112.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null, // decorativo: no lo lee el lector de pantalla
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 28.dp)
            )

            Text(
                text = stringResource(R.string.bienvenida_lema),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )

            Text(
                text = stringResource(R.string.bienvenida_estado_fase),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 40.dp)
            )
        }
    }
}

// @Preview permite ver la pantalla dentro de Android Studio sin instalar la app.
@Preview(showBackground = true, name = "Bienvenida - oscuro")
@Composable
private fun VistaPreviaBienvenidaOscura() {
    FocusZoneTheme(temaOscuro = true) { PantallaBienvenida() }
}

@Preview(showBackground = true, name = "Bienvenida - claro")
@Composable
private fun VistaPreviaBienvenidaClara() {
    FocusZoneTheme(temaOscuro = false) { PantallaBienvenida() }
}
