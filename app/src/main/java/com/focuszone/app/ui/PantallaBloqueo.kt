package com.focuszone.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focuszone.app.R
import com.focuszone.app.bloqueo.AppInstalada
import com.focuszone.app.bloqueo.AppsBloqueadas
import com.focuszone.app.bloqueo.AppsInstaladas
import com.focuszone.app.bloqueo.EstadoPermisos
import com.focuszone.app.bloqueo.PermisosBloqueo
import com.focuszone.app.bloqueo.ServicioBloqueo

/**
 * Estado de los permisos especiales, que se vuelve a leer cada vez que la
 * pantalla reaparece (ON_RESUME). Asi, al volver de Ajustes tras activar un
 * permiso, la pantalla lo refleja sin hacer nada.
 */
@Composable
fun recordarEstadoPermisos(): EstadoPermisos {
    val context = LocalContext.current
    var estado by remember { mutableStateOf(PermisosBloqueo.leer(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        estado = PermisosBloqueo.leer(context)
    }
    // Android conecta el servicio un instante despues de abrir la app: lo
    // escuchamos para no mostrar un aviso de "detenido" que no es real.
    val funcionando by ServicioBloqueo.funcionando.collectAsStateWithLifecycle()
    return estado.copy(accesibilidadFuncionando = estado.accesibilidad && funcionando)
}

/** Configuracion del bloqueo: permisos especiales + eleccion de apps distractoras. */
@Composable
fun PantallaBloqueo(onVolver: () -> Unit) {
    // Boton "atras" del celular = volver a la pantalla de inicio de FocusZone.
    BackHandler(onBack = onVolver)

    val context = LocalContext.current
    val colores = MaterialTheme.colorScheme
    val permisos = recordarEstadoPermisos()
    val seleccionadas by remember { AppsBloqueadas.observar(context) }.collectAsStateWithLifecycle()

    // Lista de apps: se carga en segundo plano; mientras tanto vale null.
    val apps by produceState<List<AppInstalada>?>(initialValue = null) {
        value = AppsInstaladas.cargar(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colores.background)
            .systemBarsPadding()
    ) {
        // Barra superior
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onVolver) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.volver),
                    tint = colores.onBackground
                )
            }
            Text(
                text = stringResource(R.string.bloqueo_titulo),
                style = MaterialTheme.typography.titleLarge,
                color = colores.onBackground
            )
        }

        // LazyColumn = lista que solo dibuja lo visible (importante con 100+ apps).
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { TituloSeccion(stringResource(R.string.bloqueo_permisos)) }
            item {
                TarjetaPermiso(
                    titulo = stringResource(R.string.permiso_accesibilidad_titulo),
                    descripcion = stringResource(R.string.permiso_accesibilidad_desc),
                    concedido = permisos.accesibilidadFuncionando,
                    aviso = if (permisos.accesibilidadDetenida) {
                        stringResource(R.string.permiso_accesibilidad_detenido)
                    } else {
                        null
                    },
                    onActivar = { PermisosBloqueo.abrirAjustesAccesibilidad(context) }
                )
            }
            if (!permisos.accesibilidadFuncionando) {
                item {
                    Text(
                        text = stringResource(R.string.ayuda_accesibilidad),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colores.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
            item {
                TarjetaPermiso(
                    titulo = stringResource(R.string.permiso_nomolestar_titulo),
                    descripcion = stringResource(R.string.permiso_nomolestar_desc),
                    concedido = permisos.noMolestar,
                    onActivar = { PermisosBloqueo.abrirAjustesNoMolestar(context) }
                )
            }
            item {
                // Android no deja comprobar este permiso de Xiaomi: siempre mostramos el boton.
                TarjetaPermiso(
                    titulo = stringResource(R.string.permiso_ventanas_titulo),
                    descripcion = stringResource(R.string.permiso_ventanas_desc),
                    concedido = false,
                    textoBoton = stringResource(R.string.permiso_abrir_ajustes_app),
                    onActivar = { PermisosBloqueo.abrirAjustesDeLaApp(context) }
                )
            }

            item {
                Column {
                    Spacer(Modifier.height(8.dp))
                    TituloSeccion(stringResource(R.string.bloqueo_apps_titulo, seleccionadas.size))
                    Text(
                        text = stringResource(R.string.bloqueo_apps_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colores.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            val lista = apps
            if (lista == null) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                items(lista, key = { it.paquete }) { app ->
                    FilaApp(
                        app = app,
                        marcada = app.paquete in seleccionadas,
                        onAlternar = { AppsBloqueadas.alternar(context, app.paquete) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TituloSeccion(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun TarjetaPermiso(
    titulo: String,
    descripcion: String,
    concedido: Boolean,
    onActivar: () -> Unit,
    aviso: String? = null,
    textoBoton: String? = null
) {
    val colores = MaterialTheme.colorScheme
    Surface(
        color = colores.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge, color = colores.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(
                descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = colores.onSurface.copy(alpha = 0.7f)
            )
            if (aviso != null) {
                Spacer(Modifier.height(8.dp))
                Text(aviso, style = MaterialTheme.typography.bodyMedium, color = colores.error)
            }
            Spacer(Modifier.height(12.dp))
            if (concedido) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = colores.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.permiso_activado),
                        color = colores.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                Button(onClick = onActivar, shape = RoundedCornerShape(12.dp)) {
                    Text(textoBoton ?: stringResource(R.string.permiso_activar))
                }
            }
        }
    }
}

@Composable
private fun FilaApp(app: AppInstalada, marcada: Boolean, onAlternar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAlternar)
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Image(bitmap = app.icono, contentDescription = null, modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(app.nombre, style = MaterialTheme.typography.bodyLarge, color = colores.onBackground)
            if (app.sugerida) {
                Text(
                    stringResource(R.string.app_sugerida),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colores.tertiary
                )
            }
        }
        Checkbox(checked = marcada, onCheckedChange = { onAlternar() })
    }
}
