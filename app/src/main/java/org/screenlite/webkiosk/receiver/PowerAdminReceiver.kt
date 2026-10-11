package org.screenlite.webkiosk.receiver

import android.app.admin.DeviceAdminReceiver

/** The device admin behind off at closing (app/TvPower.kt). It holds one policy, force-lock, and does nothing else. */
class PowerAdminReceiver : DeviceAdminReceiver()
