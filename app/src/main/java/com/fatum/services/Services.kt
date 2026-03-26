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
import androidx.lifecycle.ProcessLifecycleOwner
import com.fatum.presentation.theme.FatumColors
import com.fatum.presentation.theme.FatumTheme

// ─────────────────────────────────────────────────────────────────────────────
// AppBlockerOverlayService  –  RF-5.2 / RF-5.3 / RF-5.4
//
// Foreground service that draws a full-screen overlay (SYSTEM_ALERT_WINDOW)
// over blocked apps while a Deep Work session is active.
// ─────────────────────────────────────────────────────────────────────────────
class AppBlockerOverlayService : Service() {

    companion object {
        const val ACTION_START = "com.fatum.ACTION_START_BLOCKER"
        const val ACTION_STOP  = "com.fatum.ACTION_STOP_BLOCKER"
        const val CHANNEL_ID   = "fatum_blocker_channel"
        const val NOTIF_ID     = 1001

        /** Social apps to block during Deep Work sessions (RF-5.2). */
        val BLOCKED_PACKAGES = setOf(
            "com.instagram.android",
            "com.twitter.android",      // X (formerly Twitter)
            "com.zhiliaoapp.musically"  // TikTok
        )
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
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
        // UsageMonitorService handles detecting the blocked app; this service
        // just exposes showOverlay() for when the monitor fires.
    }

    private fun stopBlocking() {
        hideOverlay()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Shows the intervention screen over a blocked app (RF-5.3). */
    fun showOverlay(blockedAppName: String) {
        if (overlayView != null) return  // Already showing
        val params = WindowManager.LayoutParams(
            MATCH_PARENT, MATCH_PARENT,
            TYPE_APPLICATION_OVERLAY,
            FLAG_NOT_FOCUSABLE or FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(ProcessLifecycleOwner.get())
            setViewTreeViewModelStoreOwner(null)
            setContent {
                FatumTheme(darkTheme = true) {
                    BlockerOverlayContent(
                        appName = blockedAppName,
                        onInterrupt = {
                            // RF-5.4 – user surrenders; stop the session
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
        overlayView?.let { windowManager?.removeView(it) }
        overlayView = null
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎯 FATUM – Sesión activa")
            .setContentText("Las redes sociales están bloqueadas.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }

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
// Overlay UI Composable (RF-5.3)
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
                color = FatumColors.Primary
            )
            Text(
                text = "$appName está bloqueado mientras tu sesión de Deep Work está activa.",
                style = MaterialTheme.typography.bodyLarge,
                color = FatumColors.PrimaryVariant
            )
            Text(
                text = "Vuelve a FATUM y sigue concentrado. Tú puedes.",
                style = MaterialTheme.typography.bodyMedium,
                color = FatumColors.AccentSecondary
            )
            Spacer(Modifier.height(16.dp))
            // ── RF-5.4: The only escape valve ────────────────────────────
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
// UsageMonitorService  –  RF-5.2
//
// Polls UsageStatsManager every 1 second to detect if the user switches
// to a blocked app, then triggers the overlay.
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
        val stats = usm.queryUsageStats(
            android.app.usage.UsageStatsManager.INTERVAL_DAILY,
            now - 5_000, now
        )
        val topPackage = stats?.maxByOrNull { it.lastTimeUsed }?.packageName ?: return
        if (topPackage in AppBlockerOverlayService.BLOCKED_PACKAGES) {
            val appLabel = try {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(topPackage, 0)
                ).toString()
            } catch (_: Exception) { topPackage }
            // Delegate overlay to AppBlockerOverlayService via broadcast
            sendBroadcast(Intent("com.fatum.ACTION_SHOW_OVERLAY").apply {
                putExtra("app_name", appLabel)
            })
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
