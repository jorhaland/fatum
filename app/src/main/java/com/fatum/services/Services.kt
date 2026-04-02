package com.fatum.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.view.WindowManager.LayoutParams.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.fatum.presentation.theme.FatumColors
import com.fatum.presentation.theme.FatumTheme
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

// ─────────────────────────────────────────────────────────────────────────────
// ServiceLifecycleOwner
// ─────────────────────────────────────────────────────────────────────────────
private class ServiceLifecycleOwner : SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    fun handleCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun handleResume() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun handleDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AppBlockerOverlayService
// ─────────────────────────────────────────────────────────────────────────────
class AppBlockerOverlayService : Service() {

    companion object {
        const val ACTION_START = "com.fatum.ACTION_START_BLOCKER"
        const val ACTION_STOP  = "com.fatum.ACTION_STOP_BLOCKER"
        const val CHANNEL_ID   = "fatum_blocker_channel"
        const val NOTIF_ID     = 1001

        val BLOCKED_PACKAGES = setOf(
            "com.instagram.android",
            "com.twitter.android",
            "com.zhiliaoapp.musically"
        )
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val composeLifecycleOwner = ServiceLifecycleOwner()

    // 📡 Receptor que escucha las alertas del monitor de uso
    private val overlayReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.fatum.ACTION_SHOW_OVERLAY") {
                val appName = intent.getStringExtra("app_name") ?: "La app"
                showOverlay(appName)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        composeLifecycleOwner.handleCreate()
        createNotificationChannel()

        // 📡 Encendemos la "radio"
        val filter = android.content.IntentFilter("com.fatum.ACTION_SHOW_OVERLAY")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(overlayReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(overlayReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startBlocking()
            ACTION_STOP  -> stopBlocking()
        }
        return START_STICKY
    }

    private fun startBlocking() {
        startForeground(NOTIF_ID, buildNotification())
        // Arrancamos el monitor de uso en segundo plano
        startService(Intent(this, UsageMonitorService::class.java))
    }

    private fun stopBlocking() {
        hideOverlay()
        // Apagamos el monitor de uso
        stopService(Intent(this, UsageMonitorService::class.java))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun showOverlay(blockedAppName: String) {
        if (overlayView != null) return
        val params = WindowManager.LayoutParams(
            MATCH_PARENT, MATCH_PARENT,
            TYPE_APPLICATION_OVERLAY,
            FLAG_NOT_FOCUSABLE or FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        composeLifecycleOwner.handleResume()

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(composeLifecycleOwner)
            setViewTreeViewModelStoreOwner(null)
            setViewTreeSavedStateRegistryOwner(composeLifecycleOwner)

            setContent {
                FatumTheme(darkTheme = true) {
                    BlockerOverlayContent(
                        appName = blockedAppName,
                        onInterrupt = {
                            sendBroadcast(Intent("com.fatum.ACTION_SESSION_INTERRUPTED"))
                            hideOverlay()
                            stopBlocking()
                        }
                    )
                }
            }
        }
        windowManager?.addView(overlayView, params)
    }

    fun hideOverlay() {
        overlayView?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
        }
        overlayView = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // 📡 Apagamos la "radio" para evitar fugas de memoria
        unregisterReceiver(overlayReceiver)
        hideOverlay()
        composeLifecycleOwner.handleDestroy()
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎯 FATUM – Sesión activa")
            .setContentText("Las redes sociales están bloqueadas.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Deep Work – Bloqueador",
            NotificationManager.IMPORTANCE_LOW
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Overlay UI
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BlockerOverlayContent(appName: String, onInterrupt: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FatumColors.Background.copy(alpha = 0.97f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🔒", style = MaterialTheme.typography.displayLarge)
            Text(
                text = "Sesión en curso",
                style = MaterialTheme.typography.headlineMedium,
                color = FatumColors.TextPrimary
            )
            Text(
                text = "$appName está bloqueado mientras tu sesión de Deep Work está activa.",
                style = MaterialTheme.typography.bodyLarge,
                color = FatumColors.TextMuted
            )
            Text(
                text = "Vuelve a FATUM y sigue concentrado. Tú puedes.",
                style = MaterialTheme.typography.bodyMedium,
                color = FatumColors.TextMuted
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onInterrupt,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FatumColors.Error)
            ) {
                Text("Interrumpir sesión y acceder")
            }
            Text(
                text = "La sesión se marcará como fallida.",
                style = MaterialTheme.typography.labelSmall,
                color = FatumColors.Error
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UsageMonitorService
// ─────────────────────────────────────────────────────────────────────────────
class UsageMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "fatum_monitor_channel"
        const val NOTIF_ID   = 1002
    }

    private val pollingThread = Thread {
        while (!Thread.interrupted()) {
            try {
                checkForegroundApp()
                Thread.sleep(1_000)
            } catch (_: InterruptedException) { break }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        pollingThread.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingThread.interrupt()
    }

    private fun checkForegroundApp() {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val now = System.currentTimeMillis()
        // Escaneamos exactamente los eventos del último minuto
        val events = usm.queryEvents(now - 60_000, now)
        val event = android.app.usage.UsageEvents.Event()
        var currentApp = ""

        // Buscamos cuál fue la última app en abrirse (RESUMED) y no cerrarse (PAUSED)
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                currentApp = event.packageName
            } else if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED) {
                if (currentApp == event.packageName) {
                    currentApp = "" // La app se fue al fondo
                }
            }
        }

        if (currentApp in AppBlockerOverlayService.BLOCKED_PACKAGES) {
            val appLabel = try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(currentApp, 0)).toString()
            } catch (_: Exception) { "Red Social" }

            // Forzamos el paquete para que Android 14 no bloquee el aviso interno
            val intent = Intent("com.fatum.ACTION_SHOW_OVERLAY").apply {
                setPackage(packageName)
                putExtra("app_name", appLabel)
            }
            sendBroadcast(intent)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FATUM monitorea apps")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Monitor de uso", NotificationManager.IMPORTANCE_MIN
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }
}