package com.focuszone.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuszone.app.R
import com.focuszone.app.bloqueo.ClaveAcceso
import com.focuszone.app.logica.ReglasClave
import com.focuszone.app.ui.theme.FocusZoneTheme
import kotlinx.coroutines.launch

/** Para que se usa la pantalla de clave. */
enum class ModoClave {
    /** Primera vez: crear la clave (escribirla y repetirla). */
    CREAR,

    /** Entrar en la configuracion: pedir la clave. */
    VERIFICAR,

    /** Cambiarla: pedir la actual, luego la nueva dos veces. */
    CAMBIAR
}

/** En que punto del proceso estamos. */
private enum class Paso { ACTUAL, NUEVA, CONFIRMAR }

/**
 * Pantalla de PIN con teclado numerico propio.
 *
 * @param onListo se llama cuando la clave es correcta (VERIFICAR) o cuando la
 *        clave nueva ya quedo guardada (CREAR / CAMBIAR).
 * @param onCancelar boton "Cancelar" o el boton atras del celular.
 */
@Composable
fun PantallaClave(modo: ModoClave, onListo: () -> Unit, onCancelar: () -> Unit) {
    BackHandler(onBack = onCancelar)

    val context = LocalContext.current
    val alcance = rememberCoroutineScope()
    val colores = MaterialTheme.colorScheme

    var paso by remember { mutableStateOf(if (modo == ModoClave.CREAR) Paso.NUEVA else Paso.ACTUAL) }
    var entrada by remember { mutableStateOf("") }
    var nueva by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var ocupado by remember { mutableStateOf(false) }

    val titulo = stringResource(
        when (paso) {
            Paso.ACTUAL -> if (modo == ModoClave.VERIFICAR) R.string.clave_titulo_verificar else R.string.clave_titulo_actual
            Paso.NUEVA -> if (modo == ModoClave.CREAR) R.string.clave_titulo_crear else R.string.clave_titulo_nueva
            Paso.CONFIRMAR -> R.string.clave_titulo_confirmar
        }
    )
    val explicacion = stringResource(
        when (paso) {
            Paso.ACTUAL -> if (modo == ModoClave.VERIFICAR) R.string.clave_sub_verificar else R.string.clave_sub_actual
            Paso.NUEVA -> R.string.clave_sub_nueva
            Paso.CONFIRMAR -> R.string.clave_sub_confirmar
        }
    )

    fun aceptar() {
        when (paso) {
            Paso.ACTUAL -> {
                ocupado = true
                alcance.launch {
                    when (val r = ClaveAcceso.verificar(context, entrada)) {
                        ClaveAcceso.Resultado.Correcta -> {
                            error = null
                            if (modo == ModoClave.VERIFICAR) {
                                onListo()
                            } else {
                                paso = Paso.NUEVA
                            }
                        }
                        is ClaveAcceso.Resultado.Incorrecta -> {
                            error = context.getString(R.string.clave_incorrecta, r.intentosRestantes)
                        }
                        is ClaveAcceso.Resultado.Esperar -> {
                            error = context.getString(R.string.clave_espera, r.segundos.toInt())
                        }
                    }
                    entrada = ""
                    ocupado = false
                }
            }

            Paso.NUEVA -> {
                nueva = entrada
                entrada = ""
                error = null
                paso = Paso.CONFIRMAR
            }

            Paso.CONFIRMAR -> {
                if (entrada == nueva) {
                    ocupado = true
                    alcance.launch {
                        ClaveAcceso.guardar(context, nueva)
                        onListo()
                    }
                } else {
                    error = context.getString(R.string.clave_no_coinciden)
                    entrada = ""
                    nueva = ""
                    paso = Paso.NUEVA
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colores.background)
            .systemBarsPadding()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = colores.primary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(16.dp))
        Text(titulo, style = MaterialTheme.typography.headlineMedium, color = colores.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            explicacion,
            style = MaterialTheme.typography.bodyMedium,
            color = colores.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))
        PuntosClave(cantidad = entrada.length)

        // Espacio fijo para el error: asi el teclado no "salta" al aparecer.
        Box(Modifier.height(56.dp).padding(top = 12.dp), contentAlignment = Alignment.TopCenter) {
            error?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = colores.error, textAlign = TextAlign.Center)
            }
        }

        TecladoNumerico(
            habilitado = !ocupado,
            puedeEscribir = entrada.length < ReglasClave.LONGITUD_MAX,
            puedeAceptar = ReglasClave.esValida(entrada),
            onDigito = { entrada += it },
            onBorrar = { entrada = entrada.dropLast(1) },
            onAceptar = { aceptar() }
        )

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) }

        if (modo != ModoClave.CREAR) {
            Text(
                stringResource(R.string.clave_olvidada),
                style = MaterialTheme.typography.bodyMedium,
                color = colores.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Puntos que muestran cuantos digitos llevas (sin revelar cuales). */
@Composable
private fun PuntosClave(cantidad: Int) {
    val colores = MaterialTheme.colorScheme
    val total = maxOf(ReglasClave.LONGITUD_MIN, cantidad)
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(total) { i ->
            val lleno = i < cantidad
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (lleno) colores.primary else colores.background)
                    .border(2.dp, if (lleno) colores.primary else colores.onBackground.copy(alpha = 0.4f), CircleShape)
            )
        }
    }
}

@Composable
private fun TecladoNumerico(
    habilitado: Boolean,
    puedeEscribir: Boolean,
    puedeAceptar: Boolean,
    onDigito: (String) -> Unit,
    onBorrar: () -> Unit,
    onAceptar: () -> Unit
) {
    val filas = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        filas.forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                fila.forEach { d -> TeclaTexto(d, habilitado && puedeEscribir) { onDigito(d) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            TeclaBorrar(habilitado, onBorrar)
            TeclaTexto("0", habilitado && puedeEscribir) { onDigito("0") }
            TeclaTexto(stringResource(R.string.clave_ok), habilitado && puedeAceptar, destacada = true, onClick = onAceptar)
        }
    }
}

@Composable
private fun TeclaTexto(texto: String, habilitada: Boolean, destacada: Boolean = false, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(if (destacada) colores.primary else colores.onBackground.copy(alpha = 0.08f))
            .clickable(enabled = habilitada, onClick = onClick)
            .alpha(if (habilitada) 1f else 0.35f)
    ) {
        Text(
            texto,
            fontSize = if (destacada) 18.sp else 26.sp,
            fontWeight = if (destacada) FontWeight.Bold else FontWeight.Normal,
            color = if (destacada) colores.onPrimary else colores.onBackground
        )
    }
}

@Composable
private fun TeclaBorrar(habilitada: Boolean, onClick: () -> Unit) {
    val descripcion = stringResource(R.string.clave_borrar)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(enabled = habilitada, onClick = onClick)
            .semantics { contentDescription = descripcion }
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Backspace,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Preview(showBackground = true, name = "Clave - crear")
@Composable
private fun VistaPreviaClave() {
    FocusZoneTheme(temaOscuro = true) {
        PantallaClave(modo = ModoClave.CREAR, onListo = {}, onCancelar = {})
    }
}
