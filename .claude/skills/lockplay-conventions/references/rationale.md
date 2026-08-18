# Rationale

A register of the decisions behind the rules: what was chosen, what was rejected, and why the winner won. Read this before proposing a change to a rule — most alternatives here were already considered.

## 1. CONFLICT RESOLVED — A2 (log at `Log.w`) vs X3 (no NowPlaying data in logs)

**The collision.** A2 requires logging platform failures at `Log.w` with the cause. X3 forbids NowPlaying data at `Log.i` or above. The catches in `MediaListenerService.kt` sit exactly where track metadata is in scope, so the natural log line ("failed while handling *Song X by Artist Y*") violates X3.

**Resolution.** Track metadata is **never** included in a log message at any level. Log the failure and, where a source needs identifying, the controller's **package name** only. `Log.d` with metadata is permitted in `src/debug/` sources only.

**Rejected alternative:** permit metadata at `Log.d` in main source. Rejected because a debug-level log in a shipped app still writes the user's listening history to logcat, where other tooling on the device can read it. The level is not the protection; absence is.

## 2. A9 — No DI framework

**Chosen:** process-wide state as a documented Kotlin `object` (`MediaRepository`).
**Rejected:** Hilt, and a hand-rolled DI container.
**Why:** there is exactly one media state per process, no second data source, and no backend. A container would exist to swap implementations that do not exist.
**Revisit deliberately if** a real backend or a second media source appears.

## 3. A3 — No `Result<T>`

**Chosen:** narrow catch, log, degrade.
**Rejected:** `Result<T>` / a sealed error type.
**Why:** every failure in this app is a platform permission revocation with exactly one sensible response — degrade. A type whose entire purpose is forcing callers to branch buys nothing when there is only one branch, and it would spread that ceremony through every call site.

## 4. S6 — Skins ignore design tokens

**Chosen:** each skin is a self-contained visual identity that draws its own colors and chrome.
**Rejected:** making skins theme-aware so themes and skins compose.
**Why:** a skin is a *whole look* (turntable, cassette, editorial), not a palette. Theming them would collapse a set of distinct designs into variants of one layout, which is the product this app is explicitly not.

## 5. T3 — No UI tests

**Chosen:** test the contract around composables (unique skin ids, default skin present, orientation declared, tokens resolve per theme).
**Rejected:** Compose UI tests and screenshot tests.
**Why:** they are slow, brittle against the constant visual churn of a skin-driven app, and there is no CI to run them on.
**Accepted cost:** layout and gesture regressions are caught by the device check in W6, not by an automated suite. **This is why W6 exists** and why it is non-negotiable.

## 6. W1 / W5 — No CI, no hooks

**Chosen:** direct commits to `main`; W4 (`./gradlew check` before commit) stated as a rule, not enforced.
**Rejected:** a pre-commit hook.
**Why:** the user's explicit call for a solo repo. A pre-commit hook gets `--no-verify`'d the first time it is inconvenient, at which point it provides ceremony instead of a guarantee. An unenforced rule that is actually followed beats an enforced one that is routinely bypassed.

## 7. D5 — No ADR directory, no `docs/`

**Chosen:** rationale lives in this file plus KDoc beside the code (D1/D2).
**Rejected:** an ADR directory.
**Why:** this register and the KDoc headers already carry the reasoning. A second location splits it, and the split copy is the one that goes stale.

## 8. A14 (two callers) vs S9 (400-line ceiling)

Splitting an oversized file into sibling files in the same package is **mechanical division, not abstraction**. It does not require two real callers, and it must not be used to justify introducing an interface, a base class, or a generic "engine" while you are in there. Recorded explicitly so the S9 ceiling is never cited as grounds for premature abstraction.

## 9. S10 — PascalCase compile-time constants

**Chosen:** `const val CardWidthFraction` — PascalCase constants, with `ktlint_standard_property-naming` disabled to allow it.
**Rejected:** renaming constants repo-wide to SCREAMING_SNAKE to satisfy the standard ktlint rule.
**Why:** that rename is a mechanical churn commit across files, for a convention the codebase already largely follows. These `const` values are read as *names* (`CardWidthFraction`, `FrameAspectRatio`), not as shouted markers.
**Note:** this rule is the reason `ktlint_standard_property-naming` is disabled. Anyone re-enabling that rule is overturning S10, not fixing a config oversight.

## 10. CONFLICT RESOLVED — Lyrics feature vs X3 ("never leaves the device")

**The collision.** The user wants a lyrics feature. Lyrics cannot be shown without fetching them from somewhere, and X3 said NowPlaying data never leaves the device — full stop. The feature cannot exist without sending *something* off-device.

**Resolution.** A narrow, named, opt-in carve-out: title, artist, album, and duration — and nothing else — may go to `lrclib.net`, and only while the user has explicitly turned the setting on; off means zero requests, enforced by `shouldFetch(enabled, title, artist)` in `LyricsMatch.kt`. A dialog explains what is sent and where before the toggle can be turned on. Fetched lyrics stay in memory only, same as the rest of NowPlaying data. The logging half of X3 is untouched — title, artist, album still never appear in a log message in `src/main/` at any level.

**Rejected alternatives:**

- **Local-only lyrics** — rejected because `MediaMetadata` has no standard lyrics key and `MediaListenerService` captures none, so the feature would show an empty panel for nearly every track. It is a non-feature, not a smaller feature.
- **On by default** — rejected; a carve-out from a privacy rule has to be a deliberate act by the user, not a default they discover later.
- **Caching lyrics to disk** — rejected; would violate X4 and X3's first bullet for a saving that doesn't matter at this scale.
- **Sending a stable device or user identifier for better matching** — rejected outright; matching stays fuzzy on title+artist+duration and the resulting occasional wrong match is the accepted cost.
- **Adding an HTTP client dependency (Retrofit/OkHttp/Ktor)** — rejected per X6; `HttpURLConnection` + `org.json` are on the platform and sufficient.

**Revisit deliberately if** LRCLIB starts requiring auth or an API key, or the request would ever need to carry more than these four fields.

## Rule dependencies

- **T2 depends on A13.** T2 ("every Compose-free logic file has a matching test") measures the files A13 creates. Reverse A13 and T2 has nothing left to apply to.
- **W6 depends on T3.** The real-device check exists *because* composables are deliberately untested. If UI tests were ever adopted, W6's justification changes.
- **X3 constrains A2.** The "log the cause" rule stops short of logging the data in scope. See §1.
- **X3's network carve-out is enforced by `shouldFetch` in `LyricsMatch.kt`.** The opt-in gate is not a UI convention — the function is the single point that decides whether a request happens at all. See §10.
- **D4 is the ledger for X1's known exception.** The `// ponytail:` marker on debug-key release signing in `app/build.gradle.kts` is the only record that this exception exists; deleting the marker deletes the tracking.
