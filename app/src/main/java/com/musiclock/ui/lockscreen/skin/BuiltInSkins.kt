package com.musiclock.ui.lockscreen.skin

import com.musiclock.ui.lockscreen.skin.SkinOrientation.LANDSCAPE
import com.musiclock.ui.lockscreen.skin.SkinOrientation.PORTRAIT

/**
 * The catalogue of player looks, in picker order. Each entry binds a stable id + display name +
 * forced orientation to a self-contained skin composable (one per file under this package).
 *
 * To add a design: write `@Composable fun FooSkin(scope: SkinScope)` and add a [PlayerSkin] here.
 */

// --- Forced landscape: device-shaped players that read best rotated ---
val TurntableSkinSpec = PlayerSkin("turntable", "Turntable", LANDSCAPE) { TurntableSkin(it) }
val ReelToReelSkinSpec = PlayerSkin("reel", "Reel-to-Reel", LANDSCAPE) { ReelToReelSkin(it) }
val HiFiVuSkinSpec = PlayerSkin("hifi", "Hi-Fi VU", LANDSCAPE) { HiFiVuSkin(it) }
val CinematicSkinSpec = PlayerSkin("cinematic", "Cinematic", LANDSCAPE) { CinematicSkin(it) }
val DjDeckSkinSpec = PlayerSkin("djdeck", "DJ Deck", LANDSCAPE) { DjDeckSkin(it) }
val BoomboxSkinSpec = PlayerSkin("boombox", "Boombox", LANDSCAPE) { BoomboxSkin(it) }
val CassetteSkinSpec = PlayerSkin("cassette", "Cassette", LANDSCAPE) { CassetteSkin(it) }

// --- Portrait: player-hero layouts ---
val CardSkinSpec = PlayerSkin("card", "Card", PORTRAIT) { CardSkin(it) }
val GlassNeonSkinSpec = PlayerSkin("glassneon", "Glass Neon", PORTRAIT) { GlassNeonSkin(it) }
val IpodSkinSpec = PlayerSkin("ipod", "iPod", PORTRAIT) { IpodSkin(it) }
val DiscmanSkinSpec = PlayerSkin("discman", "Discman", PORTRAIT) { DiscmanSkin(it) }
val BrutalistSkinSpec = PlayerSkin("brutalist", "Brutalist", PORTRAIT) { BrutalistSkin(it) }
val AuroraSkinSpec = PlayerSkin("aurora", "Aurora", PORTRAIT) { AuroraSkin(it) }
val MinimalSkinSpec = PlayerSkin("minimal", "Minimal", PORTRAIT) { MinimalSkin(it) }
val WinampSkinSpec = PlayerSkin("winamp", "Winamp", PORTRAIT) { WinampSkin(it) }
val VaporwaveSkinSpec = PlayerSkin("vaporwave", "Vaporwave", PORTRAIT) { VaporwaveSkin(it) }
val TerminalSkinSpec = PlayerSkin("terminal", "Terminal", PORTRAIT) { TerminalSkin(it) }
val WalkmanSkinSpec = PlayerSkin("walkman", "Walkman", PORTRAIT) { WalkmanSkin(it) }
val JukeboxSkinSpec = PlayerSkin("jukebox", "Jukebox", PORTRAIT) { JukeboxSkin(it) }
val NeumorphicSkinSpec = PlayerSkin("neumorphic", "Neumorphic", PORTRAIT) { NeumorphicSkin(it) }

/** Source of truth for selectable skins. Order is the order shown in the picker. */
val BuiltInSkins: List<PlayerSkin> = listOf(
    CardSkinSpec,
    GlassNeonSkinSpec, AuroraSkinSpec, MinimalSkinSpec, DiscmanSkinSpec, IpodSkinSpec,
    BrutalistSkinSpec, VaporwaveSkinSpec, TerminalSkinSpec, WinampSkinSpec, WalkmanSkinSpec,
    JukeboxSkinSpec, NeumorphicSkinSpec,
    CassetteSkinSpec, TurntableSkinSpec, ReelToReelSkinSpec, HiFiVuSkinSpec, CinematicSkinSpec,
    DjDeckSkinSpec, BoomboxSkinSpec,
)

val DefaultSkin: PlayerSkin = CardSkinSpec

fun skinById(id: String?): PlayerSkin = BuiltInSkins.firstOrNull { it.id == id } ?: DefaultSkin
