package com.lockplay.ui.lockscreen.skin

import com.lockplay.ui.lockscreen.skin.SkinOrientation.PORTRAIT

/**
 * The catalogue of player looks, in picker order. Each entry binds a stable id + display name +
 * forced orientation to a self-contained skin composable (one per file under this package).
 *
 * To add a design: write `@Composable fun FooSkin(scope: SkinScope)` and add a [PlayerSkin] here.
 */

val CardSkinSpec = PlayerSkin("card", "Card", PORTRAIT) { CardSkin(it) }
val TurntableSkinSpec = PlayerSkin("turntable", "Turntable", PORTRAIT) { TurntableSkin(it) }
val GlassSkinSpec = PlayerSkin("glass", "Glass", PORTRAIT) { GlassSkin(it) }
val EditorialSkinSpec = PlayerSkin("editorial", "Editorial", PORTRAIT) { EditorialSkin(it) }

// Landscape tape, two separate facts: registered PORTRAIT because the DEVICE never turns, and
// contentRotation = 90f because the skin draws its own content sideways. Overlays above the skin
// (the lyrics sheet) read contentRotation so they stay aligned with the tape.
val CassetteSkinSpec = PlayerSkin("cassette", "Cassette", PORTRAIT, contentRotation = 90f) { CassetteSkin(it) }

/** Source of truth for selectable skins. Order is the order shown in the picker. */
val BuiltInSkins: List<PlayerSkin> =
    listOf(CardSkinSpec, TurntableSkinSpec, GlassSkinSpec, EditorialSkinSpec, CassetteSkinSpec)

val DefaultSkin: PlayerSkin = CardSkinSpec

fun skinById(id: String?): PlayerSkin = BuiltInSkins.firstOrNull { it.id == id } ?: DefaultSkin
