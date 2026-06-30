package com.musiclock.ui.lockscreen.skin

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Wood = Color(0xFF1C1410)
private val Gold = Color(0xFFD4923A)
private val Cream = Color(0xFFF0D9B5)
private val Brown = Color(0xFF9C7C52)

/**
 * Turntable (landscape) — a spinning vinyl record with a pickup arm on the left, track info on the
 * right. The record turns only while playing.
 */
@Composable
fun TurntableSkin(scope: SkinScope) {
    Row(
        modifier = Modifier.fillMaxSize().background(Wood).padding(horizontal = 30.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.fillMaxHeight().padding(vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Record(playing = scope.isPlaying)
            PickupArm(modifier = Modifier.align(Alignment.TopEnd))
        }

        Spacer(Modifier.width(26.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text("N O W   S P I N N I N G", color = Brown, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(scope.title, color = Cream, fontSize = 30.sp, fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(scope.artist, color = Brown, fontSize = 15.sp, fontFamily = FontFamily.Serif,
                maxLines = 1, overflow = TextOverflow.Ellipsis)

            Spacer(Modifier.height(22.dp))
            GoldProgress(scope)

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(30.dp), verticalAlignment = Alignment.CenterVertically) {
                ArmIcon(Icons.Rounded.SkipPrevious, "Previous", scope.onPrev)
                ArmIcon(
                    if (scope.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (scope.isPlaying) "Pause" else "Play", scope.onPlayPause,
                )
                ArmIcon(Icons.Rounded.SkipNext, "Next", scope.onNext)
            }
        }
    }
}

@Composable
private fun Record(playing: Boolean) {
    val angle = rememberSpin(playing, 5000)
    Canvas(modifier = Modifier.size(230.dp).rotate(angle)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val outer = size.minDimension / 2f
        drawCircle(Color(0xFF0D0A08), outer, c)
        // Grooves
        var r = outer * 0.96f
        while (r > outer * 0.36f) {
            drawCircle(Color(0xFF222222), r, c, style = Stroke(width = 1.2f))
            r -= 5f
        }
        // Gold center label + spindle
        drawCircle(
            brush = Brush.radialGradient(listOf(Gold, Color(0xFF7A4F1D)), center = c, radius = outer * 0.34f),
            radius = outer * 0.34f, center = c,
        )
        drawCircle(Wood, outer * 0.04f, c)
    }
}

/** A static angled pickup arm reaching onto the record. */
@Composable
private fun PickupArm(modifier: Modifier) {
    Box(modifier = modifier.size(150.dp)) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 6.dp, end = 4.dp)
                .rotate(26f)
                .width(8.dp)
                .height(130.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFCFCFCF), Color(0xFF888888)))),
        )
    }
}

@Composable
private fun GoldProgress(scope: SkinScope) {
    val fraction by androidx.compose.runtime.remember(scope) {
        androidx.compose.runtime.derivedStateOf { scope.progressFraction() }
    }
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.13f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Gold),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(scope.position()), color = Brown, fontSize = 11.sp)
            Text(formatTime(scope.durationMs), color = Brown, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ArmIcon(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Icon(
        icon, contentDescription = desc, tint = Gold,
        modifier = Modifier.size(30.dp).clip(CircleShape).clickable(onClick = onClick),
    )
}
