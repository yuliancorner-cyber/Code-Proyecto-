package com.focuszone.app.logica

import com.focuszone.app.logica.MotorSoloChats.Accion
import com.focuszone.app.logica.MotorSoloChats.Redireccion
import org.junit.Assert.assertEquals
import org.junit.Test

class MotorSoloChatsTest {

    private val motor = MotorSoloChats()

    private fun fuera(ahora: Long, visor: Boolean = false, boton: Boolean = true) =
        motor.evaluar(enChats = false, enVisor = visor, hayBotonMensajes = boton, ahoraMs = ahora)

    @Test
    fun enChatsQuitaLaCortina() {
        assertEquals(Accion.QuitarCortina, motor.evaluar(true, false, false, 0L))
    }

    @Test
    fun esperaUnaConfirmacionAntesDeTapar() {
        assertEquals(Accion.Nada, fuera(0L))
        assertEquals(Accion.Cubrir(Redireccion.BOTON_MENSAJES), fuera(300L))
    }

    @Test
    fun noRepiteIntentosMuySeguidos() {
        fuera(0L)
        fuera(300L)
        assertEquals(Accion.Cubrir(null), fuera(600L))
        assertEquals(Accion.Cubrir(null), fuera(1_700L))
        // 1,5 s despues del primer intento: otro intento, con otra forma.
        assertEquals(Accion.Cubrir(Redireccion.ENLACE), fuera(1_800L))
        assertEquals(Accion.Cubrir(Redireccion.ATRAS), fuera(3_300L))
        assertEquals(Accion.Cubrir(Redireccion.BOTON_MENSAJES), fuera(4_800L))
    }

    @Test
    fun enUnReelPrimeroPulsaAtras() {
        fuera(0L, visor = true)
        assertEquals(Accion.Cubrir(Redireccion.ATRAS), fuera(300L, visor = true))
    }

    @Test
    fun sinBotonDeMensajesUsaElEnlace() {
        fuera(0L, boton = false)
        assertEquals(Accion.Cubrir(Redireccion.ENLACE), fuera(300L, boton = false))
    }

    @Test
    fun volverALosChatsReiniciaTodo() {
        fuera(0L)
        fuera(300L)
        motor.evaluar(true, false, true, 600L)
        // De nuevo fuera: vuelve a esperar confirmacion y empieza por el boton.
        assertEquals(Accion.Nada, fuera(900L))
        assertEquals(Accion.Cubrir(Redireccion.BOTON_MENSAJES), fuera(1_200L))
    }
}
