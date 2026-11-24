# Project brief: TipPool cents handling

> DRAFT written by the AI from the code audit. Lines marked **[YOU]** need the owner's own
> words or correction before this counts as the brief.

## The need
TipPool splits a shift's tip pool across employees by tier (100 / 75 / 50 / 25 %). Today
`TipPoolViewModel.calculate()` refuses any pool that isn't a whole-dollar amount: card $100.50 +
cash $111.90 = $212.40 shows an error and no split. **[YOU: how often this happens on a real
shift, and what a shift lead does today when it does - round by hand? skip the cents?]**

Secondary defects found in the same function: negative amounts are accepted and produce negative
payouts; `NaN`/`Infinity` parse as valid numbers; amounts that overflow `Int` are not handled; and
there is no test of any tip-pool behavior.

## The user
A shift lead counting tips at close, on their own Android phone, at the end of a shift.
**[YOU: confirm / correct - who actually does the count, and how rushed are they?]**

## The change
The calculator accepts any amount with cents, works in integer cents internally (no floating
point), splits the **whole dollars** across tiers exactly as it does now, and shows the leftover
cents on their own line, **"Leave for next pool"**. Cents are display-only: nothing is stored and
nothing carries into another pool.

## Definition of done (checklist)
- [ ] $212.40 split with 7 / 0 / 0 / 0 employees gives a result, not an error; $0.40 is shown as
      "Leave for next pool".
- [ ] A whole-dollar pool shows "Leave for next pool: $0.00" (or hides the line - UI decision,
      recorded in the retro).
- [ ] Negative, NaN, Infinity and overflowing amounts are rejected with a plain message.
- [ ] Existing whole-dollar results are unchanged (regression tests pin today's numbers).
- [ ] The tip-pool math is a pure function with its own test class, written test-first.
- [ ] `./gradlew testDebugUnitTest --tests "com.tippool.TipPoolMathTest"` is green; README says how.
- [ ] Adversarial review of the math change is saved and every finding is triaged.
- [ ] Retro, SUBMISSION.md, and one reusable skill are committed.

## Explicit non-goals
- Carrying cents into the next pool, or storing anything between shifts.
- Fixing the 4 pre-existing failing catering tests (see `docs/evidence/baseline-tests-2026-11-24.md`).
- Any change to the catering features, print report, settings, or notifications.
- A UI redesign: one new row, nothing else.
- Fairness policy for the whole-dollar remainder ("Extra dollars" after flooring each tier) - the
  existing behavior stays; only documenting it.
- Play Store release, signing, or publishing a build.
