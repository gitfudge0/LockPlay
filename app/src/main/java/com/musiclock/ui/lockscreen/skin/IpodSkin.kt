package com.musiclock.ui.lockscreen.skin

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LcdInk = Color(0xFF1C3A2A)
private val WheelLabel = Color(0xFF555555)

/**
 * Classic iPod (portrait) — a silver body with a green LCD panel (now-playing header, album cover,
 * title/artist, thin progress bar) above a tactile click wheel whose regions drive transport.
 */
@Composable
fun IpodSkin(scope: SkinScope) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFD8DBE0), Color(0xFFB0B4BC)))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // ---- Green LCD panel ----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFCFE8D8), Color(0xFFBCD8C6))))
                    .border(2.dp, Color(0xFF88AAAA), RoundedCornerShape(10.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("▶ Now Playing", color = LcdInk.copy(alpha = 0.8f), fontSize = 10.sp)
                    TinyTime("9:41", color = LcdInk.copy(alpha = 0.8f))
                }

                Spacer(Modifier.height(12.dp))
                SkinAlbumArt(
                    scope = scope,
                    shape = RoundedCornerShape(4.dp),
                    fallback = Brush.linearGradient(listOf(Color(0xFF2C4A38), Color(0xFF12241A))),
                    modifier = Modifier.size(96.dp),
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    scope.title, color = LcdInk, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    scope.artist, color = LcdInk.copy(alpha = 0.75f), fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(12.dp))
                LcdProgress(scope)
            }

            Spacer(Modifier.height(40.dp))

            // ---- Click wheel ----
            ClickWheel(scope)
        }
    }
}

@Composable
private fun LcdProgress(scope: SkinScope) {
    val fraction by remember(scope) {
        androidx.compose.runtime.derivedStateOf { scope.progressFraction() }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White.copy(alpha = 0.35f))
            .border(1.dp, LcdInk, RoundedCornerShape(3.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(LcdInk),
        )
    }
}

@Composable
private fun ClickWheel(scope: SkinScope) {
    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFFE8EAEE), Color(0xFFCFD2D8)))),
        contentAlignment = Alignment.Center,
    ) {
        // Tappable hit regions (placed so they don't overlap the center button).
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(width = 80.dp, height = 44.dp)
                .clickable(onClick = scope.onPrev),
            contentAlignment = Alignment.Center,
        ) {
            Text("MENU", color = WheelLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(width = 44.dp, height = 80.dp)
                .clickable(onClick = scope.onPrev),
            contentAlignment = Alignment.Center,
        ) {
            Text("◀◀", color = WheelLabel, fontSize = 14.sp)
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(width = 44.dp, height = 80.dp)
                .clickable(onClick = scope.onNext),
            contentAlignment = Alignment.Center,
        ) {
            Text("▶▶", color = WheelLabel, fontSize = 14.sp)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(width = 80.dp, height = 44.dp)
                .clickable(onClick = scope.onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            Text("▶ ❙❙", color = WheelLabel, fontSize = 13.sp)
        }
        // Center button.
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFFDFE2E7), Color(0xFFC3C7CE))))
                .clickable(onClick = scope.onPlayPause),
        )
    }
}
