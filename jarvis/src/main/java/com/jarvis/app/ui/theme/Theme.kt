package com.jarvis.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Que color usa cada "rol" de Material 3. JARVIS solo tiene tema oscuro.
private val JarvisColors = darkColorScheme(
    primary = Cyan,
    onPrimary = OnCyan,
    primaryContainer = CyanContainer,
    onPrimaryContainer = TextPrimary,
    secondary = Cyan,
    onSecondary = OnCyan,
    secondaryContainer = CyanContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = Warning,
    onTertiary = Black,
    background = Black,
    onBackground = TextPrimary,
    surface = Black,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = TextSecondary,
    // Material tine las superficies elevadas con este color: negro = sin tinte.
    surfaceTint = Black,
    surfaceContainerLowest = Black,
    surfaceContainerLow = SurfaceCard,
    surfaceContainer = SurfaceCard,
    surfaceContainerHigh = SurfaceRaised,
    surfaceContainerHighest = SurfaceTop,
    outline = HairlineStrong,
    outlineVariant = Hairline,
    error = Danger,
    onError = Black,
    scrim = Black
)

/** Envuelve la interfaz para aplicarle los colores y la tipografia de JARVIS. */
@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JarvisColors,
        typography = JarvisTypography,
        content = content
    )
}
