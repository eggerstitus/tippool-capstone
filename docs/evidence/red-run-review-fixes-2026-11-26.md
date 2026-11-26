# Red run for the review fixes (tests first)

Command: `./gradlew.bat :app:testDebugUnitTest --offline --tests "com.tippool.TipPoolMathTest"`
Result: 42 tests, 10 failed. 15 tests were added for findings 1, 2, 7 and 8 of the adversarial review;
the new API (`parseAmount`, `amountMessage`, `rejectionMessage`, `formatCents`) was stubbed so the
tests compile and fail on assertions (empty strings, "not a number"), not on missing symbols.

Failing (new behavior not there yet): comma decimal, negative reason, too-large reason, limit boundary,
leading zeros, non-ASCII/decoration, parseCents agrees with parseAmount, amount messages, rejection
messages, formatCents.

Passing already, on purpose (characterization / guards for existing behavior): cents from both fields
carry, both fields at the maximum, every payout is the exact floor, payouts match the old app's pinned
outputs. One passes for the wrong reason: `amount_commaAsThousandsOrMixedIsNotANumber` passes only because the stub
always answers NOT_A_NUMBER; it becomes meaningful once the comma rule exists.
