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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

private val AmpText = Color(0xFFE8D8B8)
private val AmpDim = Color(0xFFA89878)
private val AmpInset = Color(0xFF1A1612)
private val NeedleRed = Color(0xFFFF5D3B)

/**
 * Hi-Fi VU (landscape) — a warm vintage amplifier face: two analog VU meter panels with swinging red
 * needles (out of phase while playing, resting at the left when paused), three indicator knobs, and a
 * faint thin progress line under the title.
 */
@Composable
fun HiFiVuSkin(scope: SkinScope) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF2B2520), Color(0xFF15110D))))
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        TinyTime(
            time = formatTime(scope.position()),
            color = AmpDim,
            modifier = Modifier.align(Alignment.TopEnd),
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Two VU meter panels side by side.
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                VuPanel("L", scope.isPlaying, 1400, Modifier.weight(1f).fillMaxHeight())
                VuPanel("R", scope.isPlaying, 1900, Modifier.weight(1f).fillMaxHeight())
            }

            Spacer(Modifier.height(14.dp))

            // Title + transport on the left, three knobs on the right.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        scope.title, color = AmpText, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        scope.artist, color = AmpDim, fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TransportIcon(Icons.Rounded.SkipPrevious, "Previous", scope.onPrev)
                        TransportIcon(
                            if (scope.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            if (scope.isPlaying) "Pause" else "Play", scope.onPlayPause,
                        )
                        TransportIcon(Icons.Rounded.SkipNext, "Next", scope.onNext)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Knob(); Knob(); Knob()
                }
            }

            Spacer(Modifier.height(10.dp))

            // Thin progress line.
            val fraction = scope.progressFraction()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AmpText.copy(alpha = 0.12f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(NeedleRed),
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(scope.position()), color = AmpDim, fontSize = 10.sp)
                Text(formatTime(scope.durationMs), color = AmpDim, fontSize = 10.sp)
            }
        }
    }
}

/** One analog VU meter: dark inset, an arc scale, and a red needle pivoting from the bottom-center. */
@Composable
private fun VuPanel(label: String, playing: Boolean, periodMillis: Int, modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "vu$label")
    val swing by transition.animateFloat(
        initialValue = -34f,
        targetValue = 34f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "swing$label",
    )
    // Rest near the left when paused.
    val angle = if (playing) swing else -30f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AmpInset)
            .padding(10.dp),
    ) {
        Text(label, color = AmpDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Pivot at bottom-center, needle length sized to the panel.
            val pivot = Offset(w / 2f, h * 0.96f)
            val len = h * 0.82f
            val arcRadius = len * 0.92f

            // Thin arc scale across the top, centered on the pivot.
            val arcTopLeft = Offset(pivot.x - arcRadius, pivot.y - arcRadius)
            val arcSize = Size(arcRadius * 2f, arcRadius * 2f)
            // -34..+34 around vertical maps to sweep centered at top (270° in canvas terms).
            drawArc(
                color = AmpDim,
                startAngle = 270f - 40f,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = 2f, cap = StrokeCap.Round),
            )

            // Red needle: angle measured from straight up, positive swings to the right.
            val rad = Math.toRadians((angle - 90f).toDouble())
            val tip = Offset(
                pivot.x + (len * cos(rad)).toFloat(),
                pivot.y + (len * sin(rad)).toFloat(),
            )
            drawLine(NeedleRed, pivot, tip, strokeWidth = 3f, cap = StrokeCap.Round)
            drawCircle(NeedleRed, radius = 4f, center = pivot)
        }
    }
}

/** A small amplifier knob: radial dark gradient with an indicator tick. */
@Composable
private fun Knob() {
    Canvas(modifier = Modifier.size(30.dp)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF4A443C), Color(0xFF241F1A)),
                center = c,
                radius = r,
            ),
            radius = r,
            center = c,
        )
        // Indicator tick pointing up.
        drawLine(
            AmpText,
            start = Offset(c.x, c.y - r * 0.2f),
            end = Offset(c.x, c.y - r * 0.85f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun TransportIcon(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Icon(
        icon, contentDescription = desc, tint = AmpText,
        modifier = Modifier.size(30.dp).clip(CircleShape).clickable(onClick = onClick),
    )
}
