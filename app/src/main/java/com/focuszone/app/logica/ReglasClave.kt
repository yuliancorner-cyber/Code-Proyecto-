package com.focuszone.app.logica

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Reglas de la clave (PIN) que protege la configuracion del bloqueo.
 *
 * La clave NUNCA se guarda tal cual. Se guarda un "hash": el resultado de pasar
 * la clave por una funcion matematica de un solo sentido (PBKDF2), mezclada con
 * una "sal" aleatoria. Para comprobar una clave se repite el calculo y se
 * comparan los resultados; a partir del hash no se puede sacar el numero.
 *
 * Sin nada de Android: probado en ReglasClaveTest.
 */
object ReglasClave {

    const val LONGITUD_MIN = 4
    const val LONGITUD_MAX = 8

    /** Fallos seguidos permitidos antes de obligar a esperar. */
    const val INTENTOS_ANTES_DE_ESPERAR = 5

    private const val ESPERA_BASE_MS = 30_000L
    private const val ESPERA_MAXIMA_MS = 5 * 60_000L

    // Cuantas veces se repite el calculo: lo hace lento a proposito para que
    // probar millones de claves sea inviable, pero rapido para una sola.
    private const val ITERACIONES = 60_000
    private const val BITS = 256
    private const val ALGORITMO = "PBKDF2WithHmacSHA256"

    /** De 4 a 8 digitos, solo numeros. */
    fun esValida(clave: String): Boolean =
        clave.length in LONGITUD_MIN..LONGITUD_MAX && clave.all { it in '0'..'9' }

    /** Convierte la clave en el texto que se guarda: "pbkdf2:iteraciones:sal:hash". */
    fun proteger(clave: String, sal: ByteArray = salNueva()): String {
        val hash = derivar(clave, sal, ITERACIONES)
        return "pbkdf2:$ITERACIONES:${base64(sal)}:${base64(hash)}"
    }

    /** true si [clave] es la que se guardo como [guardada]. */
    fun coincide(clave: String, guardada: String): Boolean = try {
        val partes = guardada.split(':')
        if (partes.size != 4 || partes[0] != "pbkdf2") {
            false
        } else {
            val iteraciones = partes[1].toInt()
            val sal = Base64.getDecoder().decode(partes[2])
            val esperado = Base64.getDecoder().decode(partes[3])
            // Comparacion en tiempo constante: no da pistas por lo que tarda.
            MessageDigest.isEqual(derivar(clave, sal, iteraciones), esperado)
        }
    } catch (e: IllegalArgumentException) {
        false // texto guardado danado
    }

    /**
     * Espera obligatoria (ms) tras el fallo numero [fallos]: cada 5 fallos
     * seguidos, 30 s, 60 s, 90 s... hasta un maximo de 5 minutos.
     */
    fun esperaTrasFallos(fallos: Int): Long {
        if (fallos < INTENTOS_ANTES_DE_ESPERAR || fallos % INTENTOS_ANTES_DE_ESPERAR != 0) return 0L
        val tanda = fallos / INTENTOS_ANTES_DE_ESPERAR
        return (ESPERA_BASE_MS * tanda).coerceAtMost(ESPERA_MAXIMA_MS)
    }

    /** Intentos que quedan antes de la siguiente espera. */
    fun intentosRestantes(fallos: Int): Int =
        INTENTOS_ANTES_DE_ESPERAR - (fallos % INTENTOS_ANTES_DE_ESPERAR)

    private fun derivar(clave: String, sal: ByteArray, iteraciones: Int): ByteArray {
        val spec = PBEKeySpec(clave.toCharArray(), sal, iteraciones, BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun salNueva(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    private fun base64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
}
