# Tooling

## The check command

```
./gradlew check
```

That is the single gate. It runs:

- **unit tests** — everything in `app/src/test/` (`testDebugUnitTest` / `testReleaseUnitTest`);
- **Android lint** — AGP's `lint` task;
- **`spotlessCheck`** — Kotlin and Gradle-script formatting via ktlint.

## W4 — `check` passes before any commit

No new wrapper script is added for this. `./gradlew check` is the command; if you find yourself wanting a `verify.sh`, the answer is no. Read the actual output — a Gradle task that fails after a long build is easy to scroll past.

## W5 — No pre-commit hook

W4 is stated, not mechanically enforced. There is no hook in this repo and none is to be added; see `rationale.md` §6.

## Spotless / ktlint

Configured in `app/build.gradle.kts`:

```kotlin
spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("1.3.1")
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.3.1")
    }
}
```

with `spotless = "6.25.0"` and the `com.diffplug.spotless` plugin declared in `gradle/libs.versions.toml`, applied `apply false` in the root `build.gradle.kts`.

- `./gradlew spotlessApply` fixes formatting in place.
- `./gradlew spotlessCheck` runs as part of `check` and fails the build on a violation.
- **Existing files may currently fail `spotlessCheck`.** The plugin was added without running a formatter over the tree. Reformatting existing sources is a separate, explicitly-requested job — do not bulk-reformat files you were not asked to touch, and do not bundle a reformat with a behaviour change (W3).

Three ktlint standard rules are disabled: `no-consecutive-comments` (a KDoc followed by an EOL comment is the house pattern — see the `// ponytail:` marker in `app/build.gradle.kts`), `function-naming` (PascalCase `@Composable` functions are the Compose convention), and `property-naming` (see the PascalCase constants rule, S10 in `structure.md`).

Everything ktlint enforces (indentation, import order, wildcard imports, spacing) is not restated here. Run the tool.

## X6 — Dependencies via the version catalog

Every dependency and plugin is declared in `gradle/libs.versions.toml` and referenced as `libs.*`. No inline coordinate strings in a build file. A dependency is added only when it carries real weight — Coil (image loading with caching and Compose integration) qualifies; a utility/extension-function library does not.
