package com.focuszone.app.ui.componentes

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Dibuja la "zona de enfoque": un rectangulo con borde discontinuo del tamano
 * aproximado de un celular, centrado en el espacio que se le da, con un borde
 * que "respira".
 *
 * FASE 1: es una guia visual fija sobre la imagen de la camara, no esta
 * anclada al escritorio real. En la Fase 2 la reemplazaremos por una zona AR
 * (ARCore) que si se queda pegada a la mesa y se puede mover y redimensionar.
 *
 * @param detectado true cuando el celular ya esta boca abajo y quieto: la zona
 *        se pone del color de acento para confirmarlo.
 * @param colorDetectado color a usar cuando [detectado] es true.
 */
@Composable
fun ZonaEnfoque(
    detectado: Boolean,
    colorDetectado: Color,
    modifier: Modifier = Modifier
) {
    val color = if (detectado) colorDetectado else Color.White

    val respiracion = rememberInfiniteTransition(label = "zona")
    val opacidadBorde by respiracion.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1200), RepeatMode.Reverse),
        label = "opacidadBorde"
    )

    Canvas(modifier = modifier) {
        // Proporcion de un celular tipico (unas 2 veces mas alto que ancho).
        // Si no cabe a lo alto, se encoge entera manteniendo la proporcion.
        val proporcion = 2.05f
        var ancho = size.width * 0.46f
        var alto = ancho * proporcion
        if (alto > size.height) {
            alto = size.height
            ancho = alto / proporcion
        }
        val esquina = Offset((size.width - ancho) / 2f, (size.height - alto) / 2f)
        val tamano = Size(ancho, alto)
        val radio = CornerRadius(28.dp.toPx())

        // Relleno translucido
        drawRoundRect(
            color = color.copy(alpha = 0.14f),
            topLeft = esquina,
            size = tamano,
            cornerRadius = radio
        )
        // Borde discontinuo: trazos de 22 dp con huecos de 14 dp
        drawRoundRect(
            color = color.copy(alpha = opacidadBorde),
            topLeft = esquina,
            size = tamano,
            cornerRadius = radio,
            style = Stroke(
                width = 3.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(22.dp.toPx(), 14.dp.toPx())
                )
            )
        )
    }
}
