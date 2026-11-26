# JARVIS

Asistente personal para Android, de uso propio (no se publica en Play Store).
La app está **en inglés**, con tema negro AMOLED y acento cian.

- **Identificador:** `com.jarvis.app` (versión de pruebas: `com.jarvis.app.debug`, "JARVIS Dev")
- **Android mínimo:** 8.0 (API 26)
- **Stack:** Kotlin + Jetpack Compose + Material 3 + Room (igual que FocusZone)
- **Internet:** solo para el clima (Open-Meteo, sin clave y sin GPS). Todo lo demás vive en el celular.

## Qué hace (versión 0.1)

| Pestaña | Qué incluye |
|---|---|
| **Today** | Saludo, % de avance del día, clima de tu ciudad, botón para oír el resumen, rutina de la mañana, tareas de hoy (y atrasadas), hábitos de hoy, y **+** para captura rápida |
| **Tasks** | Tareas con *intención de implementación* (James Clear): qué, **cuándo** y **dónde**; primer paso de dos minutos, prioridad, proyecto, repetición, recordatorio y pasos |
| **Habits** | La cuadrícula mensual de tu hoja de cálculo (hábitos × días, con `Count`, `%` y `% del día`), rachas, aviso *"never miss twice"*, meta diaria (agua: 8 vasos) y recordatorios |
| **Brain** | Notas del "segundo cerebro": bandeja de entrada + categorías PARA (Projects, Areas, Resources, Archive), búsqueda, fijar, y convertir una nota en tarea |
| **Settings** | Nombre, ciudad, alarma, voz, rutina, permisos del celular y copia de seguridad |

**La alarma (5:15 AM por defecto, editable):** suena un tono que sube de volumen,
y a los 20 s JARVIS dice con voz grave algo como:

> *"Good morning, Julian. It's 5:15 AM on Monday, November 16. Right now it's 14
> degrees and overcast in Bogotá. Today's high is 21 and the low is 10, with a 35
> percent chance of rain. First things first: make your bed, do your push-ups,
> organize your space, and drink a big glass of water. You have 2 tasks today.
> First, study calculus at 8:00 AM in the library…"*

Luego el tono vuelve, hasta que toques **I'm up** (repite el resumen una vez, ya
despierto, y muestra la rutina para ir marcándola) o **Snooze**. Si nadie la
apaga en 10 minutos se detiene sola y deja una notificación.

Los hábitos del resumen se dicen solo como cantidad ("10 habits"), no por nombre.

**Check-ins de hábitos (v0.2):** a las **12:00 PM, 5:00 PM y 9:00 PM** (editables)
llega una notificación que pregunta, uno por uno, por los hábitos que aún no
marcaste hoy. Respondes ahí mismo, sin abrir la app, y se llena la cuadrícula:

| Botón | Qué hace |
|---|---|
| **Yes** | Lo marca como hecho y pasa al siguiente |
| **Not today** | No lo vuelve a preguntar hoy (queda sin marcar) |
| **Later** | Lo pregunta en la próxima ronda |
| **+1** / **All 8** | Para hábitos con contador (agua): suma uno o lo completa |

- Un hábito con hora en su plan (p. ej. *read 10 pages at 9:00 PM*) no se pregunta
  antes de esa hora. La última ronda del día pregunta por todo lo pendiente.
- Al terminar sale un resumen corto (*"All caught up · 10 of 10"*) que se va solo.
- Si marcas algo desde la app, la pregunta abierta se actualiza sola.
- **Settings › Habit check-ins:** encender/apagar, cambiar, añadir o quitar horas,
  y **Check in now** para probarlo al momento.
- En cada hábito (pestaña Habits › tocar el hábito) puedes apagar **Ask me in check-ins**.
- Con el celular bloqueado, si ocultas el contenido sensible en Ajustes, solo se ve
  *"Habit check-in"*, nunca el nombre del hábito.
- **Xiaomi:** para que la pregunta aparezca flotando arriba de la pantalla, en
  *Ajustes › Notificaciones › JARVIS Dev* activa **Notificaciones flotantes**
  (en el canal *Habit check-ins*).

## Instalarla

1. En Android Studio: **Git › Pull…** y espera a que termine de sincronizar
   (la primera vez descarga librerías nuevas).
2. En el desplegable junto a **Run ▶** elige **jarvis** y pulsa **Run ▶**.
   Se instala como **JARVIS Dev**.
