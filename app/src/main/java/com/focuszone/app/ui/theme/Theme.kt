package com.focuszone.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Un "esquema de color" agrupa que color usa cada rol de la interfaz
// (fondo, superficie, acento, error...). Material 3 los aplica solo.

private val EsquemaOscuro = darkColorScheme(
    primary = VerdeEnfoque,
    onPrimary = AzulNoche,
    primaryContainer = VerdeEnfoqueOscuro,
    onPrimaryContainer = GrisTexto,
    background = AzulNoche,
    onBackground = GrisTexto,
    surface = AzulNocheSuave,
    onSurface = GrisTexto,
    tertiary = AmbarAlerta,
    error = RojoCancelado
)

private val EsquemaClaro = lightColorScheme(
    primary = VerdeClaro,
    onPrimary = SuperficieClara,
    // Sin esto, Material usaba su lila por defecto (el circulo de la Fase 0).
    primaryContainer = VerdeMenta,
    onPrimaryContainer = VerdeClaro,
    background = FondoClaro,
    onBackground = GrisTextoOscuro,
    surface = SuperficieClara,
    onSurface = GrisTextoOscuro,
    tertiary = AmbarClaro,
    error = RojoClaro
)

/**
 * Envuelve toda la interfaz de la app para aplicarle colores y tipografia.
 *
 * Un "@Composable" es una funcion que dibuja parte de la pantalla.
 * Este de aqui no dibuja nada por si mismo: solo prepara el tema para
 * todo lo que este dentro de [contenido].
 *
 * @param temaOscuro si es true usa la paleta oscura. Por defecto sigue la
 *        configuracion del sistema (Ajustes > Pantalla > Tema oscuro).
 */
@Composable
fun FocusZoneTheme(
    temaOscuro: Boolean = isSystemInDarkTheme(),
    contenido: @Composable () -> Unit
) {
    val esquema = if (temaOscuro) EsquemaOscuro else EsquemaClaro

    val vista = LocalView.current
    // isInEditMode es true en la vista previa de Android Studio, donde no
    // existe una ventana real: ahi nos saltamos este ajuste.
    if (!vista.isInEditMode) {
        SideEffect {
            val ventana = (vista.context as Activity).window
            // Iconos de la barra de estado (reloj, bateria) claros u oscuros
            // segun el tema, para que siempre se lean.
            WindowCompat.getInsetsController(ventana, vista)
                .isAppearanceLightStatusBars = !temaOscuro
        }
    }

    MaterialTheme(
        colorScheme = esquema,
        typography = Tipografia,
        content = contenido
    )
}
