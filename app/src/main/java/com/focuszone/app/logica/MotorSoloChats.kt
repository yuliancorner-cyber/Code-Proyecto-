package com.focuszone.app.logica

/**
 * Decide que hacer en Instagram con "solo chats", cada vez que el servicio
 * mira la pantalla (unas 3 veces por segundo mientras estas en Instagram).
 *
 * Idea: en cuanto sales de los chats, una "cortina" de FocusZone tapa
 * Instagram (asi no ves nada aunque llevarte a los chats tarde) y, detras de
 * ella, se intenta volver a los chats de varias formas, una cada poco.
 *
 * Sin nada de Android: probado en MotorSoloChatsTest.
 */
class MotorSoloChats {

    companion object {
        /**
         * Miradas seguidas fuera de los chats antes de tapar. Evita que la
         * cortina parpadee en el instante en que una pantalla de chat carga.
         */
        const val CONFIRMACIONES = 2

        /** Tiempo minimo entre dos intentos de volver a los chats (dale tiempo a Instagram). */
        const val ESPERA_ENTRE_INTENTOS_MS = 1_500L
    }

    /** Formas de volver a los chats. */
    enum class Redireccion {
        /** Tocar el boton de mensajes de Instagram (lo mas rapido y fiable). */
        BOTON_MENSAJES,

        /** Abrir el enlace de la bandeja de mensajes. */
        ENLACE,

        /** "Atras": cierra un reel o historia abiertos desde un chat. */
        ATRAS
    }

    sealed interface Accion {
        /** Aun no hay que hacer nada (o ya se hizo y se espera a Instagram). */
        data object Nada : Accion

        /** Estas en los chats: quitar la cortina si estaba. */
        data object QuitarCortina : Accion

        /** Tapar Instagram; si [redireccion] != null, intentar volver a los chats asi. */
        data class Cubrir(val redireccion: Redireccion?) : Accion
    }

    private var miradasFuera = 0
    private var intentos = 0
    private var ultimoIntentoMs = Long.MIN_VALUE / 2

    /**
     * @param enChats la pantalla es la bandeja o un chat.
     * @param enVisor es un reel o una historia a pantalla completa.
     * @param hayBotonMensajes se ve el boton que abre la bandeja.
     */
    fun evaluar(enChats: Boolean, enVisor: Boolean, hayBotonMensajes: Boolean, ahoraMs: Long): Accion {
        if (enChats) {
            reiniciar()
            return Accion.QuitarCortina
        }
        miradasFuera++
        if (miradasFuera < CONFIRMACIONES) return Accion.Nada
        if (ahoraMs - ultimoIntentoMs < ESPERA_ENTRE_INTENTOS_MS) return Accion.Cubrir(null)

        ultimoIntentoMs = ahoraMs
        val primera = when {
            enVisor -> Redireccion.ATRAS
            hayBotonMensajes -> Redireccion.BOTON_MENSAJES
            else -> Redireccion.ENLACE
        }
        // Si la primera forma no funciono, se van alternando las demas.
        val orden = listOf(primera) + (Redireccion.entries - primera)
        val redireccion = orden[intentos % orden.size]
        intentos++
        return Accion.Cubrir(redireccion)
    }

    /** Al salir de Instagram o volver a los chats: todo de cero. */
    fun reiniciar() {
        miradasFuera = 0
        intentos = 0
        ultimoIntentoMs = Long.MIN_VALUE / 2
    }
}
