package org.screenlite.webkiosk.components

import android.app.Activity
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.screenlite.webkiosk.data.KioskSettingsFactory

@Composable
fun MainScreen(activity: Activity, modifier: Modifier) {
    val context = LocalContext.current
    // Null until the saved address is read, so the first frame is the branded
    // loading screen rather than a blank page.
    var url by remember { mutableStateOf<String?>(null) }
    val kioskSettings = remember { KioskSettingsFactory.get(context) }

    LaunchedEffect(Unit) {
        kioskSettings.getStartUrl().collect { newUrl ->
            url = newUrl
        }
    }

    val current = url
    if (current == null) {
        BrandScreen(BrandState.Loading, modifier)
    } else {
        key(current) {
            WebViewComponent(url = current, activity = activity, modifier)
        }
    }
}