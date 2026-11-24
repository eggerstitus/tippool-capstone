# Plan v1 (2026-11-24) - status: APPROVED by owner, then REVISED -> see plan-v2.md

1. Docs first: brief, CLAUDE.md, plans folder.
2. Test-first: new pure function `calculatePool(cardCents, cashCents, counts)` in `TipPoolMath.kt`;
   write `TipPoolMathTest` first, commit it failing.
3. Policy B: integer cents; floor pool to whole dollars; split dollars across tiers as today;
   remainder cents reported separately as "Leave for next pool".
4. Wire it in: `TipPoolViewModel.calculate()` calls it, cents error removed, reject negative and
   non-finite input, UI shows a "Leave for next pool" row.
5. Adversarial review in a fresh session; triage file.
6. Reusable skill (`extract-pure-logic-test-first`) + a GitHub Action running only tip-pool tests.

## Critique recorded when v1 was reviewed
Owner said "plan looks good" and chose: public repo, name `tippool-capstone`, cents are
display-only (nothing is stored or carried to the next pool).
Weak spots the AI flagged itself in the same message (not owner objections):
- Step 4 bundles validation + ViewModel + UI in one step -> one big commit.
- Step 6 CI may be dishonest if a runner has no Android SDK.
