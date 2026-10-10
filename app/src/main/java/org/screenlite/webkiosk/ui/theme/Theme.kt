package org.screenlite.webkiosk.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

// Always dark: the TV sits in a gym, and the brand is black with the ember accent.
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ScreenliteWebKioskTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = PaperInk,
            background = Frame,
            onBackground = Ink,
            surface = Graphite,
            onSurface = Ink,
        ),
        typography = Typography,
        content = content
    )
}
