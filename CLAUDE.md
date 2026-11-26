# TipPool - project context for AI sessions

Small Android app (Kotlin, Jetpack Compose, single `app` module). Two features: the **tip pool
calculator** (home screen; the subject of the capstone) and a larger **catering** tool (orders,
supply lists, PDF reports) that is out of scope. Capstone goal: fix cents handling in the tip pool,
with tests. See `docs/brief.md` for what "done" means and `docs/plans/` for the plans.

## Where things are
- Tip pool logic: `app/src/main/java/com/example/tippool/TipPollViewModel.kt` (sic - the filename
  has a typo; don't rename it in this capstone) - `TipPoolViewModel.calculate()`.
- Tip pool UI: `TipPoolScreen.kt`. Tiers are the `TIERS` list (100/75/50/25, 7 people default at 100%).
- Catering code (leave alone): `Catering*.kt`, `Settings*.kt`, `NotificationHelper.kt`.
- Packages: sources live in `com/example/tippool/` but the declared package is `com.tippool` (and
  `com.tippool.ui.screens`). `ExampleUnitTest` wrongly declares `com.example.tippool`. Use `com.tippool`.

## Build and test
- JDK: use `C:\Users\Titus\.jdks\dragonwell-21.0.12` (`JAVA_HOME`). Android Studio's bundled JBR is
  JDK 25, which this Gradle setup does not use. No `java` is on the PATH.
- Unit tests: `./gradlew.bat :app:testDebugUnitTest --offline` (about 70 s). The tip-pool tests only:
  `... --tests "com.tippool.TipPoolMathTest"`.
- `local.properties` (SDK path) is local and git-ignored.
- **Known red:** 4 `CateringLogicTest` tests fail on the baseline and are out of scope. Never claim
  "all tests pass"; name the class that passed.

## Rules for this repo
- Money math is in **integer cents**, never `Double`. Floor to dollars per tier exactly as the
  current code does. Do not change existing whole-dollar results; pin them with tests.
- Cents left over are **display-only** ("Leave for next pool"). Do not store or carry them.
- Pure logic goes in a plain Kotlin file with no Compose or Android imports so it unit-tests on the JVM.
- Test first for new behavior: write the test, run it, see it fail for the right reason, commit it
  failing, then implement. Keep that order visible in `git log`.
- Small commits, one idea each. Never commit build output, `.idea/`, or `local.properties`.
- Do not touch the catering code. Do not put real phone numbers, emails or names in the repo - the
  repo is public (the original number and two restaurant emails were masked on import).
- Evidence (test output, review reports, triage) goes under `docs/evidence/`; plans under `docs/plans/`.
- Pushing: remote is HTTPS `eggerstitus/tippool-capstone`. Ask before pushing.
- Shell hygiene (learned the hard way, docs/failures.md #1): no long multi-heredoc commands. Write files with the
  editor tool, keep shell commands short, and verify with `git status` or the test-results XML before saying a step happened.
- Before making anything public, scan EVERY commit (`git grep <pattern> $(git rev-list --all)`), not just the working tree.
