# Old floating-point payouts vs new integer payouts

Suspicion: the old `calculate()` used `Double` (`floor(pool / shares * pct/100)`), the new
`calculatePool` uses integer division (`pool * pct / sum(count*pct)`). They might disagree on an
exact split (a dollar lost to rounding error).

Method: `compare-old-vs-new.py` re-implements the old formula with IEEE doubles (same as the JVM)
and the new one with integers, and compares every payout.
Range: pool $1..$1,500, 0..8 people in each of the 4 tiers (all combinations with at least one
person) = 9,840,000 cases.

Result: **0 differences.** The suspicion was wrong; existing whole-dollar results are unchanged in
that range. Not covered: pools above $1,500 and tiers above 8 people (the integer version is
exact for all inputs by construction, so only the old code could be wrong there).

Consequence: no caveat-catch from this; the regression tests keep pinning known examples
(`wholeDollarPool_hasNoLeftoverCents`, `mixedTiers_...`).
