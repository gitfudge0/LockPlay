package com.musiclock.ui.lockscreen.skin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DeckText = Color(0xFFD8C8A8)
private val DeckSubText = Color(0xFF9C8C6C)
private val DeckAccent = Color(0xFFD8A85D)
private val ReelRim = Color(0xFF6A604C)
private val ReelBody = Color(0xFF26221A)
private val ReelHub = Color(0xFF8A7C5C)

/**
 * Reel-to-Reel (landscape) — two large tape reels span the screen left and right, joined by a thin
 * tape line across their centers, spinning independently while playing. Track info and transport
 * sit centered below, with a faint clock top-right.
 */
@Composable
fun ReelToReelSkin(scope: SkinScope) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF3A352C), Color(0xFF1F1B15))),
            )
            .padding(horizontal = 28.dp, vertical = 16.dp),
    ) {
        // Reels + tape line across the top band
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Reel(playing = scope.isPlaying, periodMillis = 6000)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(ReelRim),
            )
            Reel(playing = scope.isPlaying, periodMillis = 6500)
        }

        // Track info + progress + transport, centered below
        Column(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                scope.title, color = DeckText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                scope.artist, color = DeckSubText, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DeckText.copy(alpha = 0.18f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(scope.progressFraction())
                        .height(2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(DeckAccent),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(scope.position()), color = DeckSubText, fontSize = 10.sp)
                Text(formatTime(scope.durationMs), color = DeckSubText, fontSize = 10.sp)
            }

            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DeckIcon(Icons.Rounded.SkipPrevious, "Previous", 30.dp, scope.onPrev)
                DeckIcon(
                    if (scope.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (scope.isPlaying) "Pause" else "Play", 40.dp, scope.onPlayPause,
                )
                DeckIcon(Icons.Rounded.SkipNext, "Next", 30.dp, scope.onNext)
            }
        }

        TinyTime(
            time = "9:41",
            color = DeckText.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
        )
    }
}

/**
 * One reel-to-reel spool: an outer rim ring, dark body, a center hub, and 3 dashed spokes radiating
 * out — rotating while [playing] with the given [periodMillis].
 */
@Composable
private fun Reel(playing: Boolean, periodMillis: Int) {
    val angle = rememberSpin(playing, periodMillis)
    Canvas(modifier = Modifier.size(150.dp).rotate(angle)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val outer = size.minDimension / 2f
        // Dark body
        drawCircle(ReelBody, outer * 0.94f, c)
        // Outer rim ring
        drawCircle(ReelRim, outer, c, style = Stroke(width = outer * 0.08f))
        // 3 dashed spokes radiating from the hub to the rim
        val hub = outer * 0.26f
        val spokeStart = hub * 1.1f
        val spokeEnd = outer * 0.84f
        for (i in 0 until 3) {
            val a = Math.toRadians(120.0 * i)
            val dx = Math.cos(a).toFloat()
            val dy = Math.sin(a).toFloat()
            // dashed: walk segments along the spoke
            var t = spokeStart
            val dash = outer * 0.07f
            val gap = outer * 0.05f
            while (t < spokeEnd) {
                val t2 = (t + dash).coerceAtMost(spokeEnd)
                drawLine(
                    ReelRim,
                    Offset(c.x + dx * t, c.y + dy * t),
                    Offset(c.x + dx * t2, c.y + dy * t2),
                    strokeWidth = outer * 0.05f,
                    cap = StrokeCap.Round,
                )
                t = t2 + gap
            }
        }
        // Center hub
        drawCircle(ReelHub, hub, c)
        drawCircle(ReelBody, hub * 0.4f, c)
    }
}

@Composable
private fun DeckIcon(icon: ImageVector, desc: String, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Icon(
        icon, contentDescription = desc, tint = DeckAccent,
        modifier = Modifier.size(size).clip(CircleShape).clickable(onClick = onClick),
    )
}
