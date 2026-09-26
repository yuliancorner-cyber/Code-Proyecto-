# FocusZone

App Android de **enfoque para estudiar**, de uso personal (no se publica en Play Store).

La idea: dejas el celular boca abajo dentro de una "zona de enfoque" marcada sobre tu
escritorio, arranca un cronómetro, y si lo levantas antes de tiempo la app te avisa y
pierdes la recompensa. Al completar la sesión ganas créditos de "tiempo de pantalla".

- **Package / identificador:** `com.focuszone.app`
- **Android mínimo:** 8.0 (API 26)
- **Stack:** Kotlin + Jetpack Compose + Material 3

---

## Estado actual: Fase 1 (MVP) — lista para probar

Se puede usar a diario: eliges una meta, dejas el celular boca abajo, corre el
cronómetro, y si lo levantas antes de tiempo te avisa y pierdes la recompensa.

### Hoja de ruta

| Fase | Qué incluye | Estado |
|---|---|---|
| 0 | Proyecto Kotlin + Compose que compila e instala | ✅ Hecho |
| 1 | MVP: zona simple sobre la cámara, detección boca-abajo por sensores, cronómetro, pantalla de bloqueo, créditos en Room | 🧪 Por probar |
| 2 | Zona AR real con ARCore (colocar, arrastrar, redimensionar, rotar) | Pendiente |
| 3 | Silenciar notificaciones y bloquear apps distractoras | Pendiente |
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

## Cómo probar la Fase 1

### Antes de empezar (solo una vez, en el Xiaomi)

MIUI/HyperOS mata los servicios en segundo plano para ahorrar batería. Para que
la sesión no se corte con la pantalla apagada:

**Ajustes › Aplicaciones › Administrar aplicaciones › FocusZone › Ahorro de batería
› Sin restricciones.** En la misma pantalla, activa también **Inicio automático**.

### Recorrido de prueba

1. Abre la app. Acepta el permiso de **cámara**: verás el escritorio con un
   rectángulo punteado encima.
2. Elige **15 min** y pulsa **Iniciar sesión**. Acepta el permiso de
   **notificaciones**.
3. Deja el celular **boca abajo** sobre la mesa. La zona se pone verde
   (*¡Detectado!*) y a los 2 s arranca el cronómetro, sin vibrar. Si lo volteas
   solo un instante (menos de 1 s) alcanzas a ver *Sesión activa* con el candado,
   el anillo y el cronómetro. Si lo mantienes levantado, salta la alerta (paso 4).
4. **Prueba la trampa:** levántalo. Vibra fuerte y aparece la pantalla ámbar
   *"No se permite el celular — aún no terminas, vuelve al trabajo"* con cuenta
   atrás de 5. Déjalo boca abajo antes del 0: la sesión sigue.
5. **Prueba la cancelación:** levántalo y espera a que llegue a 0 → *Sesión
   cancelada*, sin créditos.
6. **Prueba completarla:** una sesión de 15 min sin tocarlo → vibración doble,
   *¡Sesión completada! +3 min*.
7. **Prueba que se guarda:** cierra la app del todo (deslízala fuera de
   recientes) y ábrela: arriba debe seguir el *Tiempo de pantalla ganado*.

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
| `PACKAGE_USAGE_STATS` | Saber qué app está en pantalla para bloquear distracciones | **A mano**: Ajustes > Apps > Acceso especial > Acceso a datos de uso (Fase 3) |

Los sensores de movimiento (acelerómetro y giroscopio) **no necesitan permiso**: cualquier
app puede leerlos.

En la Fase 3 harán falta además dos accesos especiales que Android obliga a activar
manualmente en Ajustes: **acceso a notificaciones** (para silenciarlas) y, si lo
decidimos así, un **servicio de accesibilidad** (para bloquear apps). Te avisaré con
instrucciones paso a paso cuando lleguemos ahí.
