package com.focuszone.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.focuszone.app.bloqueo.ModoSilencio
import com.focuszone.app.logica.EstadoSesion
import com.focuszone.app.sesion.SesionActual
import com.focuszone.app.ui.AppFocusZone
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
        // Debe ir ANTES de super.onCreate: muestra la pantalla de carga y luego
        // pasa al tema normal de la app.
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Dibuja la app tambien detras de la barra de estado y la de navegacion,
        // para que se vea a pantalla completa (estilo moderno de Android).
        enableEdgeToEdge()

        // Si no hay ninguna sesion en marcha pero quedo activado nuestro
        // No molestar (p. ej. porque Android cerro la app a mitad de sesion),
        // lo devolvemos a la normalidad.
        if (SesionActual.estado.value == EstadoSesion.Inactivo) {
            ModoSilencio.restaurar(this)
        }

        // setContent es donde le decimos a Compose QUE dibujar.
        setContent {
            FocusZoneTheme {
                AppFocusZone()
            }
        }
    }
}
