package com.focuszone.app.bloqueo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.focuszone.app.MainActivity
import com.focuszone.app.R
import com.focuszone.app.logica.ReglasGuardian

/**
 * Servicio "guardian": corre todo el tiempo mientras tengas apps marcadas como
 * distractoras.
 *
 * Por que existe: HyperOS/MIUI "limpia" las apps en segundo plano para ahorrar
 * bateria, y al hacerlo detiene el servicio de accesibilidad que bloquea las
 * apps. Una app con un servicio en primer plano (con su notificacion fija) es
 * de las ultimas que el sistema mata, asi que el bloqueo aguanta mucho mas.
 *
 * Ademas vigila: cada minuto comprueba que el bloqueo siga funcionando y, si
 * no, avisa con una notificacion que lleva directo a reactivarlo.
 */
class ServicioGuardian : Service() {

    companion object {
        private const val TAG = "ServicioGuardian"
        private const val CANAL_GUARDIAN = "guardian"
        private const val CANAL_AVISOS = "avisos_bloqueo"
        private const val ID_NOTIF_GUARDIAN = 3
        private const val ID_NOTIF_AVISO = 4

        /**
         * Arranca el guardian si hay apps marcadas. Seguro de llamar muchas veces.
         * Si Android no deja arrancarlo en ese momento (p. ej. desde segundo
         * plano), no pasa nada: se intentara la proxima vez que abras la app.
         */
        fun iniciar(context: Context) {
            if (AppsBloqueadas.observar(context).value.isEmpty()) return
            try {
                ContextCompat.startForegroundService(context, Intent(context, ServicioGuardian::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo arrancar el guardian ahora", e)
            }
        }

        fun detener(context: Context) {
            context.stopService(Intent(context, ServicioGuardian::class.java))
        }
    }

    private val manejador = Handler(Looper.getMainLooper())
    private var fallosSeguidos = 0

    private val revision = object : Runnable {
        override fun run() {
            try {
                revisar()
            } catch (e: Exception) {
                Log.e(TAG, "Error al revisar el bloqueo", e)
            }
            manejador.postDelayed(this, ReglasGuardian.INTERVALO_REVISION_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        crearCanales()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Obligatorio en los primeros segundos tras startForegroundService().
        entrarEnPrimerPlano()
        manejador.removeCallbacks(revision)
        manejador.postDelayed(revision, ReglasGuardian.INTERVALO_REVISION_MS)
        // STICKY: si el sistema lo mata, Android lo vuelve a arrancar en cuanto puede.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        manejador.removeCallbacks(revision)
        super.onDestroy()
    }

    private fun revisar() {
        val estado = ReglasGuardian.estado(
            hayAppsMarcadas = AppsBloqueadas.observar(this).value.isNotEmpty(),
            accesibilidadActivada = PermisosBloqueo.accesibilidadActiva(this),
            servicioFuncionando = ServicioBloqueo.funcionando.value
        )
        when (estado) {
            ReglasGuardian.Estado.SIN_APPS -> {
                cancelarAviso()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ReglasGuardian.Estado.FUNCIONANDO -> {
                fallosSeguidos = 0
                cancelarAviso()
            }
            ReglasGuardian.Estado.DESACTIVADO, ReglasGuardian.Estado.DETENIDO -> {
                fallosSeguidos++
                if (ReglasGuardian.debeAvisar(fallosSeguidos)) avisar(estado)
            }
        }
    }

    // ---------------------------------------------------------------------
    // Notificaciones
    // ---------------------------------------------------------------------

    private fun entrarEnPrimerPlano() {
        val notificacion = NotificationCompat.Builder(this, CANAL_GUARDIAN)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(getString(R.string.guardian_titulo))
            .setContentText(getString(R.string.guardian_texto))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(abrir(Intent(this, MainActivity::class.java), codigo = 0))
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(ID_NOTIF_GUARDIAN, notificacion, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(ID_NOTIF_GUARDIAN, notificacion)
        }
    }

    /** "El bloqueo se detuvo": al tocarla te lleva a Ajustes > Accesibilidad. */
    private fun avisar(estado: ReglasGuardian.Estado) {
        val texto = getString(
            if (estado == ReglasGuardian.Estado.DESACTIVADO) R.string.aviso_bloqueo_desactivado
            else R.string.aviso_bloqueo_detenido
        )
        val notificacion = NotificationCompat.Builder(this, CANAL_AVISOS)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(getString(R.string.aviso_bloqueo_titulo))
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(abrir(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), codigo = 1))
            .build()
        getSystemService(NotificationManager::class.java).notify(ID_NOTIF_AVISO, notificacion)
    }

    private fun cancelarAviso() {
        getSystemService(NotificationManager::class.java).cancel(ID_NOTIF_AVISO)
    }

    private fun abrir(intent: Intent, codigo: Int): PendingIntent = PendingIntent.getActivity(
        this,
        codigo,
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun crearCanales() {
        val gestor = getSystemService(NotificationManager::class.java)
        // MIN: no hace ruido ni pone icono en la barra; queda abajo del todo.
        gestor.createNotificationChannel(
            NotificationChannel(CANAL_GUARDIAN, getString(R.string.canal_guardian), NotificationManager.IMPORTANCE_MIN)
        )
        gestor.createNotificationChannel(
            NotificationChannel(CANAL_AVISOS, getString(R.string.canal_avisos_bloqueo), NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
}
