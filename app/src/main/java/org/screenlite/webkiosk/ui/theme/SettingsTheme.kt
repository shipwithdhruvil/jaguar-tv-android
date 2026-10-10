package org.screenlite.webkiosk.ui.theme

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val settingsColorScheme = darkColorScheme(
    background = Frame,
    surface = Graphite,
    surfaceVariant = Raised,
    primary = Accent,
    onPrimary = PaperInk,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Muted,
    outline = Hairline,
    error = AccentText,
)

@Composable
fun TvSettingsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = settingsColorScheme,
        typography = Typography(bodyLarge = RowLabel, titleLarge = TitleMedium, headlineLarge = TitleLarge, labelMedium = Caption),
        content = content
    )
}

fun isTvDevice(): Boolean {
    val uiMode = android.content.res.Resources.getSystem().configuration.uiMode
    return (uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION
}
