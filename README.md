# FocusZone

App Android de **enfoque para estudiar**, de uso personal (no se publica en Play Store).

La idea: dejas el celular boca abajo dentro de una "zona de enfoque" marcada sobre tu
escritorio, arranca un cronómetro, y si lo levantas antes de tiempo la app te avisa y
pierdes la recompensa. Al completar la sesión ganas créditos de "tiempo de pantalla".

- **Package / identificador:** `com.focuszone.app`
- **Android mínimo:** 8.0 (API 26)
- **Stack:** Kotlin + Jetpack Compose + Material 3

---

## Estado actual: Fase 4 (estadísticas) — lista para probar

Se puede usar a diario: eliges una meta, dejas el celular boca abajo, corre el
cronómetro, y si lo levantas antes de tiempo te avisa y pierdes la recompensa.
Durante la sesión el celular entra en No molestar. Las apps que marques como
distractoras quedan bloqueadas **siempre**: fuera de las sesiones puedes abrirlas
un rato gastando el saldo que ganaste ese día.

### Hoja de ruta

| Fase | Qué incluye | Estado |
|---|---|---|
| 0 | Proyecto Kotlin + Compose que compila e instala | ✅ Hecho |
| 1 | MVP: zona simple sobre la cámara, detección boca-abajo por sensores, cronómetro, pantalla de bloqueo, créditos en Room | ✅ Hecho |
| 2 | Zona AR real con ARCore (colocar, arrastrar, redimensionar, rotar) — se hará después de la 3 | Pendiente |
| 3a | No molestar automático + bloqueo de apps distractoras **durante la sesión** | ✅ Hecho |
| 3b | Apps distractoras bloqueadas **siempre**; se desbloquean gastando los minutos ganados | ✅ Hecho |
| 4 | Historial de sesiones y estadísticas de créditos | 🧪 Por probar |
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

### Reglas del saldo

| Regla | Valor |
|---|---|
| Saldo de hoy | Minutos ganados hoy − minutos gastados hoy |
| Reinicio | A medianoche: lo no gastado se pierde |
| Cómo se gasta | Al abrir una app bloqueada eliges 5, 10 o 15 min (o lo que te quede), se descuentan al momento |
| Durante el desbloqueo | **Todas** tus apps distractoras quedan libres; al acabarse te saca de la app |
| Durante una sesión | Bloqueadas siempre, tengas saldo o no |
| Sin saldo | No hay desbloqueo de emergencia |

**Limitación honesta:** con el celular boca abajo la cámara no ve nada (la trasera
apunta al techo), así que la zona sirve para *colocarlo*, y lo que vigila la sesión
son los sensores. En la Fase 1 la zona es una guía fija en pantalla; en la Fase 2
quedará anclada al escritorio con ARCore.

---

## Cómo actualizar tu copia (cada vez que haya una fase nueva)

En Android Studio: menú **Git › Pull…** (o el botón ↙ azul de arriba) y acepta.
Descarga los cambios de GitHub. Luego **Run ▶**.

## Cómo probar la Fase 4

En el panel superior de inicio, toca **📊** (encima del ⚙). Verás:

| Sección | Qué muestra |
|---|---|
| **Racha** | Días seguidos con al menos una sesión completada (si hoy aún no estudiaste, cuenta hasta ayer) |
| **Éxito** | % de sesiones completadas frente a canceladas |
| **Tiempo total de enfoque** | Minutos con el celular boca abajo desde el principio (incluye lo estudiado en sesiones canceladas) |
| **Gráfica de la semana** | Minutos de enfoque de los últimos 7 días; toca una barra para ver su valor |
| **Saldo de pantalla** | Ganado y gastado hoy, y desde el principio |
| **Historial** | Últimas 30 sesiones y desbloqueos, con fecha y minutos |

Sin celular: abre `ui/PantallaEstadisticas.kt` y pulsa **Split** para ver la
pantalla con datos de ejemplo en tema claro y oscuro.

## Cómo probar la Fase 3b

Requiere los permisos de la Fase 3a (accesibilidad y No molestar, en ⚙) y al
menos una app marcada como distractora.

1. **Sin saldo:** si hoy no has completado ninguna sesión, abre una app bloqueada
   desde el menú de apps. Te devuelve al inicio y FocusZone muestra
   *"X está bloqueada · Hoy no te queda saldo"*.
2. **Ganar saldo:** completa una sesión de 15 min → *Saldo de hoy: 3 min* en el
   panel superior.
3. **Gastar:** abre la app bloqueada → elige **3 min** → se abre sola. En FocusZone
   el panel muestra *"🔓 Apps libres · quedan 02:59"* con el botón **Bloquear ya**.
4. **Que te saque:** quédate dentro de la app hasta que pasen los 3 min → te
   devuelve al inicio y FocusZone vuelve a preguntar.
5. **Bloquear ya:** desbloquea, vuelve a FocusZone y pulsa **Bloquear ya** → la
   app vuelve a estar bloqueada (los minutos no se devuelven).
