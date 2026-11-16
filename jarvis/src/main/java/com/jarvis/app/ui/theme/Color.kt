package com.jarvis.app.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta de JARVIS: negro puro (AMOLED: los pixeles negros quedan apagados y
// ahorran bateria) con cian como unico color de acento, estilo "HUD".

// --- Fondos ---
val Black = Color(0xFF000000)            // fondo de todas las pantallas
val SurfaceCard = Color(0xFF0A1014)      // tarjetas
val SurfaceRaised = Color(0xFF111A20)    // campos de texto, dialogos
val SurfaceTop = Color(0xFF17232B)       // lo que va encima de un dialogo
val Hairline = Color(0xFF1D2B33)         // bordes finos de las tarjetas
val HairlineStrong = Color(0xFF34474F)   // bordes de campos y casillas vacias

// --- Acento ---
val Cyan = Color(0xFF00E5FF)             // acento principal
// Relleno de barras de progreso y casillas de la cuadricula. El cian brillante
// queda "lavado" como relleno grande sobre negro; este tono pasa el validador
// de la guia de visualizacion (banda de luminosidad y contraste) sobre la tarjeta.
val CyanData = Color(0xFF12A5BD)
val CyanTrack = Color(0xFF0A3540)        // "riel" de las barras de progreso (mismo tono, oscuro)
val CyanContainer = Color(0xFF06303A)    // fondo de elementos seleccionados
val OnCyan = Color(0xFF001B20)           // texto/iconos encima del cian

// --- Texto ---
val TextPrimary = Color(0xFFE8F4F7)
val TextSecondary = Color(0xFF9FB3BB)
val TextMuted = Color(0xFF6E838C)

// --- Estados (siempre acompanados de texto o icono, nunca solo el color) ---
val Danger = Color(0xFFFF6B6B)           // prioridad alta, atrasado, borrar
val Warning = Color(0xFFFFC857)          // prioridad media, avisos
val Calm = Color(0xFF8AB4F8)             // prioridad baja
