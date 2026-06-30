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
import androidx.compose.foundation.layout.statusBarsPadding
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

private val DiscManText2 = Color(0xFFB9C2CF)
private val DiscManTransport = Color(0xFFCFD8E3)
private val DiscManBezel = Color(0xFF1A1F26)

private val CdRainbow = listOf(
    Color(0xFFFF6EC4),
    Color(0xFF7873F5),
    Color(0xFF42E695),
    Color(0xFFFFD86F),
    Color(0xFFFF6EC4),
)

/**
 * Discman (portrait) — a big circular CD-player lid with a spinning rainbow disc, a center hub hole,
 * and a diagonal glare; below it the title, artist, a thin progress line, and transport controls.
 */
@Composable
fun DiscmanSkin(scope: SkinScope) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF5A6675), Color(0xFF222831))),
            ),
    ) {
        TinyTime(
            time = formatTime(scope.position()),
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.statusBarsPadding().align(Alignment.TopEnd).padding(20.dp),
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CdLid(playing = scope.isPlaying)

            Spacer(Modifier.height(34.dp))
            Text(
                scope.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                scope.artist, color = DiscManText2, fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(24.dp))
            DiscProgress(scope)

            Spacer(Modifier.height(26.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                DiscIcon(Icons.Rounded.SkipPrevious, "Previous", scope.onPrev)
                DiscIcon(
                    if (scope.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (scope.isPlaying) "Pause" else "Play", scope.onPlayPause,
                )
                DiscIcon(Icons.Rounded.SkipNext, "Next", scope.onNext)
            }
        }
    }
}

@Composable
private fun CdLid(playing: Boolean) {
    val angle = rememberSpin(playing, 3000)
    Box(modifier = Modifier.size(250.dp), contentAlignment = Alignment.Center) {
        // Dark bezel/ring around the disc.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = DiscManBezel, radius = size.minDimension / 2f)
        }
        // Spinning rainbow CD.
        Canvas(
            modifier = Modifier
                .size(205.dp)
                .rotate(angle),
        ) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val outer = size.minDimension / 2f
            drawCircle(
                brush = Brush.sweepGradient(CdRainbow, center = c),
                radius = outer,
                center = c,
            )
            // Center hub: a small dark hole with a lighter surrounding ring.
            drawCircle(Color(0xFF2A313B), radius = outer * 0.22f, center = c)
            drawCircle(Color(0xFFD8DEE7), radius = outer * 0.14f, center = c)
            drawCircle(Color(0xFF14181E), radius = outer * 0.08f, center = c)
        }
        // Diagonal glare (does not spin) across the top-left.
        Canvas(modifier = Modifier.size(205.dp)) {
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.32f),
                        Color.White.copy(alpha = 0.0f),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.85f, size.height * 0.85f),
                ),
                radius = size.minDimension / 2f,
            )
        }
    }
}

@Composable
private fun DiscProgress(scope: SkinScope) {
    val fraction by remember(scope) { derivedStateOf { scope.progressFraction() } }
    val duration = scope.durationMs.coerceAtLeast(1L)
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.18f))
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
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(scope.position()), color = DiscManText2, fontSize = 11.sp)
            Text(formatTime(scope.durationMs), color = DiscManText2, fontSize = 11.sp)
        }
    }
}

@Composable
private fun DiscIcon(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Icon(
        icon, contentDescription = desc, tint = DiscManTransport,
        modifier = Modifier.size(34.dp).clip(CircleShape).clickable(onClick = onClick),
    )
}
