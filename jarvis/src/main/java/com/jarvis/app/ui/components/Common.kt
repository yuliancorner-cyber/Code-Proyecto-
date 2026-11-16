package com.jarvis.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.app.logic.Days
import com.jarvis.app.logic.Priority
import com.jarvis.app.ui.theme.Calm
import com.jarvis.app.ui.theme.Cyan
import com.jarvis.app.ui.theme.CyanContainer
import com.jarvis.app.ui.theme.CyanData
import com.jarvis.app.ui.theme.CyanTrack
import com.jarvis.app.ui.theme.Danger
import com.jarvis.app.ui.theme.Hairline
import com.jarvis.app.ui.theme.HairlineStrong
import com.jarvis.app.ui.theme.OnCyan
import com.jarvis.app.ui.theme.SurfaceCard
import com.jarvis.app.ui.theme.TextMuted
import com.jarvis.app.ui.theme.TextPrimary
import com.jarvis.app.ui.theme.TextSecondary
import com.jarvis.app.ui.theme.Warning
import kotlinx.coroutines.flow.Flow

// Piezas pequenas de interfaz que se repiten en varias pantallas.

val CardShape = RoundedCornerShape(16.dp)

/** Tarjeta: fondo casi negro con un borde fino. Si recibe [onClick], se puede tocar. */
@Composable
fun JarvisCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val border = BorderStroke(1.dp, Hairline)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = CardShape,
            color = SurfaceCard,
            border = border
        ) { Column(Modifier.padding(padding), content = content) }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = CardShape,
            color = SurfaceCard,
            border = border
        ) { Column(Modifier.padding(padding), content = content) }
    }
}

/** Rotulo de seccion en mayusculas, con contenido opcional a la derecha. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) trailing()
    }
}

/**
 * Barra de progreso ("meter"): un riel oscuro y un relleno del mismo tono.
 * [fraction] va de 0 a 1.
 */
@Composable
fun Meter(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    description: String? = null
) {
    val safe = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(CyanTrack)
            .semantics { if (description != null) contentDescription = description }
    ) {
        if (safe > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(safe)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(CyanData)
            )
        }
    }
}

/** Casilla redonda: aro vacio o circulo cian con una palomita. */
@Composable
fun CheckCircle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // La zona tactil es de 44dp aunque el circulo se vea mas pequeno.
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Checkbox) { onCheckedChange(!checked) }
            .semantics { contentDescription = if (checked) "$label, done" else "$label, not done" },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Box(
                Modifier
                    .size(24.dp)
                    .background(Cyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = OnCyan, modifier = Modifier.size(16.dp))
            }
        } else {
            Box(
                Modifier
                    .size(24.dp)
                    .border(2.dp, HairlineStrong, CircleShape)
            )
        }
    }
}

/** Color con el que se pinta cada prioridad (siempre junto a su texto). */
fun priorityColor(priority: Priority): Color = when (priority) {
    Priority.HIGH -> Danger
    Priority.MEDIUM -> Warning
    Priority.LOW -> Calm
    Priority.NONE -> TextMuted
}

/** Etiqueta pequena: un punto de color + texto ("● High"). */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier, dot: Color? = null) {
    Row(
        modifier = modifier
            .border(1.dp, Hairline, CircleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (dot != null) {
            Box(
                Modifier
                    .size(7.dp)
                    .background(dot, CircleShape)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Mensaje para listas vacias. */
@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

/** Siete botones redondos para elegir dias de la semana (M T W T F S S). */
@Composable
fun DayPicker(mask: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (day in Days.ORDER) {
            val selected = Days.contains(mask, day)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (selected) CyanContainer else Color.Transparent)
                    .border(1.dp, if (selected) Cyan else HairlineStrong, CircleShape)
                    .clickable(role = Role.Checkbox) { onChange(Days.toggle(mask, day)) }
                    .semantics {
                        contentDescription = Days.shortName(day) + if (selected) ", selected" else ", not selected"
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Days.initial(day),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Cyan else TextSecondary
                )
            }
        }
    }
}

/** Fila "rotulo ........ valor" que se puede tocar (para ajustes y editores). */
@Composable
fun ValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    valueColor: Color = Cyan,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

/**
 * Observa un Flow desde una pantalla. remember evita crear el Flow de nuevo
 * en cada redibujado (lo que reiniciaria la lectura una y otra vez).
 */
@Composable
fun <T> rememberFlowState(initial: T, vararg keys: Any?, flow: () -> Flow<T>): State<T> {
    val remembered = remember(*keys) { flow() }
    return remembered.collectAsStateWithLifecycle(initial)
}

/**
 * Ejecuta una accion que escribe en la base de datos. No depende de la
 * pantalla: termina aunque la cierres enseguida, y las acciones se ejecutan
 * en orden, de una en una (ver AppGraph.run).
 */
fun launchAction(block: suspend () -> Unit) {
    com.jarvis.app.data.AppGraph.run(block)
}