3. En la bienvenida: tu nombre, tu **ciudad** (se busca por nombre, sin GPS) y la
   hora de la alarma. Acepta el permiso de notificaciones.

Para la versión final firmada (**JARVIS**), los mismos pasos que en FocusZone
(*Build › Generate Signed App Bundle or APK…*), eligiendo el módulo **jarvis**;
puedes usar la misma llave `.jks`. Para pasar tus datos de "JARVIS Dev" a
"JARVIS": **Settings › Backup › Export** en una, **Import** en la otra.

## Ajustes del Xiaomi (imprescindibles para que la alarma suene)

Dentro de la app, **Settings › Make the alarm reliable** muestra qué falta y te
lleva a cada ajuste. En HyperOS/MIUI hay que hacer además, en
**Ajustes › Aplicaciones › JARVIS Dev**:

1. **Inicio automático:** activado (sin esto, tras reiniciar el celular no hay alarma
   hasta que abras la app).
2. **Ahorro de batería:** *Sin restricciones*.
3. **Otros permisos:** permitir **Mostrar en la pantalla de bloqueo** y
   **Mostrar ventanas emergentes mientras se ejecuta en segundo plano**
   (para que la pantalla de la alarma aparezca con el celular bloqueado).
4. **Candado en recientes** (mantén pulsada la tarjeta de la app › 🔒), como en FocusZone.
5. Sube el **volumen de alarma** del celular: el tono y la voz suenan por ese volumen.

**Prueba antes de fiarte:** *Settings › Test alarm* la hace sonar al momento.
Pruébala también con la pantalla apagada y bloqueada, y programa una alarma real
para dentro de unos minutos. Mientras no compruebes que te despierta, deja tu
alarma de siempre como respaldo.

## La voz (grave, masculina, en inglés)

JARVIS usa el motor de texto a voz del celular. Para que suene bien:

1. Instala **"Servicios de voz de Google"** (*Speech Recognition & Synthesis*)
   desde Play Store si no lo tienes. JARVIS lo usa automáticamente si está instalado.
2. *Settings › Voice settings* › motor de Google › ⚙ › **Instalar datos de voz** ›
   descarga **English (United Kingdom)** e **English (United States)**.
3. En JARVIS: **Settings › Voice**. *Automatic* elige la voz masculina que encuentre
   (prefiere la británica). Android no dice si una voz es de hombre o de mujer, así
   que la elección automática es una deducción por el nombre de la voz: **toca cada
   voz de la lista para oírla y quédate con la que más te guste.**
4. **Voice tone:** *Deep* (por defecto) o *Very deep* bajan el tono de cualquier voz.
   **Voice speed** cambia la velocidad. **Test voice** dice una frase de prueba.

## Copia de seguridad

**Settings › Backup › Export** guarda un archivo `jarvis-backup-FECHA.json` donde
elijas (Drive, Descargas…). **Import** reemplaza TODO lo que haya por el contenido
del archivo. No hay sincronización en la nube: haz una copia de vez en cuando.

## Pruebas automáticas (152)

Corren en el PC, sin celular. En Android Studio: panel izquierdo ›
`jarvis/kotlin+java/com.jarvis.app (test)` › clic derecho › **Run 'Tests in…'**.

