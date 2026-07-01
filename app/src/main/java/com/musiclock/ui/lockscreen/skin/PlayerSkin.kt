package com.musiclock.ui.lockscreen.skin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.musiclock.model.NowPlaying

/**
 * Which way the lockscreen activity is locked while a skin is showing. Device-shaped skins
 * (turntable, cassette, reel-to-reel, VU meters…) read best in [LANDSCAPE]; the activity sets
 * `requestedOrientation` to match when the skin becomes active. See [com.musiclock.ui.lockscreen.LockscreenActivity].
 */
enum class SkinOrientation { PORTRAIT, LANDSCAPE }

/**
 * Everything a [PlayerSkin] needs to render and drive playback. The player IS the screen, so a skin
 * gets the full surface and decides its own layout, colors and chrome — it deliberately does NOT read
 * the app's theme tokens (each skin is its own self-contained visual identity).
 *
 * [position] is the auto-ticking elapsed position in ms (hoisted in [com.musiclock.ui.lockscreen.LockscreenScreen]
 * so it advances once a second between real session updates); read it lazily so only the progress
 * element recomposes each tick.
 */
class SkinScope(
    val state: NowPlaying,
    val position: () -> Long,
    val onSeek: (Long) -> Unit,
    val onPrev: () -> Unit,
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
) {
    val title: String get() = state.title.ifEmpty { "Unknown title" }
    val artist: String get() = state.artist.ifEmpty { "Unknown artist" }
    val album: String get() = state.album
    val isPlaying: Boolean get() = state.isPlaying
    val durationMs: Long get() = state.durationMs
}

/**
 * A complete, named player look. To add a new design, write a self-contained `@Composable (SkinScope)`
 * and register a [PlayerSkin] for it in [BuiltInSkins] — no other code changes. Components are not
 * shared across skins beyond the small primitives in `SkinPrimitives.kt`.
 */
@Immutable
class PlayerSkin(
    val id: String,
    val displayName: String,
    val orientation: SkinOrientation,
    val content: @Composable (SkinScope) -> Unit,
)
