# FocusZone

App Android de **enfoque para estudiar**, de uso personal (no se publica en Play Store).

La idea: dejas el celular boca abajo dentro de una "zona de enfoque" marcada sobre tu
escritorio, arranca un cronómetro, y si lo levantas antes de tiempo la app te avisa y
pierdes la recompensa. Al completar la sesión ganas créditos de "tiempo de pantalla".

- **Package / identificador:** `com.focuszone.app`
- **Android mínimo:** 8.0 (API 26)
- **Stack:** Kotlin + Jetpack Compose + Material 3

---

## Estado actual: Fase 3a (bloqueo durante la sesión) — lista para probar

Se puede usar a diario: eliges una meta, dejas el celular boca abajo, corre el
cronómetro, y si lo levantas antes de tiempo te avisa y pierdes la recompensa.
Durante la sesión el celular entra en No molestar y las apps que marques como
distractoras no se pueden abrir.

### Hoja de ruta

| Fase | Qué incluye | Estado |
|---|---|---|
| 0 | Proyecto Kotlin + Compose que compila e instala | ✅ Hecho |
| 1 | MVP: zona simple sobre la cámara, detección boca-abajo por sensores, cronómetro, pantalla de bloqueo, créditos en Room | ✅ Hecho |
| 2 | Zona AR real con ARCore (colocar, arrastrar, redimensionar, rotar) — se hará después de la 3 | Pendiente |
| 3a | No molestar automático + bloqueo de apps distractoras **durante la sesión** | 🧪 Por probar |
| 3b | Apps distractoras bloqueadas **siempre**; se desbloquean gastando los minutos ganados | Pendiente |
| 4 | Historial de sesiones y estadísticas de créditos | Pendiente |
| 5 | Ícono, splash, modo oscuro, APK final | Pendiente |

### Reglas de una sesión

| Regla | Valor |
|---|---|
| Metas disponibles | 15, 25, 45 o 60 min |
| Recompensa | 1 min de pantalla por cada 5 de estudio (25 min → +5 min) |
| Para arrancar | Boca abajo y quieto durante 2 s |
| Para dar la alerta | Fuera de posición durante 0,7 s (un golpe a la mesa no cuenta) |
| Tolerancia | 5 s para volver a dejarlo; si no, se cancela sin créditos |
| Durante la alerta | El cronómetro se pausa |

**Limitación honesta:** con el celular boca abajo la cámara no ve nada (la trasera
apunta al techo), así que la zona sirve para *colocarlo*, y lo que vigila la sesión
son los sensores. En la Fase 1 la zona es una guía fija en pantalla; en la Fase 2
quedará anclada al escritorio con ARCore.

---

## Cómo actualizar tu copia (cada vez que haya una fase nueva)

En Android Studio: menú **Git › Pull…** (o el botón ↙ azul de arriba) y acepta.
Descarga los cambios de GitHub. Luego **Run ▶**.

## Cómo probar la Fase 3a

### 1. Activar los permisos (una sola vez)

En la app, toca **⚙** (arriba a la derecha del panel superior). Verás dos tarjetas:

**Servicio de accesibilidad** → **Activar**. Se abre Ajustes › Accesibilidad:
1. Busca **Apps descargadas** / **Servicios instalados** › **FocusZone: bloqueo de apps**.
2. Actívalo. Android mostrará una advertencia seria ("puede ver la pantalla…"):
   es el texto genérico de cualquier servicio de accesibilidad. FocusZone solo
   recibe el nombre de la app que se abre (`canRetrieveWindowContent="false"`).
3. Si sale gris o dice **"Ajuste restringido"**: Ajustes › Aplicaciones › FocusZone ›
   **⋮** (arriba a la derecha) › **Permitir ajustes restringidos**, y repite.

**Acceso a No molestar** → **Activar** → busca FocusZone en la lista y permítelo.

Al volver a FocusZone las dos tarjetas deben decir **✓ Activado**.

> **Xiaomi:** si el servicio de accesibilidad se desactiva solo al rato, es el
> ahorro de batería. Revisa que FocusZone siga en **Sin restricciones** y con
> **Inicio automático** (ver más abajo).

### 2. Elegir las apps distractoras

En la misma pantalla, marca las apps que te distraen. Las **Sugeridas** salen
arriba. Ajustes y el teléfono no aparecen: nunca se bloquean.

Al volver al inicio, el panel superior dice *"Bloqueando N apps durante la sesión"*.

### 3. Recorrido de prueba

1. Inicia una sesión de 15 min. Baja la barra de notificaciones: debe aparecer
   el icono de **No molestar**.
2. Levanta el celular (salta la alerta) y, antes del 0, abre una app bloqueada
   desde el menú de apps: te devuelve al inicio al instante, aparece el aviso
   *"X está bloqueada…"* y FocusZone vuelve al frente con la cuenta atrás.
3. Abre una app **no** bloqueada: se abre normal (aunque la alerta sigue corriendo).
4. Al terminar (o cancelar) la sesión, **No molestar se desactiva solo** y las
   apps vuelven a abrir con normalidad.
5. Si ya tenías No molestar puesto antes de empezar, FocusZone no lo toca.

### Ajustes del Xiaomi (si no los hiciste en la Fase 1)

**Ajustes › Aplicaciones › Administrar aplicaciones › FocusZone:**
**Ahorro de batería › Sin restricciones** e **Inicio automático** activado.
Sin esto, HyperOS puede cortar la sesión o apagar el servicio de bloqueo.

### Pruebas automáticas

