# Security and user data

This app reads the user's music-notification stream. That is the most sensitive thing it touches, and every rule below exists to keep it from leaking.

## X1 — No secrets in the repo or `BuildConfig`

No API keys, tokens, or credentials committed, and none injected into `BuildConfig`. Keystores and `local.properties` stay gitignored. Known exception, already recorded as a `// ponytail:` marker in `app/build.gradle.kts` (D4): release builds are signed with the **debug** keystore so `install.sh` works with no setup — that marker is the ledger entry, and it is the thing to fix before publishing.

## X2 — No permission without a feature, a degraded path, and an explanation

Adding a `<uses-permission>` (or a special-access grant like notification listener / display-over-apps / full-screen intent) requires all three:

1. a user-visible feature that genuinely needs it;
2. a working degraded path when it is denied or later revoked — this is the A1/A2 catch (`MediaListenerService.kt` keeps running with no sessions when notification access is pulled);
3. a plain-language onboarding step explaining *why*, shown **before** the system dialog — the wizard in `OnboardingFlow.kt` / `OnboardingLogic.kt`, whose step list `OnboardingPerms` is the registry to extend.

If you cannot supply all three, the permission does not go in the manifest.

## X3 — NowPlaying data is in-memory only

Title, artist, album, and album art:

- **never persisted** — not to DataStore, not to a file, not to a cache you control;
- **never included in a log message at any level** in `src/main/` — not `Log.d`, not while debugging, not temporarily. Log the failure and, if you need to identify the source, the controller's **package name**. Metadata in a `Log.d` is allowed only in `src/debug/` sources;
- **never leaves the device** — no analytics, no crash-reporter breadcrumbs, no network call carrying it.

It lives in `MediaRepository`'s `StateFlow` and dies with the process. This constraint overrides the natural instinct in A2 to log "what we were handling"; see `rationale.md` §1.

## X4 — DataStore holds preferences only

The DataStore is for user preferences — theme id, skin id, and things of that shape. No playback history, no track data, no cached metadata. If you are about to write something to DataStore, check it against X3 first.

## X5 — The lockscreen never offers a path past the keyguard

`LockscreenActivity` shows transport controls and nothing else. No "open app", no deep link into another screen, no share sheet, no notification action that dismisses the keyguard. Anyone holding the phone can already reach the transport controls by design; they must not be able to reach anything else.

## X6 — Dependencies earn their place

A new dependency needs real weight behind it (Coil qualifies; a utility library does not) and is always declared in `gradle/libs.versions.toml`. Every dependency is code you ship, permissions you may inherit, and a supply-chain surface. See `tooling.md`.
