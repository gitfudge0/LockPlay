# Workflow

## W1 — Direct commits to `main`

Solo repo. No feature branches, no pull requests, no CI. Commit to `main`. Do not create a branch "to be safe" and do not open a PR against this repo unless the user explicitly asks.

## W2 — Conventional Commits

`feat:`, `fix:`, `chore:`, `refactor:`, `docs:`, `test:` — imperative subject, under ~72 characters, no trailing period.

```
feat: add Cassette skin + debug skin lab
chore: rename MusicLock -> LockPlay
```

Write a body only when the *why* is not obvious from the diff (a platform quirk you worked around, a rejected alternative). Do not write a body that lists the files you changed.

## W3 — One commit per logical change

A rename is its own commit, never bundled with behaviour — otherwise the diff is unreadable and a later `git log` cannot tell you when behaviour actually changed. Same for a mechanical reformat, a dependency bump, and a move between packages: each stands alone.

## W6 — Done = `check` green **and** seen working on a real device

`./gradlew check` passing is necessary, not sufficient. Run `./run.sh` and watch the actual change on a device: the lockscreen appearing on screen-off, the skin rendering, the transport buttons doing something. A compiled-only Android change is not done.

This rule exists because T3 deliberately declines to write UI tests — the device check is where layout, gesture, orientation, and lockscreen-trigger regressions get caught. If you cannot get to a device, say so explicitly rather than reporting the work as done.

## W7 — Release is `./install.sh`

That is the whole release process. No version bump, no tag, no changelog ceremony until the app actually ships to users. Note that release builds are currently signed with the debug key (marked with a `// ponytail:` in `app/build.gradle.kts`, per D4) — that marker is the trigger to revisit before publishing.
