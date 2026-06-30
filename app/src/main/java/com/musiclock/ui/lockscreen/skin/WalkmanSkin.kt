package com.musiclock.ui.lockscreen.skin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val WmOrange = Color(0xFFFF8A00)
private val WmOrangeDark = Color(0xFFE35A00)
private val WmBorder = Color(0xFFB54400)
private val WmPanel = Color(0xFF2A2C30)
private val WmPanelBorder = Color(0xFF111111)
private val WmBg = Color(0xFF1B1D22)

/**
 * Walkman (portrait) — a Sony-cassette-player-style orange device body with a clear window holding two
 * spinning reels flanking the track label, a tape-strip progress fill, and chunky transport buttons.
 */
@Composable
fun WalkmanSkin(scope: SkinScope) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WmBg)
            .padding(horizontal = 24.dp),
    ) {
        TinyTime(
            time = formatTime(scope.position()),
            color = WmOrange,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 18.dp, end = 4.dp),
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Orange device body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(WmOrange, WmOrangeDark)))
                    .border(2.dp, WmBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 18.dp),
            ) {
                Text(
                    "WALKMAN",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 3.sp,
                )

                Spacer(Modifier.height(14.dp))

                // Clear window panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(WmPanel)
                        .border(2.dp, WmPanelBorder, RoundedCornerShape(10.dp))
                        .padding(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Reel(playing = scope.isPlaying)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                scope.title, color = Color.White, fontSize = 13.sp,
                                fontWeight = FontWeight.Bold, maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                scope.artist, color = WmOrange, fontSize = 10.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Reel(playing = scope.isPlaying)
                    }

                    Spacer(Modifier.height(12.dp))

                    // Tape strip progress
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF111111)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(scope.progressFraction())
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(WmOrange, WmOrangeDark)),
                                ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Transport controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WalkmanButton(Icons.Rounded.SkipPrevious, "Previous", scope.onPrev)
                WalkmanButton(
                    if (scope.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (scope.isPlaying) "Pause" else "Play",
                    scope.onPlayPause,
                )
                WalkmanButton(Icons.Rounded.SkipNext, "Next", scope.onNext)
            }
        }
    }
}

/** A small cassette reel: a dark hub ringed with dashed spokes, spinning while [playing]. */
@Composable
private fun Reel(playing: Boolean) {
    val angle = rememberSpin(playing, 3000)
    Canvas(modifier = Modifier.size(38.dp).rotate(angle)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val outer = size.minDimension / 2f
        drawCircle(Color(0xFF15161A), outer, c)
        drawCircle(Color(0xFF3A3C42), outer * 0.78f, c)
        // Dashed spokes radiating from the hub
        val spokes = 8
        for (i in 0 until spokes) {
            val a = Math.toRadians((360.0 / spokes) * i)
            val cos = Math.cos(a).toFloat()
            val sin = Math.sin(a).toFloat()
            val inner = outer * 0.36f
            val end = outer * 0.7f
            drawLine(
                color = WmOrange,
                start = Offset(c.x + inner * cos, c.y + inner * sin),
                end = Offset(c.x + end * cos, c.y + end * sin),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(Color(0xFF15161A), outer * 0.32f, c)
        drawCircle(WmOrange, outer * 0.12f, c)
    }
}

@Composable
private fun WalkmanButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(WmPanel)
            .border(1.dp, WmPanelBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Icon(icon, contentDescription = desc, tint = WmOrange, modifier = Modifier.size(26.dp))
    }
}
