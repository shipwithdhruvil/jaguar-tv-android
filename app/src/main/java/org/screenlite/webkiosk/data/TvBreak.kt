package org.screenlite.webkiosk.data

import android.content.Context

/**
 * "Take a break" from TV settings: for a while nothing brings Jaguar TV back to the front, so the TV can be used
 * for something else. A timed break ends by itself and the gym screen returns within one check; any break ends
 * when someone opens Jaguar TV, and "Until opened" also ends when the TV restarts. Kept in plain preferences
 * because the stay-on-top check reads it every few seconds, off any coroutine.
 */
object TvBreak {
    private const val PREFS = "jaguar_tv_break"
    private const val KEY_UNTIL = "until"
    private const val KEY_STARTED = "started"

    /** A break with no end time: until Jaguar TV is opened, or the TV restarts. */
    const val UNTIL_OPENED = Long.MAX_VALUE

    /** Opening Jaguar TV this soon after starting a break is the hand-over to the home screen, not a return. */
    private const val SETTLE_MS = 5_000L

    fun start(context: Context, until: Long) {
        prefs(context).edit()
            .putLong(KEY_UNTIL, until)
            .putLong(KEY_STARTED, System.currentTimeMillis())
            .apply()
    }

    fun end(context: Context) {
        prefs(context).edit().remove(KEY_UNTIL).remove(KEY_STARTED).apply()
    }

    fun isOn(context: Context): Boolean = prefs(context).getLong(KEY_UNTIL, 0L) > System.currentTimeMillis()

    /** Jaguar TV came to the front: someone opened it, so the break is over. */
    fun endOnReturn(context: Context) {
        val p = prefs(context)
        if (p.getLong(KEY_UNTIL, 0L) == 0L) return
        if (System.currentTimeMillis() - p.getLong(KEY_STARTED, 0L) < SETTLE_MS) return
        end(context)
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
