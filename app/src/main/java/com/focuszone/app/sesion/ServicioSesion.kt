package com.focuszone.app.sesion

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.focuszone.app.MainActivity
import com.focuszone.app.R
import com.focuszone.app.datos.BaseDatos
import com.focuszone.app.datos.SesionRegistro
import com.focuszone.app.logica.DetectorPosicion
import com.focuszone.app.logica.EstadoSesion
import com.focuszone.app.logica.MotorSesion
import com.focuszone.app.logica.Recompensa
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano que "vigila" la sesion.
 *
 * Por que un servicio y no la pantalla: si apagas la pantalla, Android pausa
 * las pantallas (Activities), pero un servicio en primer plano sigue vivo.
 * A cambio, Android obliga a mostrar una notificacion permanente mientras
 * corre, para que el usuario sepa que algo esta activo.
 *
 * Que hace, en orden:
 *  1. Muestra la notificacion y mantiene la CPU despierta (wake lock).
 *  2. Escucha el acelerometro y pasa cada lectura por [DetectorPosicion].
 *  3. Alimenta al [MotorSesion] con esas lecturas y con un "tic" cada 250 ms.
 *  4. Publica cada estado nuevo en [SesionActual] para que lo vea la pantalla.
 *  5. Vibra en la alerta y al completar.
 *  6. Al terminar, guarda la sesion en Room y se detiene solo.
 */
class ServicioSesion : Service(), SensorEventListener {

    companion object {
        const val ACCION_INICIAR = "com.focuszone.app.INICIAR"
        const val ACCION_ABANDONAR = "com.focuszone.app.ABANDONAR"
        const val EXTRA_META_MIN = "meta_min"

        private const val TAG = "ServicioSesion"
        private const val CANAL_SESION = "sesion"
        private const val CANAL_RESULTADOS = "resultados"
        private const val ID_NOTIF_SESION = 1
        private const val ID_NOTIF_RESULTADO = 2
        private const val INTERVALO_TIC_MS = 250L
    }

    private val motor = MotorSesion()
    private val detector = DetectorPosicion()

    private lateinit var sensores: SensorManager
    private var acelerometro: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // Todo (sensores, tics, motor) corre en el hilo principal: asi no hay
    // dos cosas tocando el motor a la vez.
    private val manejador = Handler(Looper.getMainLooper())
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var sesionActiva = false
    private var metaMin = Recompensa.META_POR_DEFECTO
    private var inicioMillis = 0L
    private var ultimoEstado: EstadoSesion = EstadoSesion.Inactivo

    private val tic = object : Runnable {
        override fun run() {
            if (!sesionActiva) return
            motor.tick(ahora())
            alCambiarEstado()
            if (sesionActiva) manejador.postDelayed(this, INTERVALO_TIC_MS)
        }
    }

    /** Reloj que nunca retrocede (a diferencia de la hora del sistema). */
    private fun ahora(): Long = SystemClock.elapsedRealtime()

