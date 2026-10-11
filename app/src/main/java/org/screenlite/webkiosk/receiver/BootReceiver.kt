package org.screenlite.webkiosk.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.screenlite.webkiosk.app.StayOnTopServiceStarter
import org.screenlite.webkiosk.data.TvBreak
import org.screenlite.webkiosk.service.StayOnTopService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("BootReceiver", "onReceive called with action: ${intent?.action}")

        // A restart ends any break: the gym screen comes back with the TV.
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            TvBreak.end(context)
        }

        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (!StayOnTopService.isRunning) {
                    StayOnTopServiceStarter.ensureRunning(context)
                    Log.d("BootReceiver", "StayOnTopService started successfully")
                } else {
                    Log.d("BootReceiver", "StayOnTopService is already running")
                }
            }
        }
    }
}