# Lab 7 submission

Track A. Project: TipPool cents handling (an existing Kotlin Android app; the tip-pool calculator now accepts cents).

## Working delivery

- Run and test instructions: [`README.md`](README.md)
- Brief with the done-criteria checklist (ticked with evidence) and non-goals: [`docs/brief.md`](docs/brief.md)
- Known gaps, stated honestly: README "Known gaps"; not run on a device by the author yet.

## Context and process discipline

- Context file: [`CLAUDE.md`](CLAUDE.md) (written before feature work; two rules added later from the failure record)
- Plans, including a revised one: [`docs/plans/plan-v1.md`](docs/plans/plan-v1.md) -> [`docs/plans/plan-v2.md`](docs/plans/plan-v2.md)
  (note: the revision came from the AI's own critique, confirmed by the owner's cents decision, not from an owner rejection)
- Commit history: `git log --oneline` (23 small commits at the time of writing, docs and tests and code separate)

## Verification and testing

- Test suite: `app/src/test/java/com/example/tippool/TipPoolMathTest.kt` (42 tests, includes unhappy paths)
- Command: `./gradlew :app:testDebugUnitTest --tests "com.tippool.TipPoolMathTest"`
- Test-first, visible in commit order: `18bcd52` tests + stubs (red) -> `a634455` implementation (green);
  again `6a00731` (red) -> `4fee76f` (green) for the review fixes
- Evidence files in [`docs/evidence/`](docs/evidence/):
  baseline-tests-2026-11-24.md (4 pre-existing failures), red-run-2026-11-24.md, green-run-2026-11-24.md,
  old-vs-new-payouts-2026-11-24.md (+ compare-old-vs-new.py), red-run-review-fixes-2026-11-26.md,
  green-run-review-fixes-2026-11-26.md, ci-run-2026-11-26.md

## Adversarial review

- Raw report (fresh Opus session, brief + diff only, unedited): [`docs/review/adversarial-review-2026-11-26.md`](docs/review/adversarial-review-2026-11-26.md)
- Triage, a decision for every finding: [`docs/review/triage-2026-11-26.md`](docs/review/triage-2026-11-26.md)

## Failure handling and design response

- Failure record (3 real failures, causes, changes): [`docs/failures.md`](docs/failures.md)
- Design/process changes it led to: `CLAUDE.md` shell-hygiene and scan-all-history rules (`5a556eb`),
  history rewrite before first push, skill pitfalls. The retro's failure section links to the same file.

## Reusable artifact

- Skill: [`.claude/skills/pure-logic-test-first/SKILL.md`](.claude/skills/pure-logic-test-first/SKILL.md)
  (trigger, steps, pitfalls and a prompt starter are in the file itself)
- Automation: [`.github/workflows/tip-pool-tests.yml`](.github/workflows/tip-pool-tests.yml), evidence in `docs/evidence/ci-run-2026-11-26.md`

## Retro quality and judgment

- Retro: [`docs/retro.md`](docs/retro.md), including the four judgment prompts (stack-call, caveat-catch, taste-pick, next-run)
