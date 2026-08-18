package com.lockplay.ui.lockscreen.skin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cassette (mixtape) — a black analog cassette that IS the whole screen. It's a landscape design,
 * but instead of forcing the device into landscape (which would rotate the system UI) the skin stays
 * PORTRAIT and rotates its own content 90°: the landscape tape is laid out at swapped dimensions via
 * [requiredSize] and turned with a graphicsLayer, so it fills a portrait screen sideways. Compose maps
 * pointer input through the rotation, so the transport still responds where it's drawn.
 *
 * Every size is derived from a [scale] factor (the short side ÷ a reference height), so the tape fills
 * both the full lockscreen and the small scaled-down gallery preview without clipping. Now-playing is
 * written on the label sticker (title in a marker/cursive face, artist beneath); the transport, tape
 * counter and "Normal Bias / A|60" flavour are printed on the black shell. Fixed palette — a cassette
 * is always black; AppTheme tokens are intentionally ignored.
 */
private val Shell = Color(0xFF0C0B0A)
private val Cream = Color(0xFFE9E6DF)
private val Red = Color(0xFFE5402A)
private val LabelBg = Color(0xFFF5F3EE)
private val Ink = Color(0xFF161514)
private val ArtistInk = Color(0xFF3A3833)
private val ReelSlit = Color(0xFF8A867D) // grey teeth on the cream reels
private val TapeBox = Color(0xFF060605) // black housing around the two reels
private val TapeWindowBg = Color(0xFF33302B) // the lit opening in the housing
private val Tape = Color(0xFF120E0A) // the wound magnetic tape (two half-circles)

/** Short-side height (dp) the raw sizes below are tuned for; actual sizes scale from this. */
private const val RefHeight = 400f

@Composable
fun CassetteSkin(scope: SkinScope) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        val portraitW = maxWidth
        val portraitH = maxHeight
        // Lay out the landscape tape at swapped dimensions, then rotate 90° to fill the portrait
        // screen. requiredSize ignores the (portrait) parent constraints so width can exceed it.
        Box(
            modifier = Modifier
                .requiredSize(width = portraitH, height = portraitW)
                .graphicsLayer { rotationZ = 90f },
        ) {
            CassetteBody(scope)
        }
    }
}

@Composable
private fun CassetteBody(scope: SkinScope) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Shell)) {
        // maxHeight here is the tape's short side; scale every dimension off it so the whole tape
        // fits whether it's a full screen (~410dp) or a gallery card preview (~240dp).
        val s = maxHeight.value / RefHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = (14 * s).dp, end = (14 * s).dp, top = (14 * s).dp, bottom = (8 * s).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The label's red stripe doubles as the seekbar, so there's no printed counter below.
            Label(scope, s, modifier = Modifier.fillMaxWidth().weight(0.34f))

            // Black shell laid out in flow (no overlays): reels + transport float in the top slack,
            // the "A|60" grade and the lip holes sit pinned at the bottom.
            Column(
                modifier = Modifier.fillMaxWidth().weight(0.66f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height((26 * s).dp)) // guaranteed gap so the reels sit off the label
                Spacer(Modifier.weight(1f))
                Deck(scope, s)
                Spacer(Modifier.height((16 * s).dp))
                Transport(scope, s)
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    scope.lyricsPill?.let { pill -> pill() }
                }

                Grade(s, modifier = Modifier.align(Alignment.End))
                Spacer(Modifier.height((6 * s).dp))
                Holes(s)
            }
        }
    }
}

/** White now-playing sticker: red stripe, printed label chrome, and the track written on it. */
@Composable
private fun Label(scope: SkinScope, s: Float, modifier: Modifier = Modifier) {
    val fraction = scope.progressFraction()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape((8 * s).dp))
            .background(LabelBg),
    ) {
        // Red stripe is the seekbar: a dim track with a red fill that tracks playback position.
        Box(
            Modifier.fillMaxWidth().height((12 * s).dp).align(Alignment.TopCenter).background(Color(0x22161514)),
        ) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(Red))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = (18 * s).dp, start = (16 * s).dp, end = (16 * s).dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("ACOUSTIC CASSETTE", color = Ink, fontSize = (10 * s).sp, fontFamily = FontFamily.Monospace)
            Text("N.R.[  ] ☐IN ☐OUT", color = Ink, fontSize = (10 * s).sp, fontFamily = FontFamily.Monospace)
        }

        // Album-art thumbnail where the "B" side chip used to be.
        SkinAlbumArt(
            scope = scope,
            shape = RoundedCornerShape((3 * s).dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = (16 * s).dp, top = (32 * s).dp)
                .size((30 * s).dp),
        )

        // Handwritten track title + artist
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = (20 * s).dp, vertical = (6 * s).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                scope.title,
                color = Ink,
                fontSize = (30 * s).sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                scope.artist,
                color = ArtistInk,
                fontSize = (14 * s).sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = (4 * s).dp),
            )
        }
    }
}

