# HTML mock → Compose conversion

**Role of the mock.** An HTML mock is the spec for layout, spacing, typography, copy, colors, and every visible state — it is not code to port. Convert intent, not DOM. Mocks are working artifacts and are never committed (repo precedent: design mocks were removed in commit 6aefa9c); keep them in a local `mocks/` dir or the scratchpad.

## Authoring rules for mocks

So conversion stays mechanical:

- Design at 1x on a 412px-wide phone frame; then 1 CSS px = 1 dp, no math.
- One labeled frame per UI state (default, loading, error, empty, permission-denied) — a state with no frame will not get built.
- All magic values in CSS custom properties at the top of the file (`--gap`, `--radius`, `--accent`, font sizes); these become the Kotlin constants.
- No JS-driven layout; flexbox/grid only.

## Translation table

- flex row / flex column → `Row` / `Column`; `gap` → `Arrangement.spacedBy(n.dp)`; `justify-content`/`align-items` → `Arrangement`/`Alignment`.
- CSS grid → `Row`+`Column` composition first; `LazyVerticalGrid` only for real collections.
- absolute positioning / stacking → `Box` with `Alignment`; `z-index` → child order.
- `border-radius` → `RoundedCornerShape`; `box-shadow` → `shadow()` elevation (approximate, don't chase pixel parity on shadows).
- font sizes px → sp 1:1; weights map to `FontWeight`.
- CSS transitions/animations → `animate*AsState` / `AnimatedVisibility`; note duration + easing from the mock.
- Colors: if the screen is a skin in `ui/lockscreen/skin/`, colors are hardcoded in that skin file (skins never read `design/Tokens.kt`); otherwise they go through existing design tokens.

## Structure rules

Defer to existing conventions, just cross-reference: state per mock frame becomes an `@Immutable data class` + sealed/enum where states are exclusive; any branching logic goes in a Compose-free `*Logic.kt` with a matching test (see `architecture.md`, `testing.md`); files placed per `structure.md`.

## Fidelity check

Definition of done for a conversion: open the mock in a browser at 412px width beside the app running on a real device via `./run.sh`; walk every labeled frame and match it. `./gradlew check` green. Pixel-perfect shadows/fonts are out of scope; layout, spacing, copy, and state coverage are not.
