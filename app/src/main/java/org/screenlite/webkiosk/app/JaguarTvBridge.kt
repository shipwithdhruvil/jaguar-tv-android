package org.screenlite.webkiosk.app

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import org.screenlite.webkiosk.data.ServerConfig

/**
 * What the CRM's player page may ask of the app, as `window.JaguarTv` (gym-crm apps/web/src/tv/tvApp.ts). Only the
 * page on the configured server's origin is answered; navigation is locked there anyway, this is the second check.
 * Calls arrive on a WebView thread, so the page's address is read from what the client last saw, not from the view.
 */
class JaguarTvBridge(
    private val context: Context,
    private val startUrl: String,
    private val pageUrl: () -> String?,
) {
    private fun ours(): Boolean {
        val url = pageUrl()
        val allowed = url != null && ServerConfig.sameOrigin(url, startUrl)
        if (!allowed) Log.w("JaguarTvBridge", "Ignored a call from $url")
        return allowed
    }

    /** Off at closing: standby now. False when it cannot (see TvPower); the page stays black then. */
    @JavascriptInterface
    fun sleep(): Boolean = ours() && TvPower.sleep(context)
}
