package com.focuszone.app.ui.componentes

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Muestra en vivo lo que ve la camara trasera.
 *
 * Usa CameraX, que se "ata" al ciclo de vida de la pantalla: cuando sales de
 * la app o cambias de pantalla, la camara se apaga sola (no gasta bateria
 * ni queda encendida en segundo plano).
 *
 * PreviewView es una vista "clasica" de Android (no Compose); AndroidView
 * sirve de puente para meterla dentro de una pantalla Compose.
 *
 * Requiere que el permiso de camara ya este concedido.
 */
@Composable
fun VistaCamara(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cicloDeVida = LocalLifecycleOwner.current

    val vistaPrevia = remember {
        PreviewView(context).apply {
            // Rellena toda el area, recortando bordes si hace falta (sin franjas negras).
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(cicloDeVida) {
        var activo = true
        var proveedor: ProcessCameraProvider? = null
        val futuro = ProcessCameraProvider.getInstance(context)

        // La camara tarda un momento en estar lista: cuando lo este, se ejecuta esto.
        futuro.addListener({
            if (!activo) return@addListener // la pantalla ya se cerro mientras esperabamos
            try {
                val p = futuro.get()
                proveedor = p
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(vistaPrevia.surfaceProvider)
                p.unbindAll()
                p.bindToLifecycle(cicloDeVida, CameraSelector.DEFAULT_BACK_CAMERA, preview)
            } catch (e: Exception) {
                Log.w("VistaCamara", "No se pudo abrir la camara", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            activo = false
            proveedor?.unbindAll()
        }
    }

    AndroidView(factory = { vistaPrevia }, modifier = modifier)
}
