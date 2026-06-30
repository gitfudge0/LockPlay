package com.musiclock.ui.lockscreen.skin

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF0c0c10)
private val LcdGreen = Color(0xFF1aff5a)
private val LcdBorder = Color(0xFF00aa33)
private val PanelBorder = Color(0xFF555555)
private val LabelText = Color(0xFFcfcfff)
private val BlueText = Color(0xFF3399ff)
private val ButtonBg = Color(0xFF2a2a38)

/**
 * Winamp (portrait) — a centered classic media-player chassis: gradient title bar, a black LCD
 * showing the big monospace time plus a scrolling-style track line, a 16-band spectrum visualizer,
 * a row of small flat transport buttons, and a thin seek line. Monospace throughout.
 */
@Composable
fun WinampSkin(scope: SkinScope) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF3a3a4a), Color(0xFF23232e))))
                .border(2.dp, PanelBorder, RoundedCornerShape(4.dp))
                .padding(2.dp),
        ) {
            // Title bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF5a5a8a), Color(0xFF23233a))))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "MUSICLOCK 2.95", color = LabelText, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 1,
                )
                Text("_ □ ✕", color = LabelText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(Modifier.height(8.dp))

            // LCD area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF000000))
                    .border(1.dp, LcdBorder, RoundedCornerShape(2.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                val timeText by remember(scope) {
                    derivedStateOf { formatTime(scope.position()) }
                }
                Text(
                    timeText, color = LcdGreen, fontSize = 30.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "►► " + scope.title + " - " + scope.artist, color = LcdGreen, fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "192kbps stereo", color = BlueText, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace, maxLines = 1,
                )
            }

            Spacer(Modifier.height(8.dp))

            // Spectrum visualizer
            EqualizerBars(
                bars = 16,
                color = LcdGreen,
                playing = scope.isPlaying,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF000000))
                    .border(1.dp, LcdBorder, RoundedCornerShape(2.dp))
                    .padding(4.dp),
            )

            Spacer(Modifier.height(8.dp))

            // Transport buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WinampButton(Icons.Rounded.SkipPrevious, "Previous", scope.onPrev, Modifier.weight(1f))
                WinampButton(
                    if (scope.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (scope.isPlaying) "Pause" else "Play",
                    scope.onPlayPause,
                    Modifier.weight(1f),
                )
                WinampButton(Icons.Rounded.SkipNext, "Next", scope.onNext, Modifier.weight(1f))
            }

            Spacer(Modifier.height(10.dp))

            // Thin seek line
            SeekLine(scope)
        }

        TinyTime(
            time = formatTime(scope.position()),
            color = LcdGreen.copy(alpha = 0.18f),
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        )
    }
}

@Composable
private fun WinampButton(icon: ImageVector, desc: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(ButtonBg)
            .border(1.dp, PanelBorder, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick),
    ) {
        Icon(icon, contentDescription = desc, tint = LabelText, modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun SeekLine(scope: SkinScope) {
    val fraction by remember(scope) { derivedStateOf { scope.progressFraction() } }
    val duration = scope.durationMs.coerceAtLeast(1L)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xFF000000))
            .border(1.dp, PanelBorder, RoundedCornerShape(2.dp))
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    scope.onSeek((offset.x / size.width * duration).toLong())
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(LcdGreen),
        )
    }
}
