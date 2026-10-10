package org.screenlite.webkiosk.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.webkit.*
import android.webkit.WebView.setWebContentsDebuggingEnabled
import androidx.annotation.RequiresApi
import org.screenlite.webkiosk.components.RotatedWebView
import org.screenlite.webkiosk.data.Rotation
import org.screenlite.webkiosk.data.ServerConfig

class WebViewManager(
    private val context: Context,
    /** The configured server; only its origin may load in the main frame. */
    private val startUrl: String,
    private val onError: (Boolean) -> Unit,
    private val onPageLoading: (Boolean) -> Unit
) {
    private var currentWebView: WebView? = null
    fun createWebView(rotation: Rotation = Rotation.ROTATION_0): WebView {
        val webView = RotatedWebView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            appliedRotation = rotation.degrees.toFloat()

            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()

            configureWebViewSettings()
            setupWebViewListeners()
        }

        currentWebView = webView
        return webView
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun WebView.configureWebViewSettings() {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            javaScriptCanOpenWindowsAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            mediaPlaybackRequiresUserGesture = false
            setSupportMultipleWindows(true)
            setWebContentsDebuggingEnabled(
                context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
            )
            val displayMetrics = context.resources.displayMetrics
            setInitialScale(calculateScale(displayMetrics))

            displayZoomControls = false
            builtInZoomControls = false
            setSupportZoom(false)

            textZoom = 100
            minimumFontSize = 1
            minimumLogicalFontSize = 1
            useWideViewPort = true
        }
    }

    private fun calculateScale(displayMetrics: DisplayMetrics): Int {
        val density = displayMetrics.density
        return (100 / density).toInt()
    }

    fun updateRotation(rotation: Rotation) {
        currentWebView?.let { webView ->
            if (webView is RotatedWebView) {
                webView.appliedRotation = rotation.degrees.toFloat()
            }

            ViewportMetaInjector.inject(webView)
        }
    }

    private fun WebView.setupWebViewListeners() {
        webViewClient = object : WebViewClient() {
            // Links and redirects to any other origin are dropped; subresources are unaffected.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return false
                if (ServerConfig.sameOrigin(request.url.toString(), startUrl)) return false
                Log.w("WebViewManager", "Blocked navigation to ${request.url}")
                return true
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                // Backstop for navigations that skip shouldOverrideUrlLoading (some redirects,
                // script-driven loads): back to the server's own page.
                if (url != null && url != "about:blank" && !ServerConfig.sameOrigin(url, startUrl)) {
                    Log.w("WebViewManager", "Left the server origin ($url), reloading $startUrl")
                    view.stopLoading()
                    view.loadUrl(startUrl)
                    return
                }
                super.onPageStarted(view, url, favicon)
                view.visibility = View.INVISIBLE
                onPageLoading(true)
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                ViewportMetaInjector.inject(view)
                view.postDelayed({
                    view.visibility = View.VISIBLE
                    onPageLoading(false)
                }, 1000)
            }

            @Suppress("DEPRECATION")
            @Deprecated("Deprecated in API 23")
            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                Log.e("WebViewManager", "Legacy page failed: $failingUrl, code=$errorCode, desc=$description")
                onPageLoading(false)
                onError(true)
                super.onReceivedError(view, errorCode, description, failingUrl)
            }

            @RequiresApi(Build.VERSION_CODES.M)
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) {
                    onPageLoading(false)
                    onError(true)
                    Log.e(
                        "WebViewManager",
                        "Main page failed: ${request.url}, code=${error.errorCode}, desc=${error.description}"
                    )
                } else {
                    Log.w(
                        "WebViewManager",
                        "Subresource failed: ${request.url}, code=${error.errorCode}, desc=${error.description}"
                    )
                }
            }

            override fun onScaleChanged(view: WebView, oldScale: Float, newScale: Float) {
                super.onScaleChanged(view, oldScale, newScale)
                if (newScale != 1.0f) {
                    view.scaleX = 1.0f
                    view.scaleY = 1.0f
                }
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean {
                // Popups never open; the page stays on the server's own origin.
                Log.w("WebViewManager", "Blocked popup window")
                return false
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Log.d(
                    "WebViewConsole",
                    "JS ${consoleMessage.messageLevel()}: ${consoleMessage.message()} @ ${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}"
                )
                return true
            }
        }
    }
}