package com.lockplay.ui.lockscreen.skin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Turntable (portrait, variant B) — a warm vinyl-deck lockscreen: a spinning 230dp vinyl disc with
 * circular album art at its centre, a decorative tonearm, bottom-aligned track meta, and a
 * pill-based transport row. Colours are resolved locally from a two-palette light/dark set;
 * AppTheme / MaterialTheme tokens are intentionally ignored.
 *
 * Dark palette:  bg gradient #5C5230 → #2A2620 → #12100C, ink #EFE9D8.
 * Light palette: bg gradient #D8CFA8 → #B8AD84 → #8F876A, ink #2A2418.
 * Pills are dark chips (#0006) with light content in both themes, so they stay legible on the tan deck.
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
    // Dark chips with light content in BOTH themes — translucent-dark on the tan light deck
    // stayed invisible, so the transport reads as floating dark buttons regardless of mode.
    val pillTint = Color(0x66000000)
    val pillContent = Color(0xFFEFE9D8)
    val mutedInk = ink.copy(alpha = 0.70f)

    val clock = rememberClockText()

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
                .navigationBarsPadding()
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
                        tint = pillContent,
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
                        .clickable(onClick = scope.onPlayPause)
                        .semantics {
                            role = Role.Button
                            contentDescription = if (scope.isPlaying) "Pause" else "Play"
                        },
                ) {
                    Text(
                        text = if (scope.isPlaying) "PAUSE" else "PLAY",
                        color = pillContent,
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
                        tint = pillContent,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Full-width vinyl disc with radial-groove texture, a circular album art label in the centre, and
 *  the tonearm resting half inside the disc. */
@Composable
private fun VinylDisc(scope: SkinScope) {
    // Lambda-read spin: the angle is sampled in the graphicsLayer draw block, so the per-frame
    // rotation invalidates only the layer — VinylDisc itself does not recompose each frame.
    val spin = rememberSpinAngle(scope.isPlaying, 8000)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
    ) {
        // Disc drawn on canvas so it rotates as one unit
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = spin() }) {
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
                .fillMaxSize(0.52f)
                .graphicsLayer { rotationZ = spin() },
        )

        // Tonearm overlaid on the disc square so its headshell rests half inside the disc
        ToneArm(modifier = Modifier.fillMaxSize())
    }
}

/**
 * Decorative tonearm sized to the disc square it overlays: the pivot sits just off the disc's
 * top-right rim, a counterweight stub pokes back to the corner, and the arm sweeps down-left so the
 * headshell rests about half a radius inside the disc. Metallic greys, fixed across themes (it's
 * hardware, not chrome), so it reads as an arm rather than a floating stick.
 */
@Composable
private fun ToneArm(modifier: Modifier = Modifier) {
    val metal = Color(0xFFCBCBD2)
    val metalDark = Color(0xFF45454C)
    Canvas(modifier = modifier) {
        // Fractions of the disc square: pivot near the top-right rim, tip half-way to the centre.
        val pivot = Offset(size.width * 0.86f, size.height * 0.14f)
        val tip = Offset(size.width * 0.34f, size.height * 0.34f)
        val weight = Offset(size.width * 0.97f, size.height * 0.03f)

        // Base plate under the pivot.
        drawCircle(metalDark, radius = 17.dp.toPx(), center = pivot)
        // Counterweight stub poking back toward the corner.
        drawLine(metalDark, pivot, weight, strokeWidth = 9.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(metalDark, radius = 10.dp.toPx(), center = weight)
        // The arm tube reaching in toward the record.
        drawLine(metal, pivot, tip, strokeWidth = 7.dp.toPx(), cap = StrokeCap.Round)
        // Pivot cap and headshell.
        drawCircle(metal, radius = 7.dp.toPx(), center = pivot)
        drawCircle(metalDark, radius = 6.dp.toPx(), center = tip)
    }
}
