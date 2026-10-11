package org.screenlite.webkiosk.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Off at closing on TVs without device admin (app/TvPower.kt). Many Android TVs leave that feature out (the Xiaomi
 * tried has none), but every TV from Android 9 has the accessibility "lock screen" action, which puts the TV in
 * standby the same way. This service reads nothing on screen and takes no events; it is only there to press that
 * one button. It is switched on once at install (runbook) or in the TV's accessibility settings.
 */
class PowerAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile
        private var running: PowerAccessibilityService? = null

        val isRunning: Boolean get() = running != null

        /** Standby now; false when the service is not switched on or the TV is older than Android 9. */
        fun lockScreen(): Boolean {
            val service = running ?: return false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
            return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN).also {
                Log.i("PowerAccessibility", "Lock screen: $it")
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        running = this
    }

    override fun onDestroy() {
        if (running === this) running = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit
}
