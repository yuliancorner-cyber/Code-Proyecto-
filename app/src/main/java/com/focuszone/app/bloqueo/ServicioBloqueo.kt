package com.focuszone.app.bloqueo

import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.focuszone.app.MainActivity
import com.focuszone.app.R
import com.focuszone.app.logica.ReglasSubventanas
import com.focuszone.app.logica.Subventana
import com.focuszone.app.logica.VistaApp
import com.focuszone.app.sesion.SesionActual
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Servicio de accesibilidad que bloquea las apps distractoras.
 *
 * Que es un servicio de accesibilidad: una pieza que Android creo para ayudar
 * a personas con discapacidad (lectores de pantalla, por ejemplo). Recibe
 * avisos de lo que pasa en pantalla. Por eso es tan potente y Android exige
 * activarlo a mano.
 *
 * Que usamos de el:
 *  - En todas las apps: SOLO el nombre del paquete de la app que se abrio.
 *  - En WhatsApp e Instagram, y solo si marcaste alguna de sus secciones:
 *    la estructura de la pantalla (nombres internos de los elementos y cual
 *    pestaña esta seleccionada) para saber si estas en Reels, Historias o
 *    Novedades. Nunca el texto de los mensajes ni lo que escribes.
 *
 * Reglas, cuando abres una app de tu lista o una seccion marcada:
 *  1. Hay una sesion de estudio en marcha -> bloqueada siempre (vuelves a la sesion).
 *  2. Tienes una ventana de desbloqueo abierta -> se permite, y se programa
 *     una revision para cuando la ventana se acabe.
 *  3. Si no -> bloqueada; FocusZone te ofrece gastar saldo para desbloquear.
 */
class ServicioBloqueo : AccessibilityService() {

    companion object {
        private const val TAG = "ServicioBloqueo"

        /** Palabras de los ids que interesan en el diagnostico (el resto es ruido). */
        private val PISTAS_DIAGNOSTICO = listOf(
            "reel", "clip", "status", "newsletter", "update", "tab", "story",
            "direct", "inbox", "thread", "message"
        )

        /**
         * Espera entre "pulsar inicio" y abrir FocusZone. Android ejecuta el
         * "inicio" con un pequeno retraso: si abrimos FocusZone de inmediato,
         * a veces el inicio llega DESPUES y la tapa (solo se veia el escritorio).
         */
        private const val RETRASO_ABRIR_FOCUSZONE_MS = 400L

        /** Espera entre cada "atras" al salir de una seccion (deja que la app reaccione). */
        private const val RETRASO_ATRAS_MS = 200L

        /**
         * WhatsApp e Instagram cambian la pantalla muchas veces por segundo (un
         * reel en marcha, por ejemplo): miramos como mucho una vez cada este rato.
         */
        private const val RETRASO_ESCANEO_MS = 400L

        /** Tras sacarte de una seccion, no volver a mirar en este rato (la app aun se esta cerrando). */
        private const val PAUSA_TRAS_BLOQUEO_MS = 2_500L

        /** Tope de elementos a revisar por pantalla, para no gastar bateria. */
        private const val MAX_NODOS = 1_500

        /**
         * Enlaces que abren directamente la bandeja de mensajes de Instagram.
         * Se prueban en orden: si Instagram cambia uno, queda el otro.
         */
        private val ENLACES_CHATS_INSTAGRAM = listOf(
            "instagram://direct-inbox",
            "https://www.instagram.com/direct/inbox/"
        )

        /**
         * Si en este rato hubo que llevarte a los chats mas de [MAX_ENVIOS_A_CHATS]
         * veces, algo falla (Instagram cambio y ya no reconocemos los chats, o el
         * enlace no lleva a ellos): en vez de dar vueltas, FocusZone te muestra su
         * pantalla de desbloqueo.
         */
        private const val VENTANA_ENVIOS_MS = 10_000L
        private const val MAX_ENVIOS_A_CHATS = 3

        private val _funcionando = MutableStateFlow(false)

        /**
         * true mientras Android tiene este servicio realmente en marcha.
         * Puede estar "activado" en Ajustes pero detenido (p. ej. si el sistema
         * lo mato para ahorrar bateria): asi la app lo detecta y te avisa.
         */
        val funcionando: StateFlow<Boolean> = _funcionando.asStateFlow()
    }

