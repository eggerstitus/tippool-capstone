# Project brief: TipPool cents handling

> Drafted by the AI from the code audit; the need, user and display decision below were then
> supplied by the owner (2026-11-24).

## The need
TipPool splits a shift's tip pool across employees by tier (100 / 75 / 50 / 25 %). Today
`TipPoolViewModel.calculate()` refuses any pool that isn't a whole-dollar amount: card $100.50 +
cash $111.90 = $212.40 shows an error and no split. **Owner: cents come up on about
99% of shifts**, so today's whole-dollar rule makes the app hard to use almost every time.

Secondary defects found in the same function: negative amounts are accepted and produce negative
payouts; `NaN`/`Infinity` parse as valid numbers; amounts that overflow `Int` are not handled; and
there is no test of any tip-pool behavior.

## The user
The shift leads do the count, usually in the morning, on an Android phone: slightly rushed but
not under pressure. They need it **quick and accurate**: no extra taps and no cents math in
their head.

## The change
The calculator accepts any amount with cents, works in integer cents internally (no floating
point), splits the **whole dollars** across tiers exactly as it does now, and shows the leftover
cents on their own line, **"Leave for next pool"**. Cents are display-only: nothing is stored and
nothing carries into another pool: the physical cents go in a jar and are counted again with the
next pool's cash. The line is hidden when there are no cents.

## Definition of done (checklist)
- [x] $212.40 split with 7 / 0 / 0 / 0 employees gives a result, not an error; $0.40 is shown as
      "Leave for next pool".
- [x] A whole-dollar pool **hides** the "Leave for next pool" line (owner decision: the cents go in
      a jar counted again in the next pool, so the line only matters when there are cents).
- [x] Negative, NaN, Infinity and overflowing amounts are rejected with a plain message.
- [x] Existing whole-dollar results are unchanged (regression tests pin today's numbers).
- [x] The tip-pool math is a pure function with its own test class, written test-first.
- [x] `./gradlew testDebugUnitTest --tests "com.tippool.TipPoolMathTest"` is green; README says how.
- [x] Adversarial review of the math change is saved and every finding is triaged.
- [x] Retro, SUBMISSION.md, and one reusable skill are committed.

## Explicit non-goals
- Carrying cents into the next pool, or storing anything between shifts.
- Fixing the 4 pre-existing failing catering tests (see `docs/evidence/baseline-tests-2026-11-24.md`).
- Any change to the catering features, print report, settings, or notifications.
- A UI redesign: one new row, nothing else.
- Fairness policy for the whole-dollar remainder ("Extra dollars" after flooring each tier) - the
  existing behavior stays; only documenting it.
- Play Store release, signing, or publishing a build.

## Status of the checklist (2026-11-26)
Ticked items are covered by `TipPoolMathTest` (42 green) and the review files; see `SUBMISSION.md`.
The "hides the line at $0" item is implemented in `TipPoolScreen.kt` but **not seen on a device**.
The retro is written (`docs/retro.md`); the screen has still not been seen on a device.
