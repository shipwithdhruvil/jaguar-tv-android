package org.screenlite.webkiosk.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import org.screenlite.webkiosk.R
import org.screenlite.webkiosk.ui.theme.Accent
import org.screenlite.webkiosk.ui.theme.DmSans
import org.screenlite.webkiosk.ui.theme.EmberGlow
import org.screenlite.webkiosk.ui.theme.Frame
import org.screenlite.webkiosk.ui.theme.Ink
import org.screenlite.webkiosk.ui.theme.Muted
import org.screenlite.webkiosk.ui.theme.Raised

enum class BrandState { Loading, Offline }

/**
 * What the TV shows before the player page is ready: the badge on black, and
 * either a thin ember bar (loading) or one line about the connection. It matches
 * the player's own idle screen, so the hand-over is seamless.
 */
@Composable
fun BrandScreen(state: BrandState, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Frame),
        contentAlignment = Alignment.Center
    ) {
        val badge = maxHeight * 0.36f
        val gap = maxHeight * 0.07f
        val glow = maxHeight * 1.1f
        Box(
            Modifier
                .size(glow)
                .background(EmberGlow)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.jaguar_badge),
                contentDescription = "Jaguar Fitness Club",
                modifier = Modifier.height(badge)
            )
            Spacer(Modifier.height(gap))
            when (state) {
                BrandState.Loading -> LoadingBar(width = badge * 0.7f)
                BrandState.Offline -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Waiting for the connection",
                        style = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.Medium, fontSize = 30.sp, letterSpacing = (-0.03).em, color = Ink),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Retrying on its own",
                        style = TextStyle(fontFamily = DmSans, fontSize = 18.sp, color = Muted),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingBar(width: Dp) {
    val transition = rememberInfiniteTransition(label = "loading")
    val progress by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "progress"
    )
    val density = LocalDensity.current
    Box(
        Modifier
            .width(width)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Raised)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(width * 0.35f)
                .offset { IntOffset(with(density) { (width * progress).roundToPx() }, 0) }
                .clip(RoundedCornerShape(2.dp))
                .background(Accent)
        )
    }
}
