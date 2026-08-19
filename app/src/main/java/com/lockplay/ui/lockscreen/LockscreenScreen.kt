package com.lockplay.ui.lockscreen

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lockplay.design.AppTheme
import com.lockplay.lyrics.LyricsController
import com.lockplay.lyrics.LyricsRepository
import com.lockplay.lyrics.shouldConsumeHorizontal
import com.lockplay.lyrics.shouldShowCoachMark
import com.lockplay.lyrics.shouldShowHint
import com.lockplay.model.NowPlaying
import com.lockplay.ui.lockscreen.skin.PlayerSkin
import com.lockplay.ui.lockscreen.skin.SkinScope
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.hypot

/** Upward drag distance (px) past which a release triggers unlock. */
private const val UnlockThresholdPx = -180f

/** Horizontal travel (px) past which a released drag flips between player and lyrics. */
private const val FlipThresholdPx = 60f

// ponytail: the flip band is a screen-fraction approximation of where every built-in skin puts its
// artwork — replace with real bounds once SkinScope can report the art rect.
private const val ArtBandTopFraction = 0.12f
private const val ArtBandBottomFraction = 0.62f

private const val CoachMarkAutoDismissMs = 6_000L

private val MinTouchTarget = 48.dp

private val CoachMarkMaxWidth = 260.dp

/** Seed the ticker from the real sample extrapolated to now (see [NowPlaying.positionAt]). */
private fun seedPosition(state: NowPlaying): Long = state.positionAt(SystemClock.elapsedRealtime())

/**
 * The lockscreen surface. Hosts the cross-cutting behaviour every skin shares — an auto-ticking
 * playback position, swipe-up-to-unlock, and the horizontal flip to the lyrics overlay — then hands
 * the full surface to the selected [skin] to paint however it likes via a [SkinScope]. The skin owns
 * all layout, colors and chrome.
 *
 * [lyricsEnabled] / [lyricsHintSeen] come from [com.lockplay.lyrics.LyricsController]; when the
 * feature is off, the flip raises the one-time hint instead and reports it via [onLyricsHintSeen].
 */
