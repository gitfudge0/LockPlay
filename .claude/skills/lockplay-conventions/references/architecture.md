# Architecture

Errors, boundaries, state, and when an abstraction is allowed to exist.

## Errors at the platform seam

### A1 — Catch the narrowest platform exception, at the seam

Every failure in this app is the Android platform refusing something. Catch the specific type, at the exact call that can throw it:

- `SecurityException` around `MediaSessionManager.addOnActiveSessionsChangedListener` / `getActiveSessions` when notification access has been revoked — see `MediaListenerService.kt`.
- `IllegalArgumentException` around `unregisterReceiver` for a receiver that was never registered — same file.

Do not wrap a whole method body. The narrower the catch, the more precisely the degraded path matches the actual failure.

### A2 — On catch: `Log.w` with the cause, then degrade

Log at `Log.w`, pass the exception as the final argument so the stack trace survives, and continue in a working lesser state rather than crashing:

```kotlin
Log.w(TAG, "getActiveSessions denied — notification access revoked?", e)
```

Degrading means the app keeps running with less: no sessions rather than no app. **Constraint from X3:** the log message carries the failure and, at most, the controller's package name. Never track metadata. See `security.md` and `rationale.md` §1.

### A3 — No `Result<T>`, no custom exception hierarchy

Do not introduce `Result<T>`, a sealed `Error` type, or app-specific exception classes. There is exactly one sensible response to every failure here (degrade), so a type that forces callers to branch buys nothing and spreads ceremony through every call site. `rationale.md` §3.

### A4 — `catch (e: Exception)` only at a genuine last-resort tier

The one sanctioned broad catch is the fallback-tier pattern in `LockLauncher.kt`, where tier 2 (`startActivity` under an overlay BAL exemption) can fail for reasons the platform does not enumerate, and tier 3 exists precisely to absorb that. If you write a broad catch, there must be a *named next tier* it falls through to. Anywhere else, name the exception.

### A5 — Never an empty catch block

A catch with no body is a silent failure that costs hours later. Minimum acceptable body is a `Log.w` with the cause. If there is genuinely nothing to do, the log line is the something to do.

### A6 — Every logging class declares its own `TAG`

```kotlin
private const val TAG = "LockLauncher"
```

Per-class, `private const val`, matching the class name. No shared logging util (that would be an S4 `utils/` in disguise), no string literal inline.

## State and types

### A7 — Absence is a sentinel, not null

Model "nothing playing" as `NowPlaying.EMPTY`, not `null`. Consumers then read `state.title` without a null dance, and `MediaRepository.clear()` is a plain assignment. Follow this for any new state type: give it an `EMPTY`/default companion value.

### A8 — State-carrying types are `@Immutable data class`

Any type that flows into Compose is a `data class` annotated `@Immutable` (`NowPlaying`, the token groups in `Tokens.kt`, `PlayerSkin`). This is load-bearing for recomposition skipping, not decoration. Mutable fields in a Compose-visible type are a bug.

### A9 — No DI framework; process-wide state is a documented `object`

There is no Hilt, no Koin, no hand-rolled container. Shared state is a Kotlin `object` with a KDoc header saying who writes it and who reads it — `MediaRepository` is the model: the service writes, the UI and the screen-off trigger read. One media state per process makes anything more a cost with no payer. `rationale.md` §2.

### A10 — Anything needing `Context` takes it as a constructor parameter

No `Context` held in an `object`, no application-static leak. Pass it in at the call site (`LockLauncher` takes what it needs), so lifetime is the caller's problem and tests can hand in whatever they like.

### A11 — ViewModels stay thin pass-throughs

`LockscreenViewModel.kt` is the shape: expose repository flows, forward transport calls, hold nothing clever. Logic that would tempt you to thicken a ViewModel belongs in a Compose-free file per A13, where it can be tested without a ViewModel harness at all.

### A12 — Hoist UI state; `StateFlow` across screens

Within a screen, state is `remember` / `rememberSaveable` in the **owning** composable and passed down — for example the auto-ticking `position` hoisted in `LockscreenScreen.kt` and handed to skins as `() -> Long` so only the progress element recomposes. State that must cross screens or outlive a composition is a `StateFlow` on a repository/controller (`MediaRepository.nowPlaying`, `SkinController`, `ThemeController`).

### A13 — Testable logic is extracted Compose-free

If a rule is worth a test, it moves into a plain-Kotlin file with no Compose imports, in the feature's package. `OnboardingLogic.kt` is the pattern: step ordering, summary status, and the core-missing rule live outside `OnboardingFlow.kt` purely so they can be exercised on the JVM. T2 requires the matching test file.

### A14 — Two real callers before an abstraction

No interface with one implementation, no base class with one subclass, no generic parameter with one instantiation. Wait for the second real caller, then extract from the two concrete cases. Splitting an oversized file per S9 is **not** abstraction and does not need two callers (`rationale.md` §8).

## Comments in the code you write

### D3 — Inline comments only for surprise

Comment a non-obvious `remember` key, a platform quirk, or load-bearing ordering — the things a careful reader would otherwise "fix" and break. Never restate the line below it. If the code needs a comment to be readable, rename something instead.

### D4 — Mark deliberate simplifications

```kotlin
// ponytail: <what's simplified> — <when to upgrade>
```

This is the project's debt ledger; it is greppable and it is the only record of a shortcut. Existing example, in `app/build.gradle.kts`: release builds are signed with the debug key so `install.sh` produces a launchable APK with no keystore setup, to be swapped before publishing. Every deliberate shortcut you take gets one of these, with both halves filled in — what, and the trigger for upgrading.
