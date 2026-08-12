# Testing

## T1 — JUnit4, backtick names, mirrored packages

Tests are JUnit4 in `app/src/test/`, in a package mirroring the source package exactly — `com.lockplay.ui.onboarding.OnboardingLogic` is tested by `app/src/test/java/com/lockplay/ui/onboarding/OnboardingLogicTest.kt`. Test method names are backticked sentences describing the behaviour:

```kotlin
@Test fun `summary is blocked when the core permission is missing`() { … }
```

Run them with `./gradlew test` while iterating; `./gradlew check` runs them as part of the commit gate.

## T2 — Every Compose-free logic file has a matching test file

This is the other half of A13. Extracting logic and then not testing it is worse than leaving it inline, because it added a file for no benefit. If you create `FooLogic.kt`, `FooLogicTest.kt` exists in the same commit. If a logic file has no test, either write one or fold the code back into its caller.

## T3 — Do not pixel-test composables; test the contract around them

There are no Compose UI tests and no screenshot tests. What is tested instead is the invariants that surround the UI, which is what actually breaks when someone adds a skin or theme. `SkinTest.kt`, `CardSkinTest.kt`, and `ThemeTest.kt` are the models:

- every built-in skin id is unique;
- the default skin is present in the catalogue;
- every skin declares an orientation;
- every built-in theme resolves a full set of tokens.

When you add a registry or catalogue, add the equivalent structural test. Layout and gesture regressions are caught by the device check in W6, deliberately — `rationale.md` §5.

## T4 — Robolectric only where a platform type is unavoidable

Robolectric is on the classpath for cases where an Android type genuinely cannot be avoided. It is never the shortcut you reach for to avoid extracting pure logic per A13. If a Robolectric test would exist only because the logic sits inside an Android class, move the logic out and write a plain JVM test.

## T5 — No mocking framework

No Mockito, MockK, or equivalent. Use hand-written fakes and plain values: construct a real `NowPlaying`, write a small fake implementing the one function you need. Mocks encode the call sequence you already assumed; fakes encode behaviour, and they break honestly when the real thing changes.

## T6 — Deterministic tests only

No wall clock, no `Thread.sleep`, no real delays, no device state. Time-dependent code takes time as a parameter (a `Long` ms, or a `() -> Long`) so the test passes it in. Coroutine timing uses `kotlinx-coroutines-test`. A test that can fail on a slow machine is a test nobody will trust in a month.

## T7 — Tests land in the same commit as the code

Not a follow-up commit, not "next session". Same commit as the behaviour they cover, so every commit on `main` is independently green.

## T8 — A failing or flaky test is fixed or deleted in that sitting

Never `@Ignore`, never leave `main` red. A test that is flaky is either fixed (usually a T6 violation) or removed outright — an ignored test is a false sense of coverage plus a permanent piece of noise.

## T9 — No coverage percentage target

No coverage tooling and no number to hit. The rule that produces coverage here is T2: extracted logic has a test. Chasing a percentage produces tests for getters.
