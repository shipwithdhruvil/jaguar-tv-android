package org.screenlite.webkiosk.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Typography
import org.screenlite.webkiosk.R

val DmSans = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
    Font(R.font.dm_sans_bold, FontWeight.Bold),
)

// Titles and values are 500 with -0.03 em tracking; reading text is 400.
val TitleLarge = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 40.sp, letterSpacing = (-0.03).em, color = Ink)
val TitleMedium = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 28.sp, letterSpacing = (-0.03).em, color = Ink)
val RowLabel = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 20.sp, color = Ink)
val RowValue = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 20.sp, letterSpacing = (-0.01).em, color = Ink)
val Caption = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, color = Muted)
val ButtonText = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 20.sp)

@OptIn(ExperimentalTvMaterial3Api::class)
val Typography = Typography(
    bodyLarge = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp)
)
