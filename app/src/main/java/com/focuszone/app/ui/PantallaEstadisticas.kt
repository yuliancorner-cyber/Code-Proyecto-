package com.focuszone.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focuszone.app.R
import com.focuszone.app.datos.RepositorioEstadisticas
import com.focuszone.app.logica.CalculoEstadisticas
import com.focuszone.app.logica.EntradaHistorial
import com.focuszone.app.logica.Estadisticas
import com.focuszone.app.logica.GastoDato
import com.focuszone.app.logica.SesionDato
import com.focuszone.app.ui.componentes.GraficaSemana
import com.focuszone.app.ui.componentes.fechaRelativa
import com.focuszone.app.ui.componentes.formatearDuracion
import com.focuszone.app.ui.componentes.formatearTiempo
import com.focuszone.app.ui.theme.FocusZoneTheme

/**
 * Fase 4: estadisticas e historial.
 *
 * Arriba, cifras sueltas en tarjetas (racha, % de exito, tiempo total); en
 * medio, la grafica de la semana y el saldo; abajo, el historial, que sirve
 * tambien como "tabla" con los datos exactos de la grafica.
 */
@Composable
fun PantallaEstadisticas(onVolver: () -> Unit) {
    BackHandler(onBack = onVolver)

    val context = LocalContext.current
    val colores = MaterialTheme.colorScheme
    val estadisticas by remember { RepositorioEstadisticas.observar(context) }
        .collectAsStateWithLifecycle<Estadisticas?>(initialValue = null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colores.background)
            .systemBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onVolver) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.volver),
                    tint = colores.onBackground
                )
            }
            Text(
                text = stringResource(R.string.estadisticas_titulo),
                style = MaterialTheme.typography.titleLarge,
                color = colores.onBackground
            )
        }

        val e = estadisticas
        when {
            e == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            e.vacio -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.est_vacio),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colores.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            else -> ContenidoEstadisticas(e)
        }
    }
}

@Composable
private fun ContenidoEstadisticas(e: Estadisticas) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Cifras principales
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TarjetaCifra(
                    titulo = stringResource(R.string.est_racha),
                    valor = context.resources.getQuantityString(R.plurals.dias, e.rachaDias, e.rachaDias),
                    detalle = stringResource(R.string.est_racha_detalle),
                    modifier = Modifier.weight(1f)
                )
                TarjetaCifra(
                    titulo = stringResource(R.string.est_exito),
                    valor = e.porcentajeExito?.let { stringResource(R.string.porcentaje, it) } ?: "—",
                    detalle = stringResource(R.string.est_exito_detalle, e.completadas, e.sesionesTotales),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            TarjetaCifra(
                titulo = stringResource(R.string.est_enfoque_total),
                valor = formatearDuracion(e.minutosEnfoqueTotal),
                detalle = stringResource(R.string.est_enfoque_total_detalle),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // --- Grafica de la semana
        item {
            Tarjeta {
                Text(
                    text = stringResource(R.string.est_semana_titulo),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.est_semana_total, formatearDuracion(e.minutosSemana)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(16.dp))
                GraficaSemana(dias = e.ultimosDias)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.est_grafica_ayuda),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // --- Saldo
        item {
            Tarjeta {
                Text(
                    text = stringResource(R.string.est_saldo_titulo),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.est_saldo_hoy, e.ganadoHoy, e.gastadoHoy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Text(
                    text = stringResource(R.string.est_saldo_total, e.ganadoTotal, e.gastadoTotal),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        // --- Historial
        item {
            Text(
                text = stringResource(R.string.est_historial),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }
        items(e.historial) { entrada -> FilaHistorial(entrada) }
    }
}

@Composable
private fun Tarjeta(contenido: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) { contenido() }
    }
}

/** Una cifra grande con su titulo: mejor que una grafica para un solo numero. */
@Composable
private fun TarjetaCifra(titulo: String, valor: String, detalle: String, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    Surface(
        color = colores.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = modifier
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelLarge, color = colores.onSurface.copy(alpha = 0.7f))
            Spacer(Modifier.height(4.dp))
            Text(valor, style = MaterialTheme.typography.headlineMedium, color = colores.onSurface)
            Text(detalle, style = MaterialTheme.typography.bodyMedium, color = colores.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun FilaHistorial(entrada: EntradaHistorial) {
    val colores = MaterialTheme.colorScheme
    val fecha = fechaRelativa(entrada.cuandoMillis)

    val icono: ImageVector
    val colorIcono: Color
    val titulo: String
    val detalle: String
    val cantidad: String

    when (entrada) {
        is EntradaHistorial.Sesion -> {
            val s = entrada.dato
            if (s.completada) {
                icono = Icons.Filled.CheckCircle
                colorIcono = colores.primary
                titulo = stringResource(R.string.hist_completada, s.metaMin)
                detalle = fecha
                cantidad = stringResource(R.string.hist_mas, s.creditosMin)
            } else {
                icono = Icons.Filled.Cancel
                colorIcono = colores.error
                titulo = stringResource(R.string.hist_cancelada)
                detalle = stringResource(
                    R.string.hist_cancelada_detalle,
                    fecha,
                    formatearTiempo(s.estudiadoSeg),
                    formatearTiempo(s.metaMin * 60L)
                )
                cantidad = stringResource(R.string.hist_mas, 0)
            }
        }
        is EntradaHistorial.Gasto -> {
            icono = Icons.Filled.LockOpen
            colorIcono = colores.tertiary
            titulo = stringResource(R.string.hist_desbloqueo)
            detalle = fecha
            cantidad = stringResource(R.string.hist_menos, entrada.dato.minutos)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Icon(icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge, color = colores.onBackground)
            Text(detalle, style = MaterialTheme.typography.bodyMedium, color = colores.onBackground.copy(alpha = 0.6f))
        }
        Text(cantidad, style = MaterialTheme.typography.labelLarge, color = colores.onBackground)
    }
}

// --- Vistas previas con datos de ejemplo (Split / Design en Android Studio) ---

private fun estadisticasDeEjemplo(): Estadisticas {
    val ahora = System.currentTimeMillis()
    val dia = 24 * 60 * 60 * 1000L
    val sesiones = listOf(
        SesionDato(ahora - 1 * 3_600_000L, 25, 25 * 60L, true, 5),
        SesionDato(ahora - 1 * dia, 45, 45 * 60L, true, 9),
        SesionDato(ahora - 1 * dia, 25, 12 * 60L + 34, false, 0),
        SesionDato(ahora - 2 * dia, 60, 60 * 60L, true, 12),
        SesionDato(ahora - 4 * dia, 15, 15 * 60L, true, 3),
        SesionDato(ahora - 5 * dia, 25, 25 * 60L, true, 5)
    )
    val gastos = listOf(GastoDato(ahora - 30 * 60_000L, 5), GastoDato(ahora - dia, 10))
    return CalculoEstadisticas.calcular(sesiones, gastos, ahora)
}

@Preview(showBackground = true, name = "Estadisticas - claro", heightDp = 1400)
@Composable
private fun VistaPreviaEstadisticasClaro() {
    FocusZoneTheme(temaOscuro = false) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            ContenidoEstadisticas(estadisticasDeEjemplo())
        }
    }
}

@Preview(showBackground = true, name = "Estadisticas - oscuro", heightDp = 1400)
@Composable
private fun VistaPreviaEstadisticasOscuro() {
    FocusZoneTheme(temaOscuro = true) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            ContenidoEstadisticas(estadisticasDeEjemplo())
        }
    }
}
