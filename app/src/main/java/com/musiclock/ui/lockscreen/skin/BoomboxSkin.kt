package com.musiclock.ui.lockscreen.skin

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DeckPanel = Color(0xFF14161A)
private val ButtonBg = Color(0xFF23262B)
private val Amber = Color(0xFFFFD24A)
private val Orange = Color(0xFFFF5D3B)
private val ArtistGray = Color(0xFF999999)

/**
 * Boombox (landscape) — a brushed graphite shell with a thumping woofer on each flank and a lit deck
 * in the middle: NOW PLAYING tag, amber title, a progress hairline, an equalizer, and transport keys.
 */
@Composable
fun BoomboxSkin(scope: SkinScope) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF3A3F47), Color(0xFF1D2024))))
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        TinyTime(
            time = formatTime(scope.position()),
            color = Orange,
            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Woofer(playing = scope.isPlaying)

            // Center deck
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DeckPanel)
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "N O W   P L A Y I N G",
                    color = Orange,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    scope.title,
                    color = Amber,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                ProgressLine(scope)
                Spacer(Modifier.height(6.dp))
                Text(
                    scope.artist,
                    color = ArtistGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(14.dp))
                EqualizerBars(
                    bars = 10,
                    color = Orange,
                    playing = scope.isPlaying,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                )

                Spacer(Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    DeckButton(Icons.Rounded.SkipPrevious, "Previous", scope.onPrev)
                    DeckButton(
                        if (scope.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (scope.isPlaying) "Pause" else "Play",
                        scope.onPlayPause,
                    )
                    DeckButton(Icons.Rounded.SkipNext, "Next", scope.onNext)
                }
            }

            Woofer(playing = scope.isPlaying)
        }
    }
}

@Composable
private fun ProgressLine(scope: SkinScope) {
    val fraction by remember(scope) { derivedStateOf { scope.progressFraction() } }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(Color.White.copy(alpha = 0.12f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(2.dp)
                .background(Orange),
        )
    }
}

/** A speaker woofer: concentric cone rings that bounce gently while [playing]. */
@Composable
private fun Woofer(playing: Boolean) {
    val transition = rememberInfiniteTransition(label = "woofer")
    val scale by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bounce",
    )
    Canvas(
        modifier = Modifier
            .size(150.dp)
            .graphicsLayer {
                val s = if (playing) scale else 1f
                scaleX = s
                scaleY = s
            },
    ) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val outer = size.minDimension / 2f
        drawCircle(Color(0xFF14161A), outer, c)
        drawCircle(Color(0xFF2E3238), outer * 0.86f, c)
        drawCircle(Color(0xFF1A1C20), outer * 0.68f, c)
        drawCircle(Color(0xFF2E3238), outer * 0.50f, c)
        drawCircle(Color(0xFF1A1C20), outer * 0.34f, c)
        // center dome
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF3A3F47), Color(0xFF14161A)),
                center = c,
                radius = outer * 0.20f,
            ),
            radius = outer * 0.18f,
            center = c,
        )
    }
}

@Composable
private fun DeckButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 52.dp, height = 38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ButtonBg)
            .clickable(onClick = onClick),
    ) {
        Icon(icon, contentDescription = desc, tint = Amber, modifier = Modifier.size(22.dp))
    }
}
