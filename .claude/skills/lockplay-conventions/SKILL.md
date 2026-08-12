---
name: lockplay-conventions
description: Use whenever writing, editing, refactoring, or reviewing code in the LockPlay repo — new files, new functions, bug fixes, tests, or config. Read before the first edit, not after. Covers how this project structures code, handles errors, tests, and gates commits.
---

# LockPlay conventions

Generated 2026-08-13 by `fudge:conventions`. `fudge:conventions` is the generator and lives elsewhere; this file is the artifact and lives in this repo. Editing this file changes this project's rules; it does not change the generator.

## The rules that matter most

1. **Skins never read design tokens.** Each skin in `ui/lockscreen/skin/` is a self-contained visual identity; the moment one imports `design/Tokens.kt` the skins collapse into theme variants of a single look, which is exactly the product this app is not.
2. **Logic worth testing is extracted Compose-free, and it has a matching test.** If a rule lives inside a `@Composable` it cannot be tested on the JVM, so it silently becomes untested — `OnboardingLogic.kt` + `OnboardingLogicTest.kt` is the shape to copy.
3. **Catch the narrowest platform exception, log at `Log.w` with the cause, degrade.** A broad or empty catch turns a revoked permission into an invisible dead app instead of a working lesser state, and you lose the one log line that would have explained it.
4. **NowPlaying data is never persisted, never in a log message at any level, and never leaves the device.** Title/artist/album art is the user's listening history; a single `Log.d` of it writes that history to logcat where other tooling can read it.
5. **Done means `./gradlew check` is green AND you have seen the change work on a real device via `./run.sh`.** This app's whole surface is a lockscreen with no UI tests — compiled-and-passing proves nothing about whether the screen actually appears.

## Adding or moving code

Single `:app` module, packages split by concern (`model/`, `media/`, `trigger/`, `design/`, `ui/<feature>/`), one-way import direction, no `utils/` package, a new skin is one file plus one line in `BuiltInSkins`. Before creating, moving, or splitting a file: **read `references/structure.md`.**

## Errors, boundaries, and state

Narrow platform catches at the platform seam, no `Result<T>`, no DI framework, sentinel-not-null absence, `@Immutable data class` state, thin ViewModels, hoisted Compose state, Compose-free logic files, two callers before an abstraction. Before writing a `try`, a new class, a ViewModel, or any shared state: **read `references/architecture.md`.**

## Tests and the check command

The one command is `./gradlew check`. It runs unit tests, Android lint, and `spotlessCheck` — there is no second wrapper script and no CI.

- How to write tests here (JUnit4, backtick names, fakes not mocks, no Compose pixel tests): `references/testing.md`.
- What `check` runs, formatting, and the Spotless/ktlint setup: `references/tooling.md`.
- Commit and release habits: `references/workflow.md`.
- Permissions, secrets, and user data: `references/security.md`.

## Definition of done

- [ ] Code follows the structure and import-direction rules in `references/structure.md`.
- [ ] Any Compose-free logic added has a matching test file in `app/src/test/` mirroring its package.
- [ ] Tests land in this same commit; nothing is `@Ignore`d and nothing is left red.
- [ ] Every deliberate shortcut carries a `// ponytail: <what's simplified> — <when to upgrade>` marker.
- [ ] The change was seen working on a real device via `./run.sh`, not just compiled.
- [ ] `./gradlew check` passes locally — output read, not assumed.
- [ ] The rules touched by this change are still true.

## When this file doesn't cover it

Name the ambiguity to the user and ask. Do not improvise a rule and do not generalise an existing one to cover a case it was not written for — a rule invented mid-task becomes precedent nobody agreed to. If you are trying to work out *why* a rule is the way it is, or whether two rules genuinely conflict, the register of decisions and rejected alternatives is `references/rationale.md`; read it before proposing a change to any rule.
