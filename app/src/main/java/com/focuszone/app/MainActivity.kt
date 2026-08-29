package com.focuszone.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.focuszone.app.ui.PantallaBienvenida
import com.focuszone.app.ui.theme.FocusZoneTheme

/**
 * MainActivity es la puerta de entrada de la app: la pantalla que Android
 * abre cuando tocas el icono.
 *
 * Una "Activity" es, basicamente, una pantalla de la aplicacion. Esta app
 * usara una sola Activity y dentro de ella cambiaremos de vista con Compose.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Dibuja la app tambien detras de la barra de estado y la de navegacion,
        // para que se vea a pantalla completa (estilo moderno de Android).
        enableEdgeToEdge()

        // setContent es donde le decimos a Compose QUE dibujar.
        setContent {
            FocusZoneTheme {
                PantallaBienvenida()
            }
        }
    }
}
