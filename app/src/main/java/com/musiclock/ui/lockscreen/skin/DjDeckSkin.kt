package com.musiclock.ui.lockscreen.skin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DjBg = Color(0xFF0C0D12)
private val DjCyan = Color(0xFF00E5FF)
private val DjPink = Color(0xFFFF3D81)
private val DjMuted = Color(0xFF7A7A9C)

/**
 * DJ Deck (landscape) — two turntable platters flanking a central mixer column. Each platter spins
 * while playing (the right one counter-rotates), and the center holds title/artist, a cyan→pink
 * crossfader, an equalizer, and the transport.
 */
@Composable
fun DjDeckSkin(scope: SkinScope) {
    Box(modifier = Modifier.fillMaxSize().background(DjBg)) {
        TinyTime(
            time = "9:41",
            color = DjMuted,
            modifier = Modifier.align(Alignment.TopEnd).padding(18.dp),
        )

        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Platter(playing = scope.isPlaying, centerColor = DjCyan, reverse = false)

            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    scope.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    scope.artist, color = DjMuted, fontSize = 11.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(16.dp))
                Crossfader(scope)

                Spacer(Modifier.height(16.dp))
                EqualizerBars(
                    bars = 10,
                    color = DjCyan,
                    playing = scope.isPlaying,
                    modifier = Modifier.fillMaxWidth().height(34.dp),
                )

                Spacer(Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DjIcon(Icons.Rounded.SkipPrevious, "Previous", 26.dp, Color.White, scope.onPrev)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(DjCyan)
                            .clickable(onClick = scope.onPlayPause),
                    ) {
                        Icon(
                            if (scope.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (scope.isPlaying) "Pause" else "Play",
                            tint = DjBg, modifier = Modifier.size(28.dp),
                        )
                    }
                    DjIcon(Icons.Rounded.SkipNext, "Next", 26.dp, Color.White, scope.onNext)
                }
            }

            Platter(playing = scope.isPlaying, centerColor = DjPink, reverse = true)
        }
    }
}

/**
 * A turntable platter: a radial-gradient disc with a thin rim and a soft glowing center dot. Rotates
 * while [playing]; [reverse] negates the spin so the two decks turn against each other.
 */
@Composable
private fun Platter(playing: Boolean, centerColor: Color, reverse: Boolean) {
    val raw = rememberSpin(playing, 4000)
    val angle = if (reverse) -raw else raw
    Canvas(modifier = Modifier.size(150.dp).rotate(angle)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f
        // Disc body
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF23242E), Color(0xFF15161D)),
                center = c,
                radius = r,
            ),
            radius = r,
            center = c,
        )
        // Thin rim
        drawCircle(
            color = Color(0xFF2A2C38),
            radius = r - 1.5f,
            center = c,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
        )
        // Glowing center dot — soft halo under a solid core
        drawCircle(centerColor.copy(alpha = 0.30f), r * 0.20f, c)
        drawCircle(centerColor.copy(alpha = 0.55f), r * 0.12f, c)
        drawCircle(centerColor, r * 0.06f, c)
    }
}

@Composable
private fun Crossfader(scope: SkinScope) {
    val fraction by remember(scope) { derivedStateOf { scope.progressFraction() } }
    val duration = scope.durationMs.coerceAtLeast(1L)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    scope.onSeek((offset.x / size.width * duration).toLong())
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        // Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF1C1C2E)),
        )
        // Played portion follows progressFraction()
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.horizontalGradient(listOf(DjCyan, DjPink))),
        )
        // Gradient handle parked at ~58%
        Box(modifier = Modifier.fillMaxWidth(0.58f)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(width = 16.dp, height = 18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.verticalGradient(listOf(DjCyan, DjPink))),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TinyTime(time = formatTime(scope.position()), color = DjMuted)
        TinyTime(time = formatTime(scope.durationMs), color = DjMuted)
    }
}

@Composable
private fun DjIcon(
    icon: ImageVector,
    desc: String,
    size: androidx.compose.ui.unit.Dp,
    tint: Color,
    onClick: () -> Unit,
) {
    Icon(
        icon, contentDescription = desc, tint = tint,
        modifier = Modifier.size(size).clip(CircleShape).clickable(onClick = onClick),
    )
}
