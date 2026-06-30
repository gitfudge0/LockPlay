package com.musiclock.ui.lockscreen.skin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Turntable (portrait, variant B) — a warm vinyl-deck lockscreen: a spinning 230dp vinyl disc with
 * circular album art at its centre, a decorative tonearm, bottom-aligned track meta, device volume
 * slider, and a pill-based transport row. Colours are resolved locally from a two-palette light/dark
 * set; AppTheme / MaterialTheme tokens are intentionally ignored.
 *
 * Dark palette:  bg gradient #5C5230 → #2A2620 → #12100C, ink #EFE9D8.
 * Light palette: bg gradient #D8CFA8 → #B8AD84 → #8F876A, ink #2A2418.
 * Pill tint: #0006 (dark) / #0002 (light).
 */
@Composable
fun TurntableSkin(scope: SkinScope) {
    val dark = isSystemInDarkTheme()

    val bgColors = if (dark) {
        listOf(Color(0xFF5C5230), Color(0xFF2A2620), Color(0xFF12100C))
    } else {
        listOf(Color(0xFFD8CFA8), Color(0xFFB8AD84), Color(0xFF8F876A))
    }
    val bgStops = listOf(0f, 0.55f, 1f)

    val ink = if (dark) Color(0xFFEFE9D8) else Color(0xFF2A2418)
    val pillTint = if (dark) Color(0x33000000) else Color(0x22000000)
    val mutedInk = ink.copy(alpha = 0.70f)

    val clock = rememberClockText()
    val (vol, setVol) = rememberDeviceVolume()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = bgStops.zip(bgColors).map { (s, c) -> s to c }.toTypedArray(),
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Status row: clock left
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = clock,
                    color = ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Deck — fills the remaining vertical slack
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                VinylDisc(scope = scope)
                ToneArm(
                    ink = ink,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }

            Spacer(Modifier.height(20.dp))

            // Track meta — left aligned
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = scope.title,
                    color = ink,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = scope.artist,
                    color = mutedInk,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(20.dp))

            // Volume row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = null,
                    tint = mutedInk,
                    modifier = Modifier.size(18.dp),
                )
                Slider(
                    value = vol,
                    onValueChange = setVol,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = ink,
                        activeTrackColor = ink,
                        inactiveTrackColor = ink.copy(alpha = 0.25f),
                    ),
                )
            }

            Spacer(Modifier.height(12.dp))

            // Controls: square prev | wide play/pause pill | square next
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Prev pill (square)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(pillTint)
                        .clickable(onClick = scope.onPrev),
                ) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = ink,
                        modifier = Modifier.size(24.dp),
                    )
                }

                // Play/Pause wide pill
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(pillTint)
                        .clickable(onClick = scope.onPlayPause),
                ) {
                    Text(
                        text = if (scope.isPlaying) "PAUSE" else "PLAY",
                        color = ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )
                }

                // Next pill (square)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(pillTint)
                        .clickable(onClick = scope.onNext),
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = ink,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

/** 230dp vinyl disc with radial-groove texture and circular album art label in the centre. */
@Composable
private fun VinylDisc(scope: SkinScope) {
    val spin = rememberSpin(scope.isPlaying, 8000)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(230.dp),
    ) {
        // Disc drawn on canvas so it rotates as one unit
        Canvas(modifier = Modifier.size(230.dp).rotate(spin)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val outer = size.minDimension / 2f

            // Black vinyl base
            drawCircle(Color(0xFF080808), outer, c)

            // Groove rings
            var r = outer * 0.97f
            while (r > outer * 0.35f) {
                drawCircle(Color(0xFF1E1E1E), r, c, style = Stroke(width = 1.4f))
                r -= 4.5f
            }

            // Warm center label (radial gradient)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFBFAD7A), Color(0xFF6B5A30)),
                    center = c,
                    radius = outer * 0.33f,
                ),
                radius = outer * 0.33f,
                center = c,
            )

            // Spindle hole
            drawCircle(Color(0xFF0D0B08), outer * 0.04f, c)
        }

        // Album art as circular label — on top of the disc, rotates with it
        SkinAlbumArt(
            scope = scope,
            shape = CircleShape,
            modifier = Modifier
                .size(120.dp)
                .rotate(spin),
        )
    }
}

/** Decorative static tonearm — a thin rounded bar angled from the top-right corner. */
@Composable
private fun ToneArm(ink: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(90.dp)) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 4.dp)
                .rotate(-30f)
                .width(6.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(ink.copy(alpha = 0.85f), ink.copy(alpha = 0.40f))
                    )
                ),
        )
    }
}
