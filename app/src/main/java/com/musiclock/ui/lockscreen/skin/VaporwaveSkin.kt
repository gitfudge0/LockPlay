package com.musiclock.ui.lockscreen.skin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val VwPink = Color(0xFFff71ce)
private val VwBlue = Color(0xFF01cdfe)
private val VwPurple = Color(0xFFb967ff)
private val VwMagenta = Color(0xFFff00c8)
private val VwCyan = Color(0xFF00fff0)

/**
 * Vaporwave (portrait) — a Y2K diagonal pink→cyan→purple gradient washed with scanlines, an italic
 * serif title with chromatic-aberration glitch, a perspective horizon grid, a thin progress line and
 * three glassy transport buttons.
 */
@Composable
fun VaporwaveSkin(scope: SkinScope) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(VwPink, VwBlue, VwPurple),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                ),
            ),
    ) {
        // Faint horizontal scanlines overlaying the whole screen.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gap = 4f
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = Color.Black.copy(alpha = 0.10f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
                y += gap
            }
        }

        TinyTime(
            time = "9:41",
            color = Color.White,
            modifier = Modifier.statusBarsPadding().align(Alignment.TopEnd).padding(20.dp),
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            GlitchTitle(scope.title)

            Spacer(Modifier.height(10.dp))
            Text(
                scope.artist.uppercase(),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(26.dp))
            PerspectiveGrid()

            Spacer(Modifier.height(26.dp))
            VaporProgress(scope)

            Spacer(Modifier.height(28.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                GlassButton(Icons.Rounded.SkipPrevious, "Previous", scope.onPrev)
                GlassButton(
                    if (scope.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (scope.isPlaying) "Pause" else "Play",
                    scope.onPlayPause,
                )
                GlassButton(Icons.Rounded.SkipNext, "Next", scope.onNext)
            }
        }
    }
}

@Composable
private fun GlitchTitle(title: String) {
    val base = TextStyle(
        fontFamily = FontFamily.Serif,
        fontStyle = FontStyle.Italic,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Box(contentAlignment = Alignment.Center) {
        // Cyan + magenta offset ghosts for the chromatic-aberration look.
        Text(
            title, color = VwCyan, style = base, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.offsetBy(-3f, 0f),
        )
        Text(
            title, color = VwMagenta, style = base, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.offsetBy(3f, 0f),
        )
        Text(
            title,
            color = Color.White,
            style = base.copy(shadow = Shadow(color = VwMagenta, offset = Offset(3f, 3f))),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun Modifier.offsetBy(x: Float, y: Float): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.place(x.toInt(), y.toInt())
        }
    },
)

@Composable
private fun PerspectiveGrid() {
    Canvas(modifier = Modifier.fillMaxWidth().height(70.dp)) {
        val w = size.width
        val h = size.height
        val vp = Offset(w / 2f, -h * 0.35f) // vanishing point above the band
        val lineColor = Color.White.copy(alpha = 0.35f)
        // Vertical fanning lines from the bottom edge toward the vanishing point.
        val cols = 9
        for (i in 0..cols) {
            val x = w * (i.toFloat() / cols)
            drawLine(lineColor, Offset(x, h), vp, strokeWidth = 1.5f)
        }
        // A couple of horizontal lines suggesting depth.
        for (frac in listOf(0.4f, 0.68f, 0.88f)) {
            val y = h * frac
            drawLine(Color.White.copy(alpha = 0.18f), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }
        // Bottom horizon border line.
        drawLine(Color.White.copy(alpha = 0.7f), Offset(0f, h), Offset(w, h), strokeWidth = 2.5f)
    }
}

@Composable
private fun VaporProgress(scope: SkinScope) {
    val fraction by remember(scope) { derivedStateOf { scope.progressFraction() } }
    val duration = scope.durationMs.coerceAtLeast(1L)
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.25f))
                .pointerInput(duration) {
                    detectTapGestures { offset ->
                        scope.onSeek((offset.x / size.width * duration).toLong())
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(scope.position()), color = Color.White, fontSize = 10.sp)
            Text(formatTime(scope.durationMs), color = Color.White, fontSize = 10.sp)
        }
    }
}

@Composable
private fun GlassButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.20f))
            .border(width = 1.dp, color = Color.White, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Icon(icon, contentDescription = desc, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}