6. **Durante una sesión** las apps siguen bloqueadas aunque tengas saldo.

> **Sobre la disciplina:** siempre puedes desmarcar apps en ⚙ o desactivar el
> servicio en Ajustes (Ajustes nunca se bloquea, por seguridad). La app pone
> fricción, no cerrojos.

### Activar los permisos (Fase 3a, una sola vez)

En la app, toca **⚙**:

- **Servicio de accesibilidad** → **Activar** → *Apps descargadas* /
  *Servicios instalados* › **FocusZone: bloqueo de apps** › actívalo. Si sale gris
  o *"Ajuste restringido"*: Ajustes › Aplicaciones › FocusZone › **⋮** ›
  **Permitir ajustes restringidos**, y repite.
- **Acceso a No molestar** → **Activar** → permite FocusZone.

- **Ventanas en segundo plano (Xiaomi)** → **Abrir ajustes de FocusZone** →
  **Otros permisos** › *Mostrar ventanas emergentes mientras se ejecuta en segundo
  plano* › **Permitir**. Sin esto, al abrir una app bloqueada solo vuelves al
  escritorio y FocusZone no aparece.

Luego marca tus apps distractoras en la lista de abajo.

### Si deja de bloquear

HyperOS a veces detiene el servicio de accesibilidad aunque en Ajustes siga
"activado". FocusZone lo detecta: el panel de inicio dice *"El bloqueo se
detuvo"* y en ⚙ la tarjeta de accesibilidad lo explica en rojo.

Arreglo: Ajustes › Accesibilidad › FocusZone › **desactívalo y vuelve a
activarlo**. Para que no se repita: Ajustes › Aplicaciones › FocusZone ›
**Ahorro de batería › Sin restricciones** e **Inicio automático** activado.

### Ajustes del Xiaomi (si no los hiciste en la Fase 1)

**Ajustes › Aplicaciones › Administrar aplicaciones › FocusZone:**
**Ahorro de batería › Sin restricciones** e **Inicio automático** activado.
Sin esto, HyperOS puede cortar la sesión o apagar el servicio de bloqueo.

### Pruebas automáticas

La lógica de sesión, saldo y estadísticas tiene 35 pruebas que corren en el PC sin celular. En Android
Studio: panel izquierdo › `app/java/com.focuszone.app (test)/logica` › clic
derecho sobre la carpeta › **Run 'Tests in logica'**. Deben salir 35 en verde.

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
    ReglasSaldo.kt             Saldo diario, opciones de desbloqueo, inicio del día
    Estadisticas.kt            Racha, % de éxito, minutos por día e historial
    EstadoSesion.kt            Los 6 estados posibles de una sesión
    DetectorPosicion.kt        Acelerómetro → "boca abajo y quieto"
    MotorSesion.kt             Máquina de estados: arranque, alerta, cancelación, meta
  sesion/
    ServicioSesion.kt          Servicio en primer plano: sensores, cronómetro, vibración, guardar
    SesionActual.kt            Puente servicio ↔ pantallas
  bloqueo/                   Bloqueo de apps y No molestar (Fase 3)
    ServicioBloqueo.kt         Servicio de accesibilidad: detecta y bloquea apps
    Desbloqueo.kt              Ventana de desbloqueo activa (hasta qué hora)
    GestorSaldo.kt             Saldo de hoy y gastar minutos
    SolicitudDesbloqueo.kt     "Intentaste abrir X": aviso del servicio a la pantalla
    AppsBloqueadas.kt          Tu lista de apps distractoras (guardada en el celular)
    AppsInstaladas.kt          Lee las apps del menú; sugeridas y nunca-bloqueables
    ModoSilencio.kt            Activa/restaura No molestar sin pisar tu configuración
    PermisosBloqueo.kt         Comprueba y abre los ajustes de los permisos especiales
  datos/                     Base de datos Room
    SesionRegistro.kt          Tabla "sesiones"
    SesionDao.kt               Consultas (insertar, créditos de hoy y totales)
    DesbloqueoRegistro.kt      Tabla "desbloqueos" (minutos gastados)
    DesbloqueoDao.kt           Consultas (insertar, gastado hoy)
    BaseDatos.kt               El archivo focuszone.db (versión 2, con migración)
    RepositorioEstadisticas.kt Lee todo y calcula las estadísticas al vuelo
  ui/
    AppFocusZone.kt            Elige qué pantalla mostrar según el estado
    PantallaInicio.kt          Cámara + zona + elegir meta
    PantallaSesion.kt          Candado, anillo y cronómetro
    PantallaAlerta.kt          "No se permite el celular" + cuenta atrás
    PantallaResultado.kt       Completada / cancelada
    PantallaBloqueo.kt         Permisos + elegir apps distractoras
    PantallaDesbloqueo.kt      "X está bloqueada · ¿Desbloquear 5/10/15 min?"
    PantallaEstadisticas.kt    Racha, éxito, gráfica de la semana, saldo e historial
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
