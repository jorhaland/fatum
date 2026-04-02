package com.fatum.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

// 🧠 El "Cerebro" del temporizador. Sobrevive a cambios de pantalla.
object FocusTimerCore {
    enum class TimerState { IDLE, RUNNING, FINISHED }
    val state = MutableStateFlow(TimerState.IDLE)
    val durationMinutes = MutableStateFlow(25)
    val remainingMs = MutableStateFlow(25 * 60_000L)
    var startTs = 0L
}

// 🔔 El Servicio que mantiene vivo el reloj en segundo plano y muestra la notificación
class FocusTimerService : Service() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val CHANNEL_ID = "fatum_timer_channel"
        const val NOTIF_ID = 1005
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        // Le dice al bloqueador de accesibilidad que puede empezar a disparar
        getSharedPreferences("fatum_prefs", Context.MODE_PRIVATE).edit().putBoolean("is_focus_active", true).apply()
        startForeground(NOTIF_ID, buildNotif("Calculando tiempo..."))

        scope.launch {
            while (FocusTimerCore.state.value == FocusTimerCore.TimerState.RUNNING) {
                delay(1000)
                val elapsed = System.currentTimeMillis() - FocusTimerCore.startTs
                val rem = (FocusTimerCore.durationMinutes.value * 60_000L) - elapsed

                if (rem <= 0) {
                    FocusTimerCore.remainingMs.value = 0
                    FocusTimerCore.state.value = FocusTimerCore.TimerState.FINISHED
                    getSharedPreferences("fatum_prefs", Context.MODE_PRIVATE).edit().putBoolean("is_focus_active", false).apply()
                    notificationManager.notify(NOTIF_ID, buildNotif("¡Sesión completada!"))
                    stopForeground(STOP_FOREGROUND_DETACH)
                    break
                } else {
                    FocusTimerCore.remainingMs.value = rem
                    notificationManager.notify(NOTIF_ID, buildNotif("Tiempo restante: ${fmtMs(rem)}"))
                }
            }
        }
        return START_STICKY
    }

    private fun fmtMs(ms: Long) = "%02d:%02d".format(ms / 60_000, (ms % 60_000) / 1_000)

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        getSharedPreferences("fatum_prefs", Context.MODE_PRIVATE).edit().putBoolean("is_focus_active", false).apply()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotif(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Modo Enfoque Activo")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setSilent(true)
            .build()

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Temporizador de Enfoque", NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
    }
}