La lógica de sesión tiene 16 pruebas que corren en el PC sin celular. En Android
Studio: panel izquierdo › `app/java/com.focuszone.app (test)/logica` › clic
derecho sobre la carpeta › **Run 'Tests in logica'**. Deben salir 16 en verde.

### Si la detección es demasiado sensible (o poco)

Los umbrales están al principio de dos archivos, con comentarios:

- `logica/DetectorPosicion.kt` → `umbralMovimiento` (1.5 m/s²). Súbelo si te da
  alertas falsas al escribir sobre la mesa.
- `logica/MotorSesion.kt` → `Reglas` (tiempos de arranque, confirmación y tolerancia).

---

## Mapa del proyecto

```
settings.gradle.kts          Qué módulos hay y de dónde bajar las librerías
build.gradle.kts             Compilación raíz (solo declara plugins)
gradle/libs.versions.toml    ← TODAS las versiones de librerías viven aquí

app/src/main/java/com/focuszone/app/
  MainActivity.kt            Punto de entrada
  logica/                    Reglas puras, sin Android (probadas con tests)
    Recompensa.kt              Metas y fórmula de créditos
    EstadoSesion.kt            Los 6 estados posibles de una sesión
    DetectorPosicion.kt        Acelerómetro → "boca abajo y quieto"
    MotorSesion.kt             Máquina de estados: arranque, alerta, cancelación, meta
  sesion/
    ServicioSesion.kt          Servicio en primer plano: sensores, cronómetro, vibración, guardar
    SesionActual.kt            Puente servicio ↔ pantallas
  bloqueo/                   Bloqueo de apps y No molestar (Fase 3)
    ServicioBloqueo.kt         Servicio de accesibilidad: detecta y bloquea apps
    AppsBloqueadas.kt          Tu lista de apps distractoras (guardada en el celular)
    AppsInstaladas.kt          Lee las apps del menú; sugeridas y nunca-bloqueables
    ModoSilencio.kt            Activa/restaura No molestar sin pisar tu configuración
    PermisosBloqueo.kt         Comprueba y abre los ajustes de los permisos especiales
  datos/                     Base de datos Room
    SesionRegistro.kt          Tabla "sesiones"
    SesionDao.kt               Consultas (insertar, total de créditos)
    BaseDatos.kt               El archivo focuszone.db
  ui/
    AppFocusZone.kt            Elige qué pantalla mostrar según el estado
    PantallaInicio.kt          Cámara + zona + elegir meta
    PantallaSesion.kt          Candado, anillo y cronómetro
    PantallaAlerta.kt          "No se permite el celular" + cuenta atrás
    PantallaResultado.kt       Completada / cancelada
    PantallaBloqueo.kt         Permisos + elegir apps distractoras
    componentes/               Cámara, zona, anillo, utilidades
    theme/                     Colores, tema claro/oscuro, tipografía

app/src/test/.../logica/     Pruebas automáticas de la lógica
app/src/main/res/            Textos (strings.xml), colores, íconos
```

### Vocabulario rápido

- **Gradle**: el sistema que compila la app. Los archivos `build.gradle.kts` son sus recetas.
- **SDK**: el conjunto de herramientas y librerías de Android. Lo instala Android Studio.
- **minSdk / targetSdk / compileSdk**: versión mínima donde se instala / versión para la
  que está afinada / versión contra la que se compila.
- **Compose**: forma moderna de dibujar la interfaz escribiendo funciones Kotlin
  (marcadas con `@Composable`) en vez de archivos XML de diseño.
- **Manifest**: el "documento de identidad" de la app — nombre, ícono, permisos, pantallas.
- **APK**: el archivo instalable de la app.
- **Servicio en primer plano**: código que sigue corriendo con la pantalla apagada,
  a cambio de mostrar una notificación fija.
- **Room**: la base de datos local; guarda las sesiones en el propio celular.
- **Máquina de estados**: lógica que siempre está en *un* estado (esperando, en curso,
  alerta…) y solo cambia por reglas concretas.
- **Acelerómetro**: sensor que mide la gravedad y los movimientos en 3 ejes.

---

## Permisos declarados y para qué

Están declarados en `AndroidManifest.xml` desde ya, aunque no se usen todavía.
Declarar un permiso **no** lo concede.

| Permiso | Para qué | Cómo se concede |
|---|---|---|
| `CAMERA` | Ver el escritorio y colocar la zona de enfoque | Diálogo dentro de la app (Fase 1) |
| `POST_NOTIFICATIONS` | Notificación de "sesión activa" | Diálogo dentro de la app (Fase 1) |
| `FOREGROUND_SERVICE` (+`SPECIAL_USE`) | Mantener el cronómetro con la pantalla apagada | Automático al instalar |
| `WAKE_LOCK` | Que el sistema no duerma los sensores | Automático al instalar |
| `VIBRATE` | Avisar si levantas el celular antes de tiempo | Automático al instalar |
| `ACCESS_NOTIFICATION_POLICY` | Activar/quitar No molestar durante la sesión | **A mano**: Ajustes › Acceso a No molestar (botón en ⚙) |
| Servicio de accesibilidad | Detectar cuándo abres una app distractora | **A mano**: Ajustes › Accesibilidad (botón en ⚙) |

Los sensores de movimiento (acelerómetro y giroscopio) **no necesitan permiso**: cualquier
app puede leerlos.

Los dos últimos son "especiales": Android no deja pedirlos con un diálogo, así que
la app te lleva a la pantalla de Ajustes correcta y tú los activas.