    private val manejador = Handler(Looper.getMainLooper())
    private var ultimoAvisoMs = 0L

    /** Ultima app "de verdad" en pantalla (sin contar teclado ni barra del sistema). */
    private var paqueteActual: String? = null

    /** Ultima pantalla interna (Activity) de esa app, p. ej. "...StatusPlaybackActivity". */
    private var claseActual: String? = null

    private var escaneoPendiente = false
    private var ultimoBloqueoSeccionMs = 0L
    private var enviosAChats = 0
    private var primerEnvioMs = 0L

    /** Al acabarse la ventana de desbloqueo: si sigues en una app bloqueada, te saca. */
    private val revisarAlExpirar = Runnable {
        val paquete = paqueteActual ?: return@Runnable
        sinCaerse { revisar(paquete) }
    }

    private val escaneo = Runnable {
        escaneoPendiente = false
        sinCaerse { escanearSecciones() }
    }

    private val irAInicio = Runnable {
        sinCaerse {
            performGlobalAction(GLOBAL_ACTION_HOME)
            manejador.postDelayed(abrirFocusZone, RETRASO_ABRIR_FOCUSZONE_MS)
        }
    }

    private val abrirFocusZone = Runnable {
        sinCaerse {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _funcionando.value = true
        // Si el guardian no estaba corriendo (p. ej. tras un reinicio), lo arranca.
        ServicioGuardian.iniciar(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val paquete = event.packageName?.toString() ?: return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (esVentanaDelSistema(paquete)) return
                if (paquete != paqueteActual) claseActual = null
                paqueteActual = paquete
                // Los menus y dialogos tambien llegan aqui con clases como
                // "android.widget.FrameLayout": esos no son una pantalla nueva.
                val clase = event.className?.toString()
                if (clase != null && !clase.startsWith("android.")) claseActual = clase
                sinCaerse { revisar(paquete) }
            }
            // Algo cambio dentro de la pantalla (otra pestaña, se abrio un reel...).
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (paquete == paqueteActual && ReglasSubventanas.vigila(paquete)) programarEscaneo()
            }
        }
    }

    /**
     * Un error aqui cerraria la app entera, y Android desactivaria el servicio
     * hasta que lo vuelvas a encender a mano. Mejor anotarlo y seguir vivos.
     */
    private inline fun sinCaerse(accion: () -> Unit) {
        try {
            accion()
        } catch (e: Exception) {
            Log.e(TAG, "Error en el servicio de bloqueo", e)
        }
    }

    private fun revisar(paquete: String) {
        if (AppsInstaladas.protegida(paquete)) return
        if (AppsBloqueadas.contiene(this, paquete)) {
            aplicarReglas(paquete, seccion = null)
        } else if (ReglasSubventanas.vigila(paquete)) {
            // La app no esta bloqueada entera: hay que mirar en que seccion estas.
            programarEscaneo()
        }
    }

    /** Las mismas reglas para apps enteras y para secciones ([seccion] != null). */
    private fun aplicarReglas(paquete: String, seccion: Subventana?) {
        when {
            SesionActual.bloqueoActivo -> bloquearPorSesion(paquete, seccion)
            Desbloqueo.activo(this) -> programarRevision()
            else -> bloquearPorSaldo(paquete, seccion)
        }
    }

    private fun bloquearPorSesion(paquete: String, seccion: Subventana?) {
        val texto = if (seccion == null) {
            getString(R.string.bloqueo_aviso, nombreDe(paquete))
        } else {
            getString(R.string.bloqueo_aviso_seccion, getString(SeccionesBloqueadas.nombre(seccion)))
        }
        avisar(texto)
        salirYMostrarFocusZone(seccion)
    }

    private fun bloquearPorSaldo(paquete: String, seccion: Subventana?) {
        // FocusZone lee esta solicitud y muestra "¿Desbloquear X min?".
        SolicitudDesbloqueo.pedir(paquete, seccion)
        salirYMostrarFocusZone(seccion)
    }

    /**
     * 1. Si es una seccion: "atras" dos veces, para que al volver a abrir la app
     *    aparezca en Chats o en el Inicio y no otra vez en Reels o Novedades.
     * 2. "Pulsar inicio": lo que de verdad te saca de la app (siempre funciona).
     * 3. Un momento despues, abrir FocusZone encima. En Xiaomi esto requiere el
     *    permiso "Mostrar ventanas emergentes en segundo plano"; si falta, al
     *    menos quedas en el escritorio, fuera de la app bloqueada.
     */
    private fun salirYMostrarFocusZone(seccion: Subventana? = null) {
        manejador.removeCallbacks(irAInicio)
        manejador.removeCallbacks(abrirFocusZone)
        if (seccion == null) {
            irAInicio.run()
            return
        }
        ultimoBloqueoSeccionMs = SystemClock.elapsedRealtime()
        performGlobalAction(GLOBAL_ACTION_BACK)
        manejador.postDelayed({ sinCaerse { performGlobalAction(GLOBAL_ACTION_BACK) } }, RETRASO_ATRAS_MS)
        manejador.postDelayed(irAInicio, RETRASO_ATRAS_MS * 2)
    }

    // ---------------------------------------------------------------------
    // Secciones de WhatsApp e Instagram
    // ---------------------------------------------------------------------

    private fun programarEscaneo() {
        if (escaneoPendiente) return
        escaneoPendiente = true
        manejador.postDelayed(escaneo, RETRASO_ESCANEO_MS)
    }

    private fun escanearSecciones() {
        val paquete = paqueteActual ?: return
        if (!ReglasSubventanas.vigila(paquete)) return
        // Si la app entera esta bloqueada, ya se encarga revisar().
        if (AppsBloqueadas.contiene(this, paquete)) return
        val activas = SeccionesBloqueadas.actuales(this).intersect(ReglasSubventanas.de(paquete))
        if (activas.isEmpty()) return
        if (SystemClock.elapsedRealtime() - ultimoBloqueoSeccionMs < PAUSA_TRAS_BLOQUEO_MS) return

        val raiz = rootInActiveWindow ?: return
        if (raiz.packageName?.toString() != paquete) {
            reciclar(raiz)
            return
        }
        val vista = leerVista(raiz, paquete)
        val seccion = ReglasSubventanas.detectar(vista, activas)
        Diagnostico.anotar(describir(vista, seccion))
        when {
            seccion == null -> Unit
            // Con "solo chats", cualquier cosa de Instagram (tambien un reel o
            // una historia) te devuelve a los chats en vez de sacarte de la app.
            Subventana.INSTAGRAM_SOLO_CHATS in activas -> soloChats(paquete)
            else -> aplicarReglas(paquete, seccion)
        }
    }

    /**
     * Instagram "solo chats": si tienes desbloqueo activo, todo libre; si no,
     * te lleva a la bandeja de mensajes (tambien durante una sesion: los chats
     * siguen permitidos).
     */
    private fun soloChats(paquete: String) {
        if (Desbloqueo.activo(this)) {
            programarRevision()
            return
        }
        val ahora = SystemClock.elapsedRealtime()
        if (ahora - primerEnvioMs > VENTANA_ENVIOS_MS) {
            primerEnvioMs = ahora
            enviosAChats = 0
        }
        enviosAChats++
        ultimoBloqueoSeccionMs = ahora
        if (enviosAChats > MAX_ENVIOS_A_CHATS || !abrirChatsDeInstagram(paquete)) {
            // Plan B: lo mismo que las demas secciones (te saca y FocusZone ofrece desbloquear).
            aplicarReglas(paquete, Subventana.INSTAGRAM_SOLO_CHATS)
            return
        }
        avisar(getString(R.string.solo_chats_aviso))
    }

    /** Abre la bandeja de mensajes de Instagram. false si ningun enlace funciono. */
    private fun abrirChatsDeInstagram(paquete: String): Boolean {
        for (enlace in ENLACES_CHATS_INSTAGRAM) {
            try {
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(enlace))
                        .setPackage(paquete)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return true
            } catch (e: ActivityNotFoundException) {
                Log.w(TAG, "Instagram no abre $enlace", e)
            }
        }
        return false
    }

    /**
     * Recorre los elementos de la pantalla y anota solo su nombre interno y,
     * si estan seleccionados (la pestaña activa), su etiqueta corta.
     */
    private fun leerVista(raiz: AccessibilityNodeInfo, paquete: String): VistaApp {
        val ids = HashSet<String>()
        val seleccionados = HashSet<String>()
        val pendientes = ArrayDeque<AccessibilityNodeInfo>()
        pendientes.addLast(raiz)
        var revisados = 0
        while (pendientes.isNotEmpty()) {
            val nodo = pendientes.removeFirst()
            if (revisados < MAX_NODOS) {
                revisados++
                if (nodo.isVisibleToUser) {
                    val id = nodo.viewIdResourceName?.let(ReglasSubventanas::idCorto)
                    if (id != null) ids += id
                    if (nodo.isSelected) {
                        if (id != null) seleccionados += id
                        etiqueta(nodo)?.let { seleccionados += it }
                    }
                }
                for (i in 0 until nodo.childCount) {
                    nodo.getChild(i)?.let { pendientes.addLast(it) }
                }
            }
            reciclar(nodo)
        }
        return VistaApp(paquete, claseActual, ids, seleccionados)
    }

    private fun etiqueta(nodo: AccessibilityNodeInfo): String? =
        (nodo.contentDescription ?: nodo.text)?.toString()?.trim()?.take(40)?.takeIf { it.isNotEmpty() }

    /** Antes de Android 13 hay que devolver cada elemento leido, o se acumulan en memoria. */
    @Suppress("DEPRECATION")
    private fun reciclar(nodo: AccessibilityNodeInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) nodo.recycle()
    }

    /** Una linea para el diagnostico: lo minimo para poder ajustar ReglasSubventanas. */
    private fun describir(vista: VistaApp, seccion: Subventana?): String {
        val app = if (vista.paquete == ReglasSubventanas.PAQUETE_INSTAGRAM) "Instagram" else "WhatsApp"
        val pantalla = vista.clase?.substringAfterLast('.') ?: "?"
        val pistas = vista.ids
            .filter { id -> PISTAS_DIAGNOSTICO.any { it in id.lowercase() } }
            .sorted()
            .take(8)
        val resultado = seccion?.let { "BLOQUEA ${it.name}" } ?: "libre"
        return "$app · $pantalla · sel: ${vista.seleccionados.sorted().joinToString()} · " +
            "ids: ${pistas.joinToString()} · $resultado"
    }

    private fun programarRevision() {
        manejador.removeCallbacks(revisarAlExpirar)
        val espera = (Desbloqueo.hasta(this) - System.currentTimeMillis()).coerceAtLeast(0L)
        // +500 ms de margen para revisar cuando la ventana ya este cerrada seguro.
        manejador.postDelayed(revisarAlExpirar, espera + 500L)
    }

    /** Aviso breve, sin repetirlo si la app lanza varios eventos seguidos. */
    private fun avisar(texto: String) {
        val ahora = SystemClock.elapsedRealtime()
        if (ahora - ultimoAvisoMs > 2_000L) {
            Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()
            ultimoAvisoMs = ahora
        }
    }

    /**
     * La barra de notificaciones y el teclado tambien generan avisos de
     * "cambio de ventana", pero no son la app que estas usando: los ignoramos
     * para no perder de vista en que app estas.
     */
    private fun esVentanaDelSistema(paquete: String): Boolean {
        if (paquete == "com.android.systemui") return true
        val teclado = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')
        return paquete == teclado
    }

    private fun nombreDe(paquete: String): String = try {
        @Suppress("DEPRECATION")
        val info = packageManager.getApplicationInfo(paquete, 0)
        packageManager.getApplicationLabel(info).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        paquete
    }

    // Obligatorio: Android lo llama si tiene que interrumpir el servicio. No hay nada que parar.
    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        _funcionando.value = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        _funcionando.value = false
        manejador.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
