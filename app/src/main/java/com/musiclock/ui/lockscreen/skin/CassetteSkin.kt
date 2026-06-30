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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TapeRed = Color(0xFFE8554E)
private val TapeRedDark = Color(0xFFC43D38)
private val TapeScrew = Color(0xFF7A201C)
private val WindowDark = Color(0xFF1A140E)

/**
 * Cassette (landscape) — a full-bleed mixtape: paper label, two tape hubs spinning at different
 * rates (the supply reel fuller and slower, like real playback), and the classic bottom mechanism.
 */
@Composable
fun CassetteSkin(scope: SkinScope) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFFF6B60), TapeRed, TapeRedDark, Color(0xFF9E2C28)),
                    radius = 1300f,
                ),
            )
            .padding(horizontal = 26.dp, vertical = 14.dp),
    ) {
        // Corner screws
        Screw(Modifier.align(Alignment.TopStart))
        Screw(Modifier.align(Alignment.TopEnd))
        Screw(Modifier.align(Alignment.BottomStart))
        Screw(Modifier.align(Alignment.BottomEnd))

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Paper sticker label
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFFDF6), Color(0xFFF3E9D4))))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    scope.title, color = Color(0xFF1A1A2A), fontSize = 22.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(scope.artist, color = TapeRedDark, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                    Text("SIDE A · TYPE II", color = Color(0xFF8A7A58), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(14.dp))

            // Dark window with the two spinning hubs
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .height(132.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF15110C), Color(0xFF241A12))))
                    .padding(horizontal = 26.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CassetteHub(playing = scope.isPlaying, periodMillis = 4000, fill = 0.62f)
                CassetteHub(playing = scope.isPlaying, periodMillis = 6500, fill = 0.42f)
            }

            Spacer(Modifier.height(16.dp))

            // Transport controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TapeButton(Icons.Rounded.SkipPrevious, "Previous", scope.onPrev)
                TapeButton(
                    if (scope.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (scope.isPlaying) "Pause" else "Play", scope.onPlayPause, primary = true,
                )
                TapeButton(Icons.Rounded.SkipNext, "Next", scope.onNext)
            }
        }

        TinyTime(
            time = formatTime(scope.position()),
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
        )
    }
}

@Composable
private fun Screw(modifier: Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFCF4A44), TapeScrew)),
            radius = size.minDimension / 2f,
        )
        // slot
        drawLine(Color(0xFF5A1714), Offset(size.width * 0.3f, size.height / 2f),
            Offset(size.width * 0.7f, size.height / 2f), strokeWidth = 2f)
    }
}

/**
 * One cassette reel: a dark wound-tape pancake whose radius reflects [fill], a toothed white sprocket
 * hub, and a dark core — the whole thing rotating while [playing] with the given [periodMillis].
 */
@Composable
private fun CassetteHub(playing: Boolean, periodMillis: Int, fill: Float) {
    val angle = rememberSpin(playing, periodMillis)
    Canvas(modifier = Modifier.size(96.dp).rotate(angle)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val outer = size.minDimension / 2f
        // Wound tape pancake — outer radius grows with fill (more tape = bigger spool).
        val pancake = outer * (0.62f + 0.38f * fill.coerceIn(0f, 1f))
        drawCircle(Color(0xFF352818), pancake, c)
        drawCircle(WindowDark, pancake * 0.78f, c) // inner edge of the wound tape
        // Toothed sprocket hub
        val hub = outer * 0.34f
        val teeth = 12
        for (i in 0 until teeth) {
            val a = Math.toRadians((360.0 / teeth) * i)
            val r = hub * 1.18f
            drawCircle(
                Color(0xFFE9E2D0),
                radius = hub * 0.16f,
                center = Offset(c.x + (r * Math.cos(a)).toFloat(), c.y + (r * Math.sin(a)).toFloat()),
            )
        }
        drawCircle(Color(0xFFB8AD92), hub, c)
        drawCircle(WindowDark, hub * 0.5f, c)
    }
}

@Composable
private fun TapeButton(icon: ImageVector, desc: String, onClick: () -> Unit, primary: Boolean = false) {
    val bg = if (primary) Color.White else Color.White.copy(alpha = 0.2f)
    val tint = if (primary) TapeRedDark else Color.White
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 36.dp, height = 28.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(bg)
            .clickable(onClick = onClick),
    ) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(18.dp))
    }
}