@Composable
fun LockscreenScreen(
    skin: PlayerSkin,
    state: NowPlaying,
    onSeek: (Long) -> Unit,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onUnlock: () -> Unit,
    lyricsEnabled: Boolean,
    lyricsHintSeen: Boolean,
    onLyricsHintSeen: () -> Unit,
    coachMarkSeen: Boolean,
    onCoachMarkSeen: () -> Unit,
) {
    // --- Auto-ticking position (hoisted ticker; skins read it lazily so reads stay scoped) ---
    var displayedPosition by remember { mutableLongStateOf(seedPosition(state)) }
    LaunchedEffect(state.isPlaying, state.positionMs, state.positionUpdateTime) {
        displayedPosition = seedPosition(state)
        if (state.isPlaying) {
            val step = (1000f * state.playbackSpeed).toLong()
            while (true) {
                delay(1000)
                displayedPosition = (displayedPosition + step)
                    .coerceAtMost(if (state.durationMs > 0) state.durationMs else Long.MAX_VALUE)
            }
        }
    }

    // --- Swipe-to-unlock drag tracking ---
    val dragOffsetState = remember { mutableFloatStateOf(0f) }
    var dragOffset by dragOffsetState
    val offsetY by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = tween(150),
        label = "unlockDrag",
    )
    val dragModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onVerticalDrag = { _, dragAmount ->
                dragOffset = (dragOffset + dragAmount).coerceAtMost(0f)
            },
            onDragEnd = { if (dragOffset <= UnlockThresholdPx) onUnlock() else dragOffset = 0f },
            onDragCancel = { dragOffset = 0f },
        )
    }

    // --- Lyrics overlay state ---
    // rememberSaveable: a configuration change must not silently drop the user back to the player.
    var lyricsVisible by rememberSaveable { mutableStateOf(false) }
    var hintVisible by remember { mutableStateOf(false) }

    val lyrics by LyricsRepository.lyrics.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // ponytail: a local instance is fine — LyricsController's DataStore is keyed by Context, not
    // by instance, so this reads the same persisted flag as any other instance.
    val lyricsController = remember(context) { LyricsController(context.applicationContext) }
    val lyricsFolderUri by lyricsController.lyricsFolderUri.collectAsStateWithLifecycle(initialValue = "")
    LaunchedEffect(state.title, state.artist, lyricsVisible, lyricsEnabled, lyricsFolderUri) {
        LyricsRepository.clear()
        // Guarded here as well as inside the repository: the call site is where the decision to
        // send track metadata off-device is actually taken.
        if (lyricsVisible && lyricsEnabled) {
            LyricsRepository.request(
                state.title,
                state.artist,
                state.album,
                state.durationMs,
                lyricsEnabled,
                context,
                lyricsFolderUri,
            )
        }
    }

    // The lyric list owns vertical drag, so the root [detectVerticalDragGestures] never sees a swipe
    // that starts over the overlay — a raw pointerInput is not a nested-scroll participant, so the
    // list's leftover delta has nowhere to go. This connection is the only path by which unlock stays
    // reachable while lyrics are showing: it feeds overscroll straight into the same drag state.
    val unlockNestedScroll = remember(onUnlock) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // Upward (negative) leftover only; dragging down past the top must not undo an unlock drag.
                if (available.y >= 0f) return Offset.Zero
                val before = dragOffsetState.floatValue
                dragOffsetState.floatValue = (before + available.y).coerceAtMost(0f)
                return Offset(0f, dragOffsetState.floatValue - before)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (dragOffsetState.floatValue <= UnlockThresholdPx) {
                    onUnlock()
                } else {
                    dragOffsetState.floatValue = 0f
                }
                return Velocity.Zero
            }
        }
    }

    // A drag can be abandoned by the overlay disappearing (flip back, feature toggled off) rather
    // than by a release, and then no fling ever settles it — reset so the surface can't stay offset.
    LaunchedEffect(lyricsVisible, lyricsEnabled) {
        if (!lyricsVisible || !lyricsEnabled) dragOffsetState.floatValue = 0f
    }

    // The flip gesture rides the root Box next to the unlock detector rather than living in a
    // transparent layer above the skin: a node that handles pointer input is hit-tested first, so a
    // band across the middle of the screen starves skin controls under it (GlassSkin's tap-to-seek
    // scrubber sits inside it). The root already hosts vertical drag without stealing those taps.
    val flipModifier = Modifier.pointerInput(lyricsEnabled, lyricsHintSeen) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val height = size.height.toFloat()
            val y = down.position.y
            if (height <= 0f || y < height * ArtBandTopFraction || y > height * ArtBandBottomFraction) {
                return@awaitEachGesture
            }
            var totalX = 0f
            var totalY = 0f
            var decided = false
            var horizontal = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                if (!change.pressed) break
                val delta = change.positionChange()
                totalX += delta.x
                totalY += delta.y
                // One decision per gesture, taken only once the pointer has moved past slop: a first
                // frame that happens to be 1px sideways must never steal a swipe-to-unlock.
                if (!decided && hypot(totalX, totalY) > viewConfiguration.touchSlop) {
                    decided = true
                    horizontal = shouldConsumeHorizontal(totalX, totalY)
                }
                if (decided && !horizontal) return@awaitEachGesture
                if (decided && horizontal) change.consume()
            }
            if (horizontal && abs(totalX) > FlipThresholdPx) {
                if (lyricsEnabled) {
                    lyricsVisible = !lyricsVisible
                } else if (shouldShowHint(lyricsEnabled, lyricsHintSeen)) {
                    hintVisible = true
                    onLyricsHintSeen()
                }
            }
        }
    }

    // Local mirror so a dismissal takes effect this frame; the DataStore flow catches up after.
    var coachMarkDismissed by remember { mutableStateOf(false) }
    val dismissCoachMark = {
        if (!coachMarkDismissed) {
            coachMarkDismissed = true
            onCoachMarkSeen()
        }
    }

    val coachMarkVisible = shouldShowCoachMark(lyricsEnabled, coachMarkSeen || coachMarkDismissed, lyricsVisible)

    LaunchedEffect(lyricsVisible) {
        if (lyricsVisible) dismissCoachMark()
    }
    LaunchedEffect(coachMarkVisible) {
        if (coachMarkVisible) {
            delay(CoachMarkAutoDismissMs)
            dismissCoachMark()
        }
    }

    val scope = SkinScope(
        state = state,
        position = { displayedPosition },
        onSeek = onSeek,
        onPrev = onPrev,
        onPlayPause = onPlayPause,
        onNext = onNext,
        lyricsPill = if (!lyricsEnabled) {
            null
        } else {
            {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (coachMarkVisible) {
                        LyricsCoachMark(onDismiss = { dismissCoachMark() })
                    }
                    LyricsPill(
                        active = lyricsVisible,
                        onClick = { lyricsVisible = !lyricsVisible },
                    )
                }
            }
        },
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(dragModifier)
            // Nothing to flip to and nothing left to teach: install no touch handling at all.
            .then(if (lyricsEnabled || !lyricsHintSeen) flipModifier else Modifier)
            .graphicsLayer { translationY = offsetY },
    ) {
        skin.content(scope)

        if (lyricsVisible && lyricsEnabled) {
            // The sheet turns with the skin: skins that draw their own content rotated (Cassette)
            // get lyrics rotated the same way, never upright over sideways content. Sized like
            // CassetteSkin does it — a quarter turn needs swapped dimensions via requiredSize, or
            // the rotated sheet lands as a sideways letterbox instead of filling the screen.
            val quarterTurned = skin.contentRotation % 180f != 0f
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                LyricsOverlay(
                    state = state,
                    lyrics = lyrics,
                    onSeek = onSeek,
                    onClose = { lyricsVisible = false },
                    modifier = Modifier
                        .then(
                            if (quarterTurned) {
                                Modifier.requiredSize(width = maxHeight, height = maxWidth)
                            } else {
                                Modifier.fillMaxSize()
                            },
                        )
                        .nestedScroll(unlockNestedScroll)
                        .graphicsLayer { rotationZ = skin.contentRotation },
                )
            }
        }

        if (hintVisible) {
            LyricsHintCard(
                onDismiss = { hintVisible = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(AppTheme.spacing.screenPadding),
            )
        }
    }
}

