package com.fatum.services

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class AppBlockerAccessibilityService : AccessibilityService() {

    companion object {
        val BLOCKED_PACKAGES = setOf(
            "com.instagram.android",
            "com.instagram.barcelona",
            "com.twitter.android",
            "com.zhiliaoapp.musically",      // TikTok Global
            "com.ss.android.ugc.trill",      // TikTok Americas
            "com.zhiliaoapp.musically.go"    // TikTok Lite
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            val prefs = getSharedPreferences("fatum_prefs", Context.MODE_PRIVATE)
            val isSessionActive = prefs.getBoolean("is_focus_active", false)

            if (isSessionActive && packageName in BLOCKED_PACKAGES) {
                // Expulsa a la pantalla de inicio INMEDIATAMENTE de forma invisible
                performGlobalAction(GLOBAL_ACTION_HOME)
                Toast.makeText(this, "¡FATUM te vigila! Vuelve al trabajo.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onInterrupt() {}
}