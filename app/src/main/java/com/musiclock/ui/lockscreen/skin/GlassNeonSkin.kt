package com.musiclock.ui.lockscreen.skin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NeonPink = Color(0xFFFF4ECD)
private val NeonViolet = Color(0xFF6A5CFF)
private val NeonText2 = Color(0xFFB7A7E8)

/**
 * Glass Neon (portrait, default) — dominant rounded album art over a deep radial purple field with a
 * neon glow, big title, gradient progress, and a bright circular play button.
 */
@Composable
fun GlassNeonSkin(scope: SkinScope) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF3A1A6E), Color(0xFF0A0618)),
                    radius = 1400f,
                ),
            ),
    ) {
        TinyTime(
            time = "9:41",
            color = NeonText2.copy(alpha = 0.7f),
            modifier = Modifier.statusBarsPadding().align(Alignment.TopEnd).padding(20.dp),
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SkinAlbumArt(
                scope = scope,
                shape = RoundedCornerShape(30.dp),
                fallback = Brush.linearGradient(listOf(NeonPink, NeonViolet)),
                modifier = Modifier.fillMaxWidth(0.66f).aspectRatio(1f),
            )

            Spacer(Modifier.height(30.dp))
            Text(
                scope.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(5.dp))
            Text(scope.artist, color = NeonText2, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)

            Spacer(Modifier.height(26.dp))
            NeonProgress(scope)

            Spacer(Modifier.height(26.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(34.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                NeonIcon(Icons.Rounded.SkipPrevious, "Previous", 30.dp, Color.White, scope.onPrev)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = scope.onPlayPause),
                ) {
                    Icon(
                        if (scope.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (scope.isPlaying) "Pause" else "Play",
                        tint = Color(0xFF1A0D3D), modifier = Modifier.size(34.dp),
                    )
                }
                NeonIcon(Icons.Rounded.SkipNext, "Next", 30.dp, Color.White, scope.onNext)
            }
        }
    }
}

@Composable
private fun NeonProgress(scope: SkinScope) {
    val fraction by androidx.compose.runtime.remember(scope) {
        androidx.compose.runtime.derivedStateOf { scope.progressFraction() }
    }
    val duration = scope.durationMs.coerceAtLeast(1L)
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.White.copy(alpha = 0.13f))
                .pointerInput(duration) {
                    detectTapGestures { offset ->
                        scope.onSeek((offset.x / size.width * duration).toLong())
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(5.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Brush.horizontalGradient(listOf(NeonPink, NeonViolet))),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(scope.position()), color = NeonText2, fontSize = 11.sp)
            Text(formatTime(scope.durationMs), color = NeonText2, fontSize = 11.sp)
        }
    }
}

@Composable
private fun NeonIcon(icon: ImageVector, desc: String, size: androidx.compose.ui.unit.Dp, tint: Color, onClick: () -> Unit) {
    Icon(
        icon, contentDescription = desc, tint = tint,
        modifier = Modifier.size(size).clip(CircleShape).clickable(onClick = onClick),
    )
}
