package com.focuszone.app.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.focuszone.app.R
import com.focuszone.app.bloqueo.GestorSaldo
import com.focuszone.app.bloqueo.SolicitudDesbloqueo
import com.focuszone.app.logica.ReglasSaldo
import kotlinx.coroutines.launch

/**
 * Aparece cuando intentas abrir una app bloqueada fuera de una sesion:
 * "Instagram esta bloqueada. ¿Desbloquear 5 / 10 / 15 min?".
 *
 * Al elegir, se descuenta del saldo de hoy, todas tus apps distractoras
 * quedan libres ese rato y se abre la app que querias.
 */
@Composable
fun PantallaDesbloqueo(paquete: String, saldoHoy: Int) {
    val context = LocalContext.current
    val alcance = rememberCoroutineScope()
    val colores = MaterialTheme.colorScheme

    val app = remember(paquete) { leerApp(context, paquete) }
    val opciones = ReglasSaldo.opciones(saldoHoy)
    var procesando by remember { mutableStateOf(false) }

    val volver = { SolicitudDesbloqueo.limpiar() }
    BackHandler(onBack = volver)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colores.background)
            .systemBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val icono = app.icono
        if (icono != null) {
            Image(bitmap = icono, contentDescription = null, modifier = Modifier.size(72.dp))
        } else {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = colores.primary, modifier = Modifier.size(72.dp))
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.desbloqueo_titulo, app.nombre),
            style = MaterialTheme.typography.headlineMedium,
            color = colores.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.saldo_hoy, saldoHoy),
            style = MaterialTheme.typography.bodyLarge,
            color = colores.primary
        )

        Spacer(Modifier.height(32.dp))

        if (opciones.isEmpty()) {
            Text(
                text = stringResource(R.string.desbloqueo_sin_saldo),
                style = MaterialTheme.typography.bodyLarge,
                color = colores.onBackground.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = volver,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.desbloqueo_ir_a_estudiar))
            }
        } else {
            Text(
                text = stringResource(R.string.desbloqueo_pregunta),
                style = MaterialTheme.typography.bodyLarge,
                color = colores.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                opciones.forEach { minutos ->
                    Button(
                        onClick = {
                            procesando = true
                            alcance.launch {
                                if (GestorSaldo.desbloquear(context, minutos)) {
                                    SolicitudDesbloqueo.limpiar()
                                    abrirApp(context, paquete)
                                }
                                procesando = false
                            }
                        },
                        enabled = !procesando,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(R.string.minutos_corto, minutos))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.desbloqueo_detalle),
                style = MaterialTheme.typography.bodyMedium,
                color = colores.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = volver) {
                Text(stringResource(R.string.desbloqueo_volver))
            }
        }
    }
}

private class InfoApp(val nombre: String, val icono: ImageBitmap?)

private fun leerApp(context: Context, paquete: String): InfoApp {
    val pm = context.packageManager
    return try {
        @Suppress("DEPRECATION")
        val info = pm.getApplicationInfo(paquete, 0)
        InfoApp(
            nombre = pm.getApplicationLabel(info).toString(),
            icono = pm.getApplicationIcon(info).toBitmap(144, 144).asImageBitmap()
        )
    } catch (e: PackageManager.NameNotFoundException) {
        InfoApp(nombre = paquete, icono = null)
    }
}

/** Abre la app que intentabas usar, ya desbloqueada. */
private fun abrirApp(context: Context, paquete: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(paquete) ?: return
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
