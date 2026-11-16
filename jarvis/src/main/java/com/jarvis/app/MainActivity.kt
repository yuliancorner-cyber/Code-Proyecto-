package com.jarvis.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.jarvis.app.ui.JarvisRoot
import com.jarvis.app.ui.theme.JarvisTheme

/** La unica pantalla "principal" de la app; dentro, Compose cambia de vista. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Dibuja detras de las barras del sistema, con iconos claros (tema oscuro).
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(transparent),
            navigationBarStyle = SystemBarStyle.dark(transparent)
        )

        setContent {
            JarvisTheme {
                JarvisRoot()
            }
        }
    }
}
