package com.focuszone.app.bloqueo

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.focuszone.app.R

/**
 * La "cortina": una pantalla de FocusZone que tapa Instagram cuando sales de
 * los chats con "solo chats" activo. Asi no ves nada (inicio, reels...)
 * mientras FocusZone te lleva de vuelta a los mensajes.
 *
 * Es una ventana especial de los servicios de accesibilidad
 * (TYPE_ACCESSIBILITY_OVERLAY): queda encima de todo y no necesita ningun
 * permiso extra. Esta hecha con vistas clasicas de Android (no Compose)
 * porque vive dentro del servicio, no de una pantalla de la app.
 */
class CortinaSoloChats(
    private val servicio: AccessibilityService,
    private val onIrAChats: () -> Unit,
    private val onDesbloquear: () -> Unit,
    private val onSalir: () -> Unit
) {
    private companion object {
        const val TAG = "CortinaSoloChats"
        const val FONDO = 0xF2101418.toInt()
        const val TEXTO = 0xFFE8EAED.toInt()
        const val TEXTO_SUAVE = 0xB3E8EAED.toInt()
    }

    private val ventanas = servicio.getSystemService(WindowManager::class.java)
    private var vista: View? = null

    val visible: Boolean get() = vista != null

    /** Muestra la cortina. false si Android no la dejo poner (raro). */
    fun mostrar(): Boolean {
        if (vista != null) return true
        val nueva = crearVista()
        val parametros = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            // NOT_FOCUSABLE: el boton "atras" y el teclado siguen yendo a
            // Instagram, que esta debajo. Los toques si los recibe la cortina.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        return try {
            ventanas.addView(nueva, parametros)
            vista = nueva
            true
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo mostrar la cortina", e)
            false
        }
    }

    fun ocultar() {
        val actual = vista ?: return
        vista = null
        try {
            ventanas.removeView(actual)
        } catch (e: Exception) {
            Log.w(TAG, "La cortina ya no estaba", e)
        }
    }

    private fun crearVista(): View {
        val contexto = servicio
        fun dp(valor: Int): Int = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, valor.toFloat(), contexto.resources.displayMetrics
        ).toInt()

        fun texto(res: Int, tamanoSp: Float, color: Int) = TextView(contexto).apply {
            setText(res)
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, tamanoSp)
            gravity = Gravity.CENTER
        }

        fun boton(res: Int, accion: () -> Unit) = Button(contexto).apply {
            setText(res)
            isAllCaps = false
            setOnClickListener { accion() }
        }

        return LinearLayout(contexto).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(FONDO)
            setPadding(dp(32), dp(32), dp(32), dp(32))
            // Toques en el fondo: se los queda la cortina (no pasan a Instagram).
            isClickable = true

            val separacion = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }

            addView(texto(R.string.solo_chats_aviso, 24f, TEXTO))
            addView(texto(R.string.cortina_texto, 16f, TEXTO_SUAVE), separacion)
            addView(boton(R.string.cortina_ir_a_chats, onIrAChats), LinearLayout.LayoutParams(separacion).apply { topMargin = dp(32) })
            addView(boton(R.string.cortina_desbloquear, onDesbloquear), separacion)
            addView(boton(R.string.cortina_salir, onSalir).apply { setTextColor(Color.WHITE); setBackgroundColor(Color.TRANSPARENT) }, separacion)
        }
    }
}
