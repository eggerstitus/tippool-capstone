# TipPool

Android app (Kotlin, Jetpack Compose) for restaurant shift leads. The home screen is a **tip pool
calculator**: enter card and cash tips and how many employees are in each tier (100 / 75 / 50 / 25 %),
and it shows each tier's payout. The rest of the app is a catering tool that this project does not touch.

This repository is the capstone for an AI-assisted software development course. The change delivered
here: **the calculator now accepts cents.** See [`SUBMISSION.md`](SUBMISSION.md) for where every piece
of evidence lives, and [`docs/brief.md`](docs/brief.md) for the need, the "done" checklist and the non-goals.

## What it does with cents

Tips are counted in cents. The **whole dollars** are split across the tiers (each person's payout is
rounded down to a whole dollar, and any dollars that don't divide evenly are shown as "Extra dollars").
The **leftover cents** are shown as **Leave for next pool** and are *not* stored: the physical cents go
in a jar and get counted again with the next pool. The line is hidden when there are no cents.

Example: card $100.50 + cash $111.90 = $212.40, with 7 people at 100%:
$30 each, $2 extra dollars, **Leave for next pool $0.40**.

Amounts accept `.` or `,` as the decimal point. Refused with a plain message: negative amounts, text
that isn't a number (`abc`, `1e3`, `1,000`), more than two decimals, and amounts above $9,999,999.99.
A pool under $1 can't be split ("Total tip pool is under $1.").

## Run it

You need JDK 21 and the Android SDK (Android Studio installs both).

1. Create `local.properties` with your SDK path: `sdk.dir=C\:\\Users\\you\\AppData\\Local\\Android\\Sdk`
   (Android Studio does this when you open the project; the file is git-ignored).
2. Open the folder in Android Studio and run the `app` configuration on an emulator or a phone, or from a terminal:
   `./gradlew installDebug` (Windows: `gradlew.bat installDebug`).

## Test it

```
./gradlew :app:testDebugUnitTest --tests "com.tippool.TipPoolMathTest"
```

That runs the 42 tip-pool tests (about a minute the first time). Windows with no `java` on the PATH:
set `JAVA_HOME` to a JDK 21 first.

**The whole suite is not green, on purpose.** `./gradlew :app:testDebugUnitTest` also runs 26
`CateringLogicTest` tests, and 4 of them already failed before this project began. Fixing them is a
documented non-goal ([`docs/evidence/baseline-tests-2026-11-24.md`](docs/evidence/baseline-tests-2026-11-24.md)).

## Where the code is

| What | File |
|------|------|
| The math: parsing, splitting, messages (pure Kotlin, no Android) | `app/src/main/java/com/example/tippool/TipPoolMath.kt` |
| Screen state, calls the math | `TipPollViewModel.kt` (the typo in the name is original) |
| The screen | `TipPoolScreen.kt` |
| Tests | `app/src/test/java/com/example/tippool/TipPoolMathTest.kt` |

(The folder is `com/example/tippool` but the Kotlin package is `com.tippool`; that mismatch is original.)

## Known gaps

- The screen has been compiled but not run by the author on a device or emulator.
- `TipPollViewModel` and the screen have no automated tests; the logic that matters lives in `TipPoolMath.kt`.
- Cents are never carried into the next pool by the app.
- On import, a support phone number and two restaurant email addresses in the original code were replaced
  with placeholders so the repository could be public.