/** Left grille · black tape housing (two reels + tape window) · right grille. */
@Composable
private fun Deck(scope: SkinScope, s: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Grille(s)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape((14 * s).dp))
                .background(TapeBox)
                .padding(horizontal = (20 * s).dp, vertical = (14 * s).dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((40 * s).dp),
            ) {
                Reel(scope.isPlaying, s)
                TapeWindow(s)
                Reel(scope.isPlaying, s)
            }
        }
        Grille(s)
    }
}

@Composable
private fun Grille(s: Float) {
    Canvas(modifier = Modifier.width((72 * s).dp).height((100 * s).dp)) {
        val gap = (4 * s).dp.toPx()
        val stroke = (2 * s).dp.toPx()
        var y = 0f
        while (y <= size.height) {
            drawLine(Color(0x1CFFFFFF), Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
            y += gap
        }
    }
}

/** A reel: white background disc with a grey hub + grey teeth strip; the white gaps read as holes. */
@Composable
private fun Reel(playing: Boolean, s: Float) {
    val spin = rememberSpinAngle(playing, 3500)
    Canvas(modifier = Modifier.size((100 * s).dp).graphicsLayer { rotationZ = spin() }) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f
        drawCircle(ReelSlit, r, c) // grey background
        // White hub, and 6 fat white teeth radiating from it: the grey gaps between them are the "holes".
        drawCircle(Cream, r * 0.55f, c)
        val teeth = 6
        val inner = r * 0.5f
        val outer = r * 0.94f // reach further out so the grey rim is ~half as thick
        for (i in 0 until teeth) {
            val a = (i * (360f / teeth)) * (PI / 180f).toFloat()
            drawLine(
                Cream,
                Offset(c.x + cos(a) * inner, c.y + sin(a) * inner),
                Offset(c.x + cos(a) * outer, c.y + sin(a) * outer),
                strokeWidth = r * 0.46f, // wider white teeth → thinner grey gaps
                cap = StrokeCap.Butt,
            )
        }
        drawCircle(Color(0x33000000), r, c, style = Stroke(width = 1.5f))
    }
}

/** Rectangular cutout in the housing showing the two tape spools as facing half-circles. */
@Composable
private fun TapeWindow(s: Float) {
    Canvas(modifier = Modifier.size(width = (150 * s).dp, height = (92 * s).dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(TapeWindowBg, cornerRadius = CornerRadius((5 * s).dp.toPx()))
        // Tape spools: circles twice the reel radius (reel Ø = 100*s dp), centred just outside each
        // edge so the window crops them into two big arcs bulging toward the middle.
        val tapeR = 2f * (50f * s).dp.toPx()
        val off = tapeR - w * 0.45f // apex lands at ~0.45·width, leaving a small centre gap
        clipRect(0f, 0f, w, h) {
            drawCircle(Tape, tapeR, Offset(-off, h / 2f))
            drawCircle(Tape, tapeR, Offset(w + off, h / 2f))
        }
    }
}

@Composable
private fun Transport(scope: SkinScope, s: Float) {
    Row(
        horizontalArrangement = Arrangement.spacedBy((34 * s).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.SkipPrevious,
            contentDescription = "Previous",
            tint = Cream,
            modifier = Modifier.size((34 * s).dp).clickable(onClick = scope.onPrev),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size((58 * s).dp)
                .clip(CircleShape)
                .background(Cream)
                .clickable(onClick = scope.onPlayPause)
                .semantics {
                    role = Role.Button
                    contentDescription = if (scope.isPlaying) "Pause" else "Play"
                },
        ) {
            Icon(
                if (scope.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Shell,
                modifier = Modifier.size((28 * s).dp),
            )
        }
        Icon(
            Icons.Rounded.SkipNext,
            contentDescription = "Next",
            tint = Cream,
            modifier = Modifier.size((34 * s).dp).clickable(onClick = scope.onNext),
        )
    }
}

/** Red "A | 60" tape-grade box. */
@Composable
private fun Grade(s: Float, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clip(RoundedCornerShape((3 * s).dp)).background(Red),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "A",
            color = Color.White,
            fontSize = (17 * s).sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = (8 * s).dp, end = (6 * s).dp, top = (4 * s).dp, bottom = (4 * s).dp),
        )
        Box(Modifier.width((1 * s).dp).height((20 * s).dp).background(Color(0x8CFFFFFF)))
        Text(
            "60",
            color = Color.White,
            fontSize = (17 * s).sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = (6 * s).dp, vertical = (4 * s).dp),
        )
    }
}

/** Recessed lip plate with drilled holes. */
@Composable
private fun Holes(s: Float, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape((10 * s).dp))
            .background(Color(0x52000000))
            .padding(horizontal = (16 * s).dp, vertical = (7 * s).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((16 * s).dp),
    ) {
        Hole(s)
        Hole(s)
        Spacer(Modifier.width((48 * s).dp))
        Hole(s)
        Hole(s)
    }
}

@Composable
private fun Hole(s: Float) {
    Box(
        modifier = Modifier
            .size(width = (9 * s).dp, height = (14 * s).dp)
            .clip(CircleShape)
            .background(Color.Black),
    )
}
