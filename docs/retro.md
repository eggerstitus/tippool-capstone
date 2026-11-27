# Retro: TipPool cents handling

**Project:** the tip-pool calculator in my older TipPool app now accepts cents. Whole dollars are split
across tiers as before, and the leftover cents are shown as "Leave for next pool" (hidden when there are none).
Evidence map: [`SUBMISSION.md`](../SUBMISSION.md).

## What did the AI do well?
- **It re-explained my own old app to me.** I wrote TipPool a while ago. The AI read the code, reminded me how
  the tip math works, and pointed out where it could be better (cents rejected, negatives accepted, no tests at all).
- **It worked test-first and showed it.** Tests were committed failing before the code (`18bcd52` then `a634455`;
  `6a00731` then `4fee76f`), with the red and green runs saved in `docs/evidence/`.
- **It checked its own suspicion and reported that it was wrong.** It worried that the old floating-point math and
  the new integer math would disagree, tested 9.84 million cases, and found no difference.
- **It found the 4 failing catering tests** that were already red before we started, and kept them out of my way
  as a documented non-goal instead of hiding or "fixing" them.
- **It set up the fresh public repo and masked personal data** (a phone number and two restaurant emails).

## What did it do badly, and how did I catch it?
Details in [`docs/failures.md`](failures.md).
- **It started in the wrong place.** It first wanted to work inside the repo of another project I'm working on,
  because it didn't understand that this project was branched off that one. I told it to leave that project alone
  and we made a fresh repo.
- **It said the git history was clean when it wasn't.** The first commit still held my phone number. It was caught
  by re-scanning every commit before the first push, so nothing was ever published.
- **A long command silently did nothing.** `git status` showed no files had changed, so the step was redone with
  shorter commands.
- **Its first draft of the brief was written from the code alone.** It assumed things about who counts the tips and
  when. The real facts (cents on about 99% of shifts, shift leads count in the morning, the cents go in a jar) came
  from me.
- **Plan v2 was the AI's own critique of plan v1, not me rejecting a plan.** I approved plan v1 as written.
- **I never ran the finished screen on a device.** Everything was compiled and unit-tested only.

## Where did my judgment enter?
**Stack-call.** I chose my older legacy TipPool over my newer, more complete CaterChecked app. The older one let me
experiment with a feature more freely. Another alternative was to keep rejecting cents, which the old app always did;
I decided to allow them, and the AI helped me do it without changing any existing whole-dollar results.

**Caveat-catch.** The brief the AI drafted assumed a different workflow than mine: counting at the end of a shift,
and cents as something to deal with in the app. I corrected it. The count happens in the morning, and the
leftover cents are physically put in a jar that gets counted again with the next pool. That is why the cents are
display-only: the app stores nothing and carries nothing forward. (The "leave for next pool" wording is mine, and
it could be read as "the app carries these forward", so I clarified what I meant.)

**Taste-pick.** I decided to hide the "Leave for next pool" line completely when there are no cents, because with a jar
it only matters when there are cents to put in it.

**Next-run.** I didn't just fix the problems that came up, I made sure they won't come up again on my next project.
The skill is a step-by-step process for pulling logic out of app code and changing it test-first, so I don't have to
figure that out again. I added rules to CLAUDE.md from the mistakes we made (keep commands short, and check every
commit for personal info before making a repo public), so the AI follows them from the start next time. I also set up
the CI workflow so the tests run on their own every time I push, instead of me remembering to run them.

## What did I encode for next time?
- A reusable skill, [`.claude/skills/pure-logic-test-first/SKILL.md`](../.claude/skills/pure-logic-test-first/SKILL.md),
  with a prompt starter for pulling logic out of UI code and changing it test-first.
- Two rules added to [`CLAUDE.md`](../CLAUDE.md) after my own failures: keep shell commands short and verify each step,
  and scan every commit for personal data before making a repo public.
- A GitHub Actions workflow that runs the tip-pool tests on every push (it passed first time).
- A README with run and test steps, and a triage file format for reviewing an AI review.

## What would I delegate differently on the next project?
I would not hand over features where the answer is mostly a matter of design taste, such as wording and layout.
The agents' design ideas don't always look good, so I would decide those myself and delegate the verifiable parts:
the math, the tests, the repo setup and the review.

## Honest gaps
- I did not run the finished screen on a device before submitting.
- The ViewModel and the screen code have no automated tests; the math lives in a separate file that does.
- The 4 pre-existing failing catering tests are untouched.
- Three of the review's findings (#4, #5, #6) I accepted without a code change; the reason is in
  [`docs/review/triage-2026-11-26.md`](review/triage-2026-11-26.md).
