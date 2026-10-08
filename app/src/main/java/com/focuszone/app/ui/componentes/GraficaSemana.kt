package com.focuszone.app.ui.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.focuszone.app.R
import com.focuszone.app.logica.DiaEstudio
import com.focuszone.app.ui.theme.BarraClaro
import com.focuszone.app.ui.theme.BarraOscuro

/**
 * Grafica de barras: minutos de enfoque de cada uno de los ultimos 7 dias.
 *
 * Reglas de diseno que sigue:
 *  - Una sola serie -> un solo color, sin leyenda (el titulo de la tarjeta la nombra).
 *  - Barras finas, esquinas de arriba redondeadas (4 dp) y apoyadas en la linea base.
 *  - Sin numeros en todas las barras: solo en la seleccionada. Toca una barra
 *    para ver sus minutos; al abrir, esta seleccionado hoy.
 *  - Los textos van en color de texto, no en el verde de las barras.
 */
@Composable
fun GraficaSemana(dias: List<DiaEstudio>, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    val colorBarra = if (colores.surface.luminance() < 0.5f) BarraOscuro else BarraClaro
    val colorTexto = colores.onSurface
    val colorTextoSuave = colores.onSurface.copy(alpha = 0.6f)
    val colorBase = colores.onSurface.copy(alpha = 0.2f)

    // "L,M,X,J,V,S,D" -> iniciales de lunes a domingo
    val iniciales = stringResource(R.string.dias_semana_iniciales).split(',')
    fun inicial(dia: DiaEstudio) = iniciales[dia.fecha.dayOfWeek.value - 1]

    var seleccion by remember(dias) { mutableIntStateOf(dias.lastIndex) }
    val textoSeleccion = stringResource(R.string.minutos_corto, dias.getOrNull(seleccion)?.minutos ?: 0)

    val medidor = rememberTextMeasurer()
    val estiloValor = MaterialTheme.typography.labelLarge.copy(color = colorTexto, fontWeight = FontWeight.Bold)

    // Para lectores de pantalla: la grafica entera leida como texto.
    val resumen = dias.joinToString(", ") { "${inicial(it)} ${it.minutos}" }
    val descripcion = stringResource(R.string.grafica_descripcion, resumen)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .semantics { contentDescription = descripcion }
                // Toda la columna de cada dia es tocable (no solo la barra): mas facil acertar.
                .pointerInput(dias.size) {
                    detectTapGestures { toque ->
                        val anchoColumna = size.width.toFloat() / dias.size
                        seleccion = (toque.x / anchoColumna).toInt().coerceIn(0, dias.lastIndex)
                    }
                }
        ) {
            val anchoColumna = size.width / dias.size
            val anchoBarra = anchoColumna * 0.55f
            val radio = 4.dp.toPx()
            val reservaEtiqueta = 28.dp.toPx() // espacio arriba para el numero de la barra mas alta
            val altoUtil = size.height - reservaEtiqueta
            val maximo = (dias.maxOfOrNull { it.minutos } ?: 0).coerceAtLeast(1)

            dias.forEachIndexed { i, dia ->
                val izquierda = anchoColumna * i + (anchoColumna - anchoBarra) / 2f
                val alto = altoUtil * dia.minutos / maximo
                val arriba = size.height - alto

                if (dia.minutos > 0) {
                    val barra = RoundRect(
                        left = izquierda,
                        top = arriba,
                        right = izquierda + anchoBarra,
                        bottom = size.height,
                        topLeftCornerRadius = CornerRadius(radio),
                        topRightCornerRadius = CornerRadius(radio),
                        bottomRightCornerRadius = CornerRadius.Zero,
                        bottomLeftCornerRadius = CornerRadius.Zero
                    )
                    drawPath(Path().apply { addRoundRect(barra) }, colorBarra)
                }

                if (i == seleccion) {
                    val etiqueta = medidor.measure(textoSeleccion, estiloValor)
                    val x = (izquierda + anchoBarra / 2f - etiqueta.size.width / 2f)
                        .coerceIn(0f, size.width - etiqueta.size.width)
                    val y = arriba - etiqueta.size.height - 4.dp.toPx()
                    drawText(etiqueta, topLeft = Offset(x, y))
                }
            }

            // Linea base, discreta
            drawLine(
                color = colorBase,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Iniciales de los dias bajo cada barra
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            dias.forEachIndexed { i, dia ->
                Text(
                    text = inicial(dia),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (i == seleccion) colorTexto else colorTextoSuave,
                    fontWeight = if (i == seleccion) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
