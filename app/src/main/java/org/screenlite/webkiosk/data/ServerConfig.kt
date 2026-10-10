package org.screenlite.webkiosk.data

import androidx.core.net.toUri
import org.screenlite.webkiosk.BuildConfig

/** Which server the TV talks to. Release builds are always live; debug builds can choose. */
enum class ServerChoice { LIVE, MAC, CUSTOM }

object ServerConfig {
    const val LIVE_URL: String = BuildConfig.LIVE_URL

    /** The dev web server on the Mac, as the emulator sees it. */
    const val MAC_URL = "http://10.0.2.2:5180/tv"

    const val SERVER_CHOICE: Boolean = BuildConfig.SERVER_CHOICE

    /** The start URL to use for a stored value; release builds ignore whatever is stored. */
    fun resolve(stored: String?): String =
        if (!SERVER_CHOICE || stored.isNullOrBlank()) LIVE_URL else stored

    fun choiceFor(url: String): ServerChoice = when (url) {
        LIVE_URL -> ServerChoice.LIVE
        MAC_URL -> ServerChoice.MAC
        else -> ServerChoice.CUSTOM
    }

    fun isValidCustom(url: String): Boolean {
        val uri = url.trim().toUri()
        val scheme = uri.scheme?.lowercase()
        return (scheme == "http" || scheme == "https") && !uri.host.isNullOrEmpty()
    }

    /** scheme://host:port with default ports filled in, or null if the URL has none. */
    fun origin(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val uri = url.toUri()
        val scheme = uri.scheme?.lowercase() ?: return null
        val host = uri.host?.lowercase() ?: return null
        val port = when {
            uri.port != -1 -> uri.port
            scheme == "https" -> 443
            scheme == "http" -> 80
            else -> -1
        }
        return "$scheme://$host:$port"
    }

    fun sameOrigin(a: String?, b: String?): Boolean {
        val oa = origin(a) ?: return false
        return oa == origin(b)
    }
}
