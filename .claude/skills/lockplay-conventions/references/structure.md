# Structure

Where code lives, and what a new file is allowed to touch.

## S1 — One module, packages by concern

Everything is in the single `:app` Gradle module under `com.lockplay`. Split packages by **concern**, not by layer-type: `model/`, `media/`, `trigger/`, `design/`, `ui/<feature>/`. There is no `data/`, `domain/`, `presentation/` triad and there will not be one. A concern-shaped tree means a feature is one directory you can read top to bottom; a layer-shaped tree scatters it across three.

## S2 — Import direction is one-way

- `ui/` may import `media/`, `model/`, `design/`.
- `media/` may import `model/`.
- `model/` imports nothing app-local — `NowPlaying.kt` depends only on the platform and Kotlin.

Never import upward. If `media/` finds itself wanting something from `ui/`, the thing belongs in `model/` or should be passed in as a parameter. The direction is what keeps `model/` and `media/` testable on the JVM without dragging Compose in.

## S3 — `design/` never imports from `ui/`

`design/` is the token and theme layer (`Tokens.kt`, `ThemeSpec.kt`, `BuiltInThemes.kt`, `ThemeController.kt`, `components/`). It is consumed by `ui/`, never the reverse. A single import from `design/` into `ui/` makes the design system a dependency of one screen and it stops being swappable.

## S4 — No `utils/` package, ever

A helper starts as a private function **in the file of its one caller**. When a genuine second caller appears, move it to the narrowest package both callers already share — not to a grab-bag. `utils/` has no owner, no boundary, and no import direction, so it becomes the place rules go to die.

## S5 — A new skin is one file plus one line

Adding a player look means:

1. One self-contained file in `ui/lockscreen/skin/` (see `CardSkin.kt`, `CassetteSkin.kt`, `TurntableSkin.kt`).
2. One entry added to `BuiltInSkins.kt`.

Nothing else changes. If your skin requires editing `LockscreenScreen.kt`, `SkinScope`, or another skin, stop — either the need belongs in `SkinScope` as a deliberate contract change (`PlayerSkin.kt`), or the skin is reaching for something it should draw itself.

## S6 — Skins never read `design/` tokens

No skin file imports `com.lockplay.design.*`. A skin picks its own colors, type, and chrome inline. Shared visual helpers across skins are limited to the small primitives already in `SkinPrimitives.kt`. Rationale and the rejected alternative: `rationale.md` §4.

## S7 — Non-skin UI reads tokens only via `AppTheme`

Every composable outside `ui/lockscreen/skin/` reads color, type, shape, spacing, and motion through the `AppTheme` accessor in `Tokens.kt`. Never `MaterialTheme.*`, never a hardcoded `Color(0xFF…)` or bare `16.dp` in a screen. Swapping one `ThemeSpec` must restyle the whole app; a single literal breaks that guarantee silently.

## S8 — Debug-only surfaces live in `src/debug/`

Anything that must not ship — preview harnesses, inspection screens — goes in `app/src/debug/`, as `SkinPreviewActivity.kt` does. Not behind a `BuildConfig.DEBUG` check in main source. Source-set separation means the code physically is not in the release APK.

## S9 — Soft ceiling of 400 lines per file

Past ~400 lines, split the file **by section into sibling files in the same package** (`FooHeader.kt`, `FooControls.kt`), keeping the same package and visibility. This is mechanical division, not abstraction: it does not require the two-caller test in A14 and must not be used to justify introducing an interface. See `rationale.md` §8.

## S10 — Compile-time constants are PascalCase

`const val` and other compile-time constants are named in **PascalCase** (`const val CardWidthFraction`), not SCREAMING_SNAKE. The example to follow is `GalleryDefaults` in `ui/gallery/PlayerGalleryScreen.kt` (`CardWidthFraction`, `FrameAspectRatio`). This is why `ktlint_standard_property-naming` is disabled in `app/build.gradle.kts`. Known non-conforming exception: `STEP_WELCOME` and `FIRST_PERM_STEP` in `OnboardingLogic.kt` still use SCREAMING_SNAKE — leave them alone, do **not** rename them. Rationale and the rejected alternative: `rationale.md` §9.

## Documentation of what you add

### D1 — KDoc explains *why*

Public types and non-obvious public functions get KDoc that states the collaborators, the invariant, or the design call — not a restatement of the signature. The standard to match is the header on `MediaRepository.kt` ("The `MediaListenerService` writes state…; a plain object is the lazy-correct choice — there is exactly one media state per process") and on `Tokens.kt`. A KDoc that only says "returns the current now-playing state" is worse than none.

### D2 — Contract files carry file-level KDoc on how to extend them

Files that other people extend — `PlayerSkin.kt`, `Tokens.kt`, `OnboardingLogic.kt` — carry a file- or type-level KDoc stating **how to extend them correctly**, including what not to do. `PlayerSkin.kt` already tells the reader to register in `BuiltInSkins` and that a skin deliberately does not read theme tokens. If you change the extension procedure for one of these files, update its KDoc in the same commit.