| Carpeta | Qué prueba |
|---|---|
| `logic/` (114) | Próxima alarma, repeticiones, intenciones, rachas y porcentajes, listas de tareas, el texto del resumen, lectura del clima, elección de voz, check-ins, copia de seguridad |
| `android/` (38) | Con un Android simulado (Robolectric): base de datos y acciones, lo que queda programado en AlarmManager, la alarma de principio a fin (suena → habla → *I'm up* / *Snooze*), los check-ins (se pulsan los botones de la notificación), la actualización de la base de datos sin perder datos, y un recorrido por las pantallas |

Para guardar además una imagen de cada pantalla en `jarvis/build/screenshots/`,
en la pestaña **Terminal**: `./gradlew :jarvis:testDebugUnitTest -Pscreenshots`

> Lo que estas pruebas **no** pueden comprobar (hace falta el celular): que el tono
> y la voz se oigan, cómo suena cada voz, y que HyperOS deje abrir la pantalla sobre
> el bloqueo. Por eso existe *Test alarm*.

## Mapa del código

```
jarvis/src/main/java/com/jarvis/app/
  JarvisApp.kt, MainActivity.kt   Arranque de la app
  logic/                          Reglas puras, sin Android (con tests)
    AlarmTime.kt                    Cuándo suena la próxima alarma
    Recurrence.kt                   Siguiente fecha de una tarea que se repite
    Intention.kt                    "I will [what] at [time] in [place]." / "After…, I will…"
    HabitStats.kt                   Rachas, never miss twice, % del mes y del día
    TaskLists.kt                    Secciones, orden, búsqueda, hora del recordatorio
    Briefing.kt                     El texto que JARVIS dice por la mañana
    Weather.kt                      Direcciones y lectura de Open-Meteo
    Voices.kt                       Elegir la voz grave y masculina
    CheckIns.kt                     Qué hábitos preguntar en cada check-in y cómo
    Days.kt, Formats.kt, Notes.kt, DayProgress.kt
  data/                           Base de datos Room, ajustes, clima, copia
    Entities.kt, Daos.kt            Tablas y consultas
    AppDatabase.kt                  jarvis.db + datos iniciales (tus 10 hábitos y la rutina)
    Repo.kt                         TODAS las acciones (crear, marcar, borrar…)
    Settings.kt, WeatherRepo.kt, Backup.kt, AppGraph.kt
  alarm/                          Alarma de la mañana y voz
    AlarmScheduler.kt               Programa la alarma en AlarmManager
    AlarmService.kt                 La hace sonar y hablar (servicio en primer plano)
    AlarmActivity.kt                Pantalla sobre el bloqueo
    Speaker.kt                      Texto a voz
    BriefingLoader.kt               Junta clima + rutina + tareas + hábitos de hoy
    AlarmReceiver.kt, BootReceiver.kt, AlarmState.kt
  reminders/                      Recordatorios de tareas y hábitos (notificaciones)
    CheckIns.kt                     Las preguntas del día y sus botones (Yes / Not today / Later)
  ui/                             Pantallas (Compose)
    JarvisRoot.kt                   Navegación: pestañas y editores
    TodayScreen.kt, TasksScreen.kt, TaskEditor.kt, HabitsScreen.kt, HabitEditor.kt,
    BrainScreen.kt, SettingsScreen.kt, SetupScreen.kt, RoutineEditor.kt
    components/, theme/             Piezas comunes, colores y tipografía
```

**Para cambiar…**
- los hábitos o la rutina iniciales: `data/AppDatabase.kt` › `Seed` (solo afecta a
  instalaciones nuevas; en la app se editan desde Habits y Today › ✎).
- lo que dice JARVIS: `logic/Briefing.kt`.
- los colores: `ui/theme/Color.kt`.

## Permisos declarados y para qué

| Permiso | Para qué |
|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | Clima y buscador de ciudades (Open-Meteo) |
| `POST_NOTIFICATIONS` | Recordatorios y notificación de la alarma |
| `USE_EXACT_ALARM` / `SCHEDULE_EXACT_ALARM` | Sonar al minuto exacto |
| `USE_FULL_SCREEN_INTENT` | Pantalla de alarma sobre el bloqueo |
| `FOREGROUND_SERVICE` (+`MEDIA_PLAYBACK`) | Servicio que hace sonar la alarma y habla |
| `RECEIVE_BOOT_COMPLETED` | Reprogramar alarma y recordatorios al encender |
| `WAKE_LOCK`, `VIBRATE` | Mantener el celular despierto mientras suena, y vibrar |

No pide ubicación, micrófono, cámara ni contactos.

## Hoja de ruta

| Fase | Qué incluye | Estado |
|---|---|---|
| 1 | Tareas, hábitos, rutina, alarma con voz y clima, recordatorios, copia de seguridad | ✅ Hecho (falta probar en el celular) |
| 1.1 | Check-ins: JARVIS pregunta por tus hábitos durante el día y respondes desde la notificación | ✅ Hecho (v0.2) |
| 2 | Notas tipo segundo cerebro (bandeja + PARA + búsqueda) | ✅ Básico hecho |
| 3 | Capa "Jarvis" con IA: chat con Claude que lee y edita tus datos, por voz | Pendiente (requiere una API key tuya) |
| — | Ideas: recordatorios de agua cada X horas, widget, Google Calendar en el resumen | Pendiente |

Datos del clima: [Open-Meteo.com](https://open-meteo.com/) (CC BY 4.0).
