package com.focuszone.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.focuszone.app.R
import com.focuszone.app.logica.EstadoSesion
import com.focuszone.app.logica.Recompensa
import com.focuszone.app.ui.componentes.MantenerPantallaEncendida
import com.focuszone.app.ui.componentes.VistaCamara
import com.focuszone.app.ui.componentes.ZonaEnfoque
import com.focuszone.app.ui.theme.VerdeEnfoque

// Paneles oscuros translucidos: se leen bien sobre cualquier imagen de camara.
private val FondoPanel = Color.Black.copy(alpha = 0.6f)
private val TextoPanel = Color.White
private val TextoPanelSuave = Color.White.copy(alpha = 0.75f)
// Los paneles son siempre oscuros: usamos el verde brillante en ambos temas.
private val AcentoPanel = VerdeEnfoque

/**
 * Pantalla principal: camara con la zona de enfoque encima.
 *
 * Tiene dos modos:
 *  - Sin sesion ([esperando] = null): eliges la meta y pulsas "Iniciar sesion".
 *  - Esperando: ya pulsaste Iniciar y la app espera a que dejes el celular
 *    boca abajo y quieto. La zona se pone verde al detectarlo.
 */
@Composable
fun PantallaInicio(
    totalCreditosMin: Int,
    esperando: EstadoSesion.Esperando?,
    onIniciar: (metaMin: Int) -> Unit,
    onCancelar: () -> Unit
) {
    val context = LocalContext.current

    // --- Permiso de camara -------------------------------------------------
    var tienePermisoCamara by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    // "Launcher" = el dialogo de Android que pregunta "¿Permitir que FocusZone...?"
    val pedirCamara = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> tienePermisoCamara = concedido }

    LaunchedEffect(Unit) {
        if (!tienePermisoCamara) pedirCamara.launch(Manifest.permission.CAMERA)
    }

    // --- Meta elegida (rememberSaveable: sobrevive a girar la pantalla) ------
    var metaElegida by rememberSaveable { mutableIntStateOf(Recompensa.META_POR_DEFECTO) }

    // --- Permiso de notificaciones (Android 13+), se pide al iniciar ---------
    // Aunque lo rechaces, la sesion arranca igual (solo no veras la notificacion).
    val pedirNotificaciones = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> onIniciar(metaElegida) }

    val alPulsarIniciar: () -> Unit = {
        val faltaPermiso = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (faltaPermiso) {
            pedirNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onIniciar(metaElegida)
        }
    }

    if (esperando != null) MantenerPantallaEncendida()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        if (tienePermisoCamara) {
            VistaCamara(modifier = Modifier.fillMaxSize())
        } else {
            Text(
                text = stringResource(R.string.sin_permiso_camara),
                color = TextoPanelSuave,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 48.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(16.dp)
        ) {
            PanelSuperior(totalCreditosMin)

            // La zona ocupa solo el hueco libre entre los dos paneles
            // (weight = "todo el espacio que sobre"), asi nunca queda tapada.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                ZonaEnfoque(
                    detectado = esperando?.enPosicion == true,
                    colorDetectado = AcentoPanel,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (esperando == null) {
                PanelElegirMeta(
                    metaElegida = metaElegida,
                    onElegir = { metaElegida = it },
                    onIniciar = alPulsarIniciar
                )
            } else {
                PanelEsperando(esperando, onCancelar)
            }
        }
    }
}

@Composable
private fun PanelSuperior(totalCreditosMin: Int) {
    Surface(color = FondoPanel, shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = stringResource(R.string.guia_colocar_zona),
                color = TextoPanel,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = AcentoPanel,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.creditos_totales, totalCreditosMin),
                    color = AcentoPanel,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun PanelElegirMeta(
    metaElegida: Int,
    onElegir: (Int) -> Unit,
    onIniciar: () -> Unit
) {
    Surface(color = FondoPanel, shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = stringResource(R.string.meta_titulo),
                color = TextoPanel,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(12.dp))

            // Una "ficha" por cada meta posible
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Recompensa.METAS_MINUTOS.forEach { meta ->
                    FichaMeta(
                        minutos = meta,
                        seleccionada = meta == metaElegida,
                        onClick = { onElegir(meta) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.recompensa_meta, Recompensa.creditosPor(metaElegida)),
                color = TextoPanelSuave,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onIniciar,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.boton_iniciar_sesion))
            }
        }
    }
}

@Composable
private fun FichaMeta(
    minutos: Int,
    seleccionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fondo = if (seleccionada) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f)
    val texto = if (seleccionada) MaterialTheme.colorScheme.onPrimary else TextoPanel
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(fondo)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.minutos_corto, minutos),
            color = texto,
            fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun PanelEsperando(esperando: EstadoSesion.Esperando, onCancelar: () -> Unit) {
    Surface(color = FondoPanel, shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Smartphone,
                contentDescription = null,
                tint = if (esperando.enPosicion) AcentoPanel else TextoPanel,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(
                    if (esperando.enPosicion) R.string.esperando_detectado else R.string.esperando_colocar
                ),
                color = if (esperando.enPosicion) AcentoPanel else TextoPanel,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.minutos_meta_elegida, esperando.metaMin),
                color = TextoPanelSuave,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onCancelar) {
                Text(stringResource(R.string.cancelar), color = TextoPanel)
            }
        }
    }
}
