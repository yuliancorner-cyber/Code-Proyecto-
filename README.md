# FocusZone

App Android de **enfoque para estudiar**, de uso personal (no se publica en Play Store).

La idea: dejas el celular boca abajo dentro de una "zona de enfoque" marcada sobre tu
escritorio, arranca un cronómetro, y si lo levantas antes de tiempo la app te avisa y
pierdes la recompensa. Al completar la sesión ganas créditos de "tiempo de pantalla".

- **Package / identificador:** `com.focuszone.app`
- **Android mínimo:** 8.0 (API 26)
- **Stack:** Kotlin + Jetpack Compose + Material 3

---

## Estado actual: Fase 0 (entorno) ✅

La app compila, se instala y abre una pantalla de bienvenida. Todavía **no** hace nada
funcional: no hay cámara, ni sensores, ni cronómetro. Eso es la Fase 1.

### Hoja de ruta

| Fase | Qué incluye | Estado |
|---|---|---|
| 0 | Proyecto Kotlin + Compose que compila e instala | ✅ Hecho |
| 1 | MVP: zona simple sobre la cámara, detección boca-abajo por sensores, cronómetro, pantalla de bloqueo, créditos en Room | Pendiente |
| 2 | Zona AR real con ARCore (colocar, arrastrar, redimensionar, rotar) | Pendiente |
| 3 | Silenciar notificaciones y bloquear apps distractoras | Pendiente |
| 4 | Historial de sesiones y estadísticas de créditos | Pendiente |
| 5 | Ícono, splash, modo oscuro, APK final | Pendiente |

---

## Cómo probar la Fase 0

### 1. ¿Tienes Android Studio?

Ábrelo desde el menú de aplicaciones de tu PC. Si no aparece, descárgalo gratis desde
<https://developer.android.com/studio> e instálalo con las opciones por defecto
(el instalador descarga solo el SDK de Android que hace falta).

Necesitas la versión **Ladybug (2024.2)** o posterior para que entienda este proyecto
sin quejarse.

### 2. Abrir el proyecto

`File > Open…` y selecciona la **carpeta raíz** de este repositorio (la que contiene
`settings.gradle.kts`). **No** abras la carpeta `app`.

Android Studio hará el "Gradle Sync" (descarga las librerías). La primera vez tarda
varios minutos y necesita internet. Si te ofrece actualizar el Android Gradle Plugin,
puedes aceptar; si algo se rompe, avísame y lo revisamos.

### 3. Preparar el celular

1. Ajustes > Acerca del teléfono > toca **7 veces** "Número de compilación".
   Aparece el mensaje "Ya eres desarrollador".
2. Ajustes > Sistema > **Opciones de desarrollador** > activa **Depuración por USB**.
3. Conecta el celular por USB y acepta el diálogo *"¿Permitir la depuración USB?"*.

Si no tienes cable a mano, en Opciones de desarrollador existe **Depuración inalámbrica**;
en Android Studio se usa con `Device Manager > Pair using Wi-Fi`.

### 4. Instalar y ejecutar

Elige tu celular en el desplegable de dispositivos (arriba, junto al botón ▶) y pulsa
**Run ▶**.

### Qué deberías ver

Una pantalla con fondo oscuro (o claro, según el tema de tu celular), un círculo con
un candado verde, el título **FocusZone**, la frase *"Deja el celular boca abajo y gana
tiempo de pantalla."* y abajo, en verde, *"Fase 0 lista: el entorno funciona."*

En el menú de aplicaciones debe aparecer el ícono de un candado verde sobre fondo azul
oscuro, con el nombre **FocusZone**.

Si ves eso, la Fase 0 está aprobada.

### Sin conectar el celular

También puedes ver la pantalla dentro de Android Studio: abre
`app/src/main/java/com/focuszone/app/ui/PantallaBienvenida.kt` y pulsa **Split** o
**Design** arriba a la derecha. Verás dos vistas previas (tema oscuro y claro).
Esto no prueba que la app se instale, pero confirma que el código compila.

---

## Mapa del proyecto

```
settings.gradle.kts          Qué módulos hay y de dónde bajar las librerías
build.gradle.kts             Compilación raíz (solo declara plugins)
gradle.properties            Ajustes de Gradle (memoria, AndroidX)
gradle/libs.versions.toml    ← TODAS las versiones de librerías viven aquí
gradlew / gradlew.bat        Lanzador de Gradle (no hace falta instalarlo aparte)

app/
  build.gradle.kts           Configuración de la app: minSdk, permisos de compilación, dependencias
  src/main/
    AndroidManifest.xml      Permisos y declaración de pantallas
    java/com/focuszone/app/
      MainActivity.kt        Punto de entrada: la pantalla que abre Android
      ui/
        PantallaBienvenida.kt   Pantalla actual (Fase 0)
        theme/
          Color.kt           Paleta de colores
          Theme.kt           Tema claro/oscuro
          Type.kt            Tamaños de letra
    res/
      values/                Textos (strings.xml), colores y tema base
      values-night/          Variantes para modo oscuro
      drawable/              Dibujo vectorial del candado del ícono
      mipmap-anydpi-v26/     Ícono adaptativo de la app
      xml/                   Reglas de copia de seguridad
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
