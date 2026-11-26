# Adversarial review: raw report (unedited)

- Reviewer: a fresh Claude subagent (model: Opus), started with no conversation history.
- Given: `docs/brief.md` and the diff `git diff 362e815..HEAD -- app/src/main app/src/test` at commit f0adebd.
- Told NOT to read `docs/plans` or `docs/evidence`; read-only on the repo.
- The author's response to each finding is in `triage-2026-11-26.md`.
- Pasted exactly as returned (only this header added).

---

Adversarial review of the TipPool cents change (`git diff 362e815..HEAD -- app/src/main app/src/test`), done against docs/brief.md only. I did not read docs/plans or docs/evidence, and I changed nothing in the repo. I did not run the Gradle tests. My throwaway scripts are in the scratchpad.

**Overall:** I found no high-severity money-math bug. The integer math is correct, and it matched the old floating-point payouts exactly in every case I fuzzed. The findings are about inputs real people type, numbers that don't add up on screen, a brief item that is only half met, and test gaps.

## Findings

1. **Medium — locale decimal comma.** Files: `TipPoolMath.kt:25` (the `MONEY` regex), `Components.kt:120` (`KeyboardType.Decimal`), `TipPoolScreen.kt:381,454,463-468` (`String.format("%.2f")`).
   - **Problem:** the parser accepts only `.` as the decimal point. Every displayed amount uses `String.format` with the device's default locale.
   - **Scenario (by reasoning):** a phone set to a comma locale (es_US, de, fr, pt_BR). Some keyboards (Samsung, Gboard in those locales) show only `,` on the Decimal keypad. The person types `100,50`. Expected: $100.50. Actual: "Card tips isn't a valid amount. Use numbers like 123.45.", which they may not be able to type. On the same phone the app's own output shows "$0,40" and "$212,40", so the app displays the very format it rejects.
   - **Compared with before:** the old code silently treated `100,50` as $0, so the new code is safer. But this is now the main way a real person gets stuck.
   - **Fix:** accept exactly one `,` as the decimal point when there is no `.` and at most two digits follow it. Format with `Locale.US`, or consistently with the locale on both sides. Add tests.

2. **Medium — negative and overflow amounts don't get their own message; `NEGATIVE_AMOUNT` can't be reached from the UI.** Files: `TipPoolMath.kt:31` and `TipPollViewModel.kt:76-82`.
   - **Problem:** the regex has no sign, so `-5` fails in `parseCents` and gets the generic message. The ViewModel's "Tips can't be negative." is never shown. An 8-digit amount also gets the generic message, with no mention of the limit.
   - **Scenario (by reasoning from the code):**
     - Card `-20` -> "Card tips isn't a valid amount. Use numbers like 123.45." The brief wants negative and overflowing amounts rejected "with a plain message". That message is plain-ish but doesn't say what is wrong.
     - `10000000` -> same generic message.
     - `00000001` ($1 with leading zeros) is refused because the digit count includes leading zeros.
   - **Fix:** have `parseCents` return a reason (negative, too big, not a number), or check for a leading `-` and the length before matching, then map to "Tips can't be negative." and "Amount is too large (max $9,999,999.99)." Strip leading zeros before the length check. If you keep it as is, delete the dead `NEGATIVE_AMOUNT` branch or note that it guards only direct callers.

3. **Low — the live "Total pool" silently ignores an invalid field, and its comment is wrong.** File: `TipPollViewModel.kt:58-62`.
   - **Problem:** the comment says the live total "never shows an amount calculate() would reject". It does: `parseCents(x) ?: 0L` drops the bad field.
   - **Scenario (by reasoning):** card `100,50`, cash `111.90`. The live total shows "$111.90" as if it were real. Calculate then errors. A rushed lead may read $111.90 as the pool.
   - **Fix:** if either field fails to parse, hide the live total or show "—". Fix the comment.

4. **Low — the "Math breakdown" doesn't add up.** File: `TipPoolScreen.kt:463-471`.
   - **Problem:** it lists Card $100.50 and Cash $111.90, then a rule, then "Pool (whole $) $212". The $0.40 appears only in the other card ("Leave for next pool"), so the breakdown has no line that reconciles 212.40 to 212.
   - **Scenario:** the brief's own $212.40 example. The column visibly sums to 212.40 but the next line says 212.
   - **Brief constraint:** "one new row, nothing else" argues against adding a row. The label change "Total pool" -> "Pool (whole $)" is already a small extra UI change.
   - **Fix:** say "Pool (whole $, $0.40 left for next pool)" when the leftover is above zero, or accept this and note it.

5. **Low — a pool under $1 is refused instead of shown as leftover.** File: `TipPoolMath.kt:48`.
   - **Scenario:** card `0.60`, cash `0.30`. Result: "Total tip pool is under $1." That is consistent, but by the brief's own model these cents simply go in the jar.
   - **Fix:** probably nothing beyond a product decision. Mention it in the README.