    // ---------------------------------------------------------------------
    // Ciclo de vida del servicio
    // ---------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        sensores = getSystemService(SensorManager::class.java)
        acelerometro = sensores.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        crearCanalesDeNotificacion()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACCION_INICIAR -> iniciar(intent.getIntExtra(EXTRA_META_MIN, Recompensa.META_POR_DEFECTO))
            ACCION_ABANDONAR -> {
                if (sesionActiva) {
                    motor.abandonar(ahora())
                    alCambiarEstado()
                } else {
                    stopSelf()
                }
            }
        }
        // NOT_STICKY: si Android llegara a matar el servicio, no lo resucita
        // con una sesion a medias (la daria por perdida).
        return START_NOT_STICKY
    }

    // Este servicio no se "enlaza" con nadie; se controla solo con Intents.
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (sesionActiva) {
            // Nos destruyeron a mitad de sesion: que la pantalla no se quede colgada.
            SesionActual.publicar(EstadoSesion.Inactivo)
        }
        liberarRecursos()
        alcance.cancel()
        super.onDestroy()
    }

    // ---------------------------------------------------------------------
    // Sesion
    // ---------------------------------------------------------------------

    private fun iniciar(metaMin: Int) {
        // Arrancar en primer plano es OBLIGATORIO en los primeros segundos
        // tras startForegroundService(), o Android cierra la app.
        val estadoParaNotificar =
            if (sesionActiva) ultimoEstado else EstadoSesion.Esperando(metaMin, false)
        entrarEnPrimerPlano(notificacionSesion(estadoParaNotificar))

        if (sesionActiva) return // doble toque en "Iniciar": ignorar

        val sensor = acelerometro
        if (sensor == null) {
            Log.e(TAG, "Este celular no tiene acelerometro")
            SesionActual.publicar(EstadoSesion.Inactivo)
            detenerServicio()
            return
        }

        sesionActiva = true
        this.metaMin = metaMin
        inicioMillis = System.currentTimeMillis()

        // Wake lock parcial: la CPU sigue despierta (y los sensores entregando
        // datos) aunque se apague la pantalla. Con tiempo maximo por seguridad,
        // para que nunca quede encendido si algo falla.
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FocusZone:sesion")
            .apply { acquire((metaMin + 10) * 60_000L) }

        detector.reiniciar()
        motor.iniciar(metaMin, ahora())
        ultimoEstado = EstadoSesion.Inactivo

        // SENSOR_DELAY_UI ≈ una lectura cada 60 ms: de sobra para detectar que
        // lo levantas, y gasta menos bateria que los modos rapidos.
        sensores.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        manejador.post(tic)
        alCambiarEstado()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!sesionActiva) return
        val t = ahora()
        val enPosicion = detector.procesar(event.values[0], event.values[1], event.values[2], t)
        motor.lectura(enPosicion, t)
        alCambiarEstado()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /** Se llama tras cada lectura o tic: reacciona solo si el estado cambio. */
    private fun alCambiarEstado() {
        val nuevo = motor.estado
        if (nuevo == ultimoEstado) return
        val anterior = ultimoEstado
        ultimoEstado = nuevo
        SesionActual.publicar(nuevo)

        // Efectos que solo ocurren al CAMBIAR DE FASE (no cada segundo).
        if (nuevo::class != anterior::class) {
            when (nuevo) {
                is EstadoSesion.Alerta -> vibrar(longArrayOf(0, 500, 150, 500, 150, 500))
                is EstadoSesion.Completada -> vibrar(longArrayOf(0, 200, 100, 200))
                else -> Unit
            }
            if (nuevo !is EstadoSesion.Completada && nuevo !is EstadoSesion.Cancelada &&
                nuevo !is EstadoSesion.Inactivo
            ) {
                notificar(ID_NOTIF_SESION, notificacionSesion(nuevo))
            }
        }

        when (nuevo) {
            is EstadoSesion.Completada, is EstadoSesion.Cancelada -> terminar(nuevo)
            EstadoSesion.Inactivo -> detenerServicio() // cancelaste antes de empezar
            else -> Unit
        }
    }

    /** Guarda la sesion en la base de datos y apaga el servicio. */
    private fun terminar(resultado: EstadoSesion) {
        sesionActiva = false
        liberarRecursos()

        val registro = when (resultado) {
            is EstadoSesion.Completada -> SesionRegistro(
                inicioMillis = inicioMillis,
                finMillis = System.currentTimeMillis(),
                metaMin = resultado.metaMin,
                estudiadoSeg = resultado.metaMin * 60L,
                completada = true,
                creditosMin = resultado.creditosMin
            )
            is EstadoSesion.Cancelada -> SesionRegistro(
                inicioMillis = inicioMillis,
                finMillis = System.currentTimeMillis(),
                metaMin = resultado.metaMin,
                estudiadoSeg = resultado.estudiadoSeg,
                completada = false,
                creditosMin = 0
            )
            else -> return
        }

        alcance.launch {
            try {
                BaseDatos.obtener(applicationContext).sesionDao().insertar(registro)
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo guardar la sesion", e)
            }
            if (resultado is EstadoSesion.Completada) {
                notificar(ID_NOTIF_RESULTADO, notificacionCompletada(resultado.creditosMin))
            }
            // Si mientras guardabamos ya empezaste otra sesion, no la apagamos.
            if (!sesionActiva) detenerServicio()
        }
    }

    private fun liberarRecursos() {
        manejador.removeCallbacks(tic)
        sensores.unregisterListener(this)
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun detenerServicio() {
        sesionActiva = false
        liberarRecursos()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ---------------------------------------------------------------------
    // Notificaciones y vibracion
    // ---------------------------------------------------------------------

    private fun entrarEnPrimerPlano(notificacion: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ exige declarar el TIPO de servicio en primer plano.
            startForeground(ID_NOTIF_SESION, notificacion, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(ID_NOTIF_SESION, notificacion)
        }
    }

    private fun crearCanalesDeNotificacion() {
        // Un "canal" agrupa notificaciones del mismo tipo; el usuario puede
        // silenciar o ajustar cada canal por separado en Ajustes.
        val gestor = getSystemService(NotificationManager::class.java)
        gestor.createNotificationChannel(
            NotificationChannel(
                CANAL_SESION,
                getString(R.string.notif_canal_sesion),
                NotificationManager.IMPORTANCE_LOW // sin sonido: solo informa
            )
        )
        gestor.createNotificationChannel(
            NotificationChannel(
                CANAL_RESULTADOS,
                getString(R.string.notif_canal_resultados),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    /** Al tocar la notificacion se abre la app. */
    private fun abrirApp(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun notificacionSesion(estado: EstadoSesion): Notification {
        val constructor = NotificationCompat.Builder(this, CANAL_SESION)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(abrirApp())

        when (estado) {
            is EstadoSesion.EnCurso -> constructor
                .setContentTitle(getString(R.string.sesion_activa))
                .setContentText(getString(R.string.notif_en_curso, estado.metaMin))
                // El propio Android dibuja un cronometro vivo en la notificacion.
                .setUsesChronometer(true)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis() - estado.estudiadoSeg * 1000)

            is EstadoSesion.Alerta -> constructor
                .setContentTitle(getString(R.string.notif_alerta_titulo))
                .setContentText(getString(R.string.alerta_trampa))

            else -> constructor
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.esperando_colocar))
        }
        return constructor.build()
    }

    private fun notificacionCompletada(creditosMin: Int): Notification =
        NotificationCompat.Builder(this, CANAL_RESULTADOS)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(getString(R.string.completada_titulo))
            .setContentText(getString(R.string.completada_ganaste, creditosMin))
            .setAutoCancel(true)
            .setContentIntent(abrirApp())
            .build()

    private fun notificar(id: Int, notificacion: Notification) {
        // Si no diste permiso de notificaciones (Android 13+), esto no muestra
        // nada, pero la sesion funciona igual.
        getSystemService(NotificationManager::class.java).notify(id, notificacion)
    }

    private fun vibrar(patron: LongArray) {
        val vibrador: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            getSystemService(Vibrator::class.java)
        }
        // -1 = no repetir el patron
        vibrador.vibrate(VibrationEffect.createWaveform(patron, -1))
    }
}
