package org.screenlite.webkiosk.app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import org.screenlite.webkiosk.receiver.PowerAdminReceiver
import org.screenlite.webkiosk.service.PowerAccessibilityService

/**
 * Off at closing (gym-crm docs/TV-PLAN.md 3.4): the CRM's player page asks, at the gym's closing time, and the TV goes
 * to standby the same way as the remote's power button. An ordinary app may do that in one of two ways, each switched
 * on once at install (runbook) or from TV settings:
 *
 * - as a device admin with the single "lock the screen" policy (`DevicePolicyManager.lockNow`), on TVs that have
 *   device admin at all;
 * - through its accessibility service's "lock screen" action (Android 9 and later), on TVs that leave device admin out
 *   (the Xiaomi tried does).
 *
 * Switching the TV back on is left to the staff in the morning: most TVs drop off the network in standby, so nothing
 * could wake them reliably.
 */
object TvPower {
    private const val TAG = "TvPower"

    private fun admin(context: Context) = ComponentName(context, PowerAdminReceiver::class.java)
    private fun policy(context: Context) = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private fun hasDeviceAdmin(context: Context) = context.packageManager.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN)
    private fun isAdmin(context: Context) = hasDeviceAdmin(context) && policy(context).isAdminActive(admin(context))

    fun canSleep(context: Context): Boolean = isAdmin(context) || PowerAccessibilityService.isRunning

    /** Standby now. False when neither way was switched on (the page then stays black instead). */
    fun sleep(context: Context): Boolean {
        if (isAdmin(context)) {
            try {
                policy(context).lockNow()
                Log.i(TAG, "Switched off at closing time (device admin)")
                return true
            } catch (e: SecurityException) {
                Log.e(TAG, "Device admin refused to switch off", e)
            }
        }
        if (PowerAccessibilityService.lockScreen()) {
            Log.i(TAG, "Switched off at closing time (accessibility)")
            return true
        }
        Log.w(TAG, "Asked to switch off, but Jaguar TV may not: allow it in TV settings")
        return false
    }

    /**
     * Android's own screen for allowing it: the device admin prompt where the TV has device admin, else the TV's
     * accessibility settings (switch on "Jaguar TV: off at closing"). Some TVs have neither; then only adb can do it.
     */
    fun allowIntent(context: Context): Intent =
        if (hasDeviceAdmin(context)) {
            Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin(context))
                .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Lets Jaguar TV switch the TV off at the gym's closing time.")
        } else {
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        }

    /**
     * Gives the device admin back, for example before uninstalling (Android keeps an admin app from being removed). The
     * accessibility way is switched off in the TV's accessibility settings, which this opens.
     */
    fun stopSleeping(context: Context): Intent? {
        if (isAdmin(context)) {
            policy(context).removeActiveAdmin(admin(context))
            return null
        }
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }
}
