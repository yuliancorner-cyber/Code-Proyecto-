package com.focuszone.app.ui.componentes

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Anillo de progreso animado con contenido en el centro (el candado).
 *
 * - El arco solido muestra cuanto llevas de la meta (0 = nada, 1 = completada).
 * - Un punto brillante gira sin parar alrededor del anillo, para que se vea
 *   "vivo" aunque el progreso avance despacio.
 *
 * @param progreso valor entre 0 y 1.
 */
@Composable
fun AnilloProgreso(
    progreso: Float,
    colorAvance: Color,
    colorFondo: Color,
    modifier: Modifier = Modifier,
    grosor: Dp = 12.dp,
    contenido: @Composable () -> Unit
) {
    // Suaviza los saltos de progreso (cada segundo) con una transicion corta.
    val progresoAnimado by animateFloatAsState(
        targetValue = progreso.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "progreso"
    )

    val giro by rememberInfiniteTransition(label = "anillo").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 4000, easing = LinearEasing)),
        label = "giro"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val g = grosor.toPx()
            val lado = size.minDimension - g
            val esquina = Offset((size.width - lado) / 2f, (size.height - lado) / 2f)
            val area = Size(lado, lado)

            // Pista de fondo (circulo completo tenue)
            drawArc(
                color = colorFondo,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = esquina,
                size = area,
                style = Stroke(width = g)
            )
            // Avance, empezando arriba (-90°) y en sentido horario
            drawArc(
                color = colorAvance,
                startAngle = -90f,
                sweepAngle = 360f * progresoAnimado,
                useCenter = false,
                topLeft = esquina,
                size = area,
                style = Stroke(width = g, cap = StrokeCap.Round)
            )
            // Punto que gira sobre la pista
            rotate(degrees = giro) {
                drawCircle(
                    color = colorAvance.copy(alpha = 0.55f),
                    radius = g * 0.45f,
                    center = Offset(center.x, esquina.y)
                )
            }
        }
        contenido()
    }
}
