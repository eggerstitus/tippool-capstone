# Plan v2 (2026-11-24) - revises plan v1

What changed and why:

- **Step 4 split into three commits** (validation, ViewModel wiring, UI row) so each is
  reviewable on its own. v1 would have produced one mixed commit.
- **Step 6 CI is conditional.** Check first whether `./gradlew testDebugUnitTest` can run on
  `ubuntu-latest` (it needs the Android SDK; GitHub's runners include one). If it can, add a
  workflow limited to the tip-pool test class. If not, document honestly that there is no
  automation and why. Do not add a CI job that is never run green.
- **Cents policy fixed by owner:** display-only. Nothing persisted; no state across pools.
  Out of scope: carrying cents forward.
- **Baseline red suite is a non-goal** (see docs/evidence/baseline-tests-2026-11-24.md); the
  green bar is the `TipPoolMathTest` class.
- **Phone number replaced with a placeholder** before the repo goes public.

Origin of the revision: the AI's own critique of v1, confirmed by the owner's cents decision.
It was not an owner rejection, and the retro should say so.
