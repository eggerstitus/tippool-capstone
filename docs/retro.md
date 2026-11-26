# Retro

> **SKELETON, NOT THE RETRO.** The AI wrote the facts under each heading ("Facts, from the record").
> The text after "Your answer" must be written by the owner, in their own words, with specifics only
> they know. Delete these notes and the fact bullets you don't use. Target: 1-2 pages.

## What did the AI do well?
Facts, from the record:
- Found the legacy app and wrote an audit before touching code; set up the fresh repo and masked personal data.
- Test-first order is visible in the log (red `18bcd52` -> green `a634455`; red `6a00731` -> green `4fee76f`).
- Checked its own suspicion (Double vs integer math) with a 9.84M-case comparison and reported that it was wrong.

Your answer: _

## What did it do badly, and how did you catch it?
Facts, from the record (`docs/failures.md`):
- Reported the history as clean while the baseline commit still held a phone number; caught by re-scanning all commits before the first push.
- A long shell command silently did nothing; caught by `git status`.
- Wrote the first draft of the brief's "need" and "user" from the code alone, before the owner supplied the real numbers (cents on ~99% of shifts).
- Never ran the screen on a device.
- Plan v2 was the AI's own critique of plan v1, not an owner rejection.

Your answer (what did *you* catch, and what did you miss?): _

## Where did your judgment enter?
Facts only; the judgment is yours.

**Stack-call** (a decision you owned against a plausible alternative):
- Candidates: chose the legacy tip pool app over CaterChecked as the project; chose whole-dollar split with leftover cents (option "B") over rejecting cents or tracking cents per person; chose display-only cents (nothing stored) over carrying them to the next pool; made the repo public.
- Your answer: _

**Caveat-catch** (an assumption or AI claim that turned out wrong or incomplete):
- Candidates: the 4 baseline-red catering tests nobody had told you about; the AI's "history is clean" claim; the AI's plan assumed "cents" rather than your real fact that cents are on ~99% of shifts and go in a jar; the reviewer's "comma keyboard" finding.
- Your answer: _

**Taste-pick** (a call that was yours because it was judgment, not correctness):
- Candidates: hide "Leave for next pool" at $0.00 (your reason: the jar is counted again); label wording; which review findings to accept without a change (#4, #5, #6 in the triage - these were the AI's first-pass calls, did you agree?).
- Your answer: _

**Next-run** (what you designed for the NEXT run, not the perfect run):
- Candidates: the `pure-logic-test-first` skill; the CLAUDE.md rules from the failure record; the CI workflow.
- Your answer: _

## What did you encode for next time?
Facts: skill file, CLAUDE.md rules, CI workflow, README run/test instructions, triage template in `docs/review/`.

Your answer: _

## What would you delegate differently on the next project?
Your answer: _

## Honest gaps
- The screen was not run on a device before submission (or: you ran it and found ...). _
- The ViewModel and screen have no automated tests.
- The 4 pre-existing failing catering tests are untouched.