/** The app-styled lyrics affordance handed to skins via [SkinScope.lyricsPill]. */
@Composable
private fun LyricsPill(
    active: Boolean,
    onClick: () -> Unit,
) {
    val background = if (active) AppTheme.colors.accentMuted else AppTheme.colors.fillGhost
    val content = if (active) AppTheme.colors.accentOnSurface else AppTheme.colors.textPrimary
    Row(
        modifier = Modifier
            .heightIn(min = MinTouchTarget)
            .background(background, AppTheme.shapes.pill)
            .then(
                if (active) {
                    Modifier
                } else {
                    Modifier.border(1.dp, AppTheme.colors.borderSubtle, AppTheme.shapes.pill)
                },
            )
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (active) Icons.Rounded.Close else Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(AppTheme.spacing.md),
        )
        Text(
            text = "Lyrics",
            style = AppTheme.typography.label,
            color = content,
        )
    }
}

/** One-time coach mark that rides above the pill, wherever the skin placed it. */
@Composable
private fun LyricsCoachMark(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = AppTheme.spacing.screenPadding, vertical = AppTheme.spacing.sm)
            .widthIn(max = CoachMarkMaxWidth)
            .background(AppTheme.colors.surfaceVariant, AppTheme.shapes.medium)
            .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) }
            .padding(AppTheme.spacing.md),
    ) {
        Text(
            text = "Tap for lyrics — or swipe the artwork",
            style = AppTheme.typography.body,
            color = AppTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

/** One-time discoverability card, raised by the flip gesture while the feature is still off. */
@Composable
private fun LyricsHintCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(AppTheme.colors.surfaceVariant, AppTheme.shapes.medium)
            .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) }
            .padding(AppTheme.spacing.md),
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("That swipe reveals lyrics") }
                append(" — turn on Lyrics in the LockPlay app to use it.")
            },
            style = AppTheme.typography.body,
            color = AppTheme.colors.textPrimary,
        )
    }
}