6. **Low — behavior changed for inputs that used to work.**
   - **Now refused (old `toDoubleOrNull` took them):** `+50`, `1e3`, `12.345` (three decimals; the old code rounded and then refused it for cents anyway), `50.` (still fine), and pasted `$50` or `50.00 ` with a zero-width space (U+200B; `trim()` doesn't remove it).
   - **Old silent bug now made loud:** invalid text such as `abc` or `1,000` used to count as $0 with no error. Now it errors, which is better.
   - **No action needed for `+` or `$`** unless you want them; nobody types them on a Decimal keypad. Consider stripping a leading `$`, since pasted amounts may carry one.

7. **Medium — test gaps.** File: `TipPoolMathTest.kt`. The tests pass but would not catch these:
   - **Cents carrying across fields:** nothing tests where cents from both fields roll into a dollar (`60` + `60` cents -> pool 1, leftover 20). Every cents test has the cents in only one field. A bug that computed `card/100 + cash/100` would pass all current tests except possibly the property loop, which also uses cash = 0. Add `split(10060, 11160, ...)` -> 212 dollars, 20 cents.
   - **The 7-digit limit:** no boundary tests. Wanted: `9999999.99` accepted (999999999 cents), `10000000` rejected, and both fields at the max still a valid split (pool 19,999,999, no Int overflow). The only "huge" test uses 20 digits, which would pass with almost any limit.
   - **Comma, `+`, `$`, NBSP, unicode digits:** no tests for comma decimal, `+5`, `$5`, NBSP trim, or Arabic-Indic / full-width digits. These are rejected today only because Java's `\d` is ASCII-only without the UNICODE_CHARACTER_CLASS flag. A future flag change would let `"١٢".toLong()` through silently.
   - **No ViewModel coverage:**
     - error-message mapping for each rejection (the dead `NEGATIVE_AMOUNT` path would have shown up);
     - `cardTips`/`cashTips` division;
     - `TIERS.first { }` label lookup;
     - `valuePerShare` in the breakdown;
     - `liveTotal`.
   - **Regression pinning is thin:** the brief asks that regression tests pin today's numbers. Only two whole-dollar cases (700/7 and the 500 mixed case) are hand-pinned, and nothing compares against the old algorithm. In fairness, my fuzz (item 2 of the checks below) found no difference, so the risk is low. A small table of pinned old outputs would still make that claim auditable.
   - **Weak assertion:** `paidOutPlusExtra_alwaysEqualsPoolDollars` checks `extraDollars >= 0`. It does not check that no tier's payout exceeds the fair share or that `extraDollars < totalSharesPct/100`-ish, so it would pass with every payout set to 0.

8. **Nit — number formatting and labels.** File: `TipPoolScreen.kt:454`.
   - The new row's value is formatted from a Double (`leftoverCents / 100.0`). That's harmless for 0..99, but `"$0.%02d".format(cents)` keeps it integer and independent of locale.
   - The "Extra dollars" box and the "Leave for next pool" row are styled differently. That's fine under "one new row", just noting it.

9. **Nit — misleading file and package naming (existing).** `TipPollViewModel.kt` (typo "Poll") and the `com/example/tippool` folder hold `package com.tippool`. The new files follow the same pattern, so nothing breaks; it just misleads.

## Checked and fine

1. **Integer math:** `payout = poolDollars*pct/totalSharesPct` with Long math is the exact floor of the intended share; `subtotal <= pool`, so the Int casts are safe; max pool 19,999,998 dollars fits Int; `poolDollars*pct` uses Long; tier counts only change by +-1 through the stepper (floored at 0), so `totalSharesPct` can't realistically overflow.
2. **No regression against the old float math (verified by running):** fuzzed old (Double, floor) against new (integer) on every count combination 0..8 per tier for pools $1-$3000, plus 2M random cases (counts 0..40, pools up to $20M). Zero differences in any tier payout.
3. **Rounding and cents:** leftover = `totalCents % 100` after summing both fields in cents is correct. Cents are never stored or carried; `leftoverCents` lives only in `CalculationResult`. The row is hidden at 0.
4. **Parser basics (by reasoning about Kotlin/Java regex semantics):** NaN, Infinity, exponents, thousands commas, three decimals, two points and a lone `.` are all rejected; blank counts as 0; Kotlin `trim()` removes NBSP and ordinary spaces; `\d` is ASCII-only so unicode digits are rejected rather than mis-parsed; very long input is length-checked before `toLong()`, so no crash.
5. **Tiers:** order (highest pct first) matches the old `TIERS` order; empty or negative-count tiers are dropped.
6. **Brief's example:** $212.40 with 7/0/0/0 gives 212 -> $30 each, $210 paid out, $2 extra, $0.40 leave-for-next-pool, which matches the brief.
7. **Scope:** nothing in the diff touches catering, print, settings or notifications. No secrets or personal data in the diff; safe for a public repo.
