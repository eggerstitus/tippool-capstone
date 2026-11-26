# Failure record

Real failures from this project, what caused each, and the change it forced. Nothing here was staged.

## 1. A long shell command silently did nothing (2026-11-24)
- **What happened:** I sent one big shell command that wrote the brief edits, the new source file and the
  test file, and ran the tests. The shell hit a quoting error ("unexpected EOF while looking for matching
  quote") before running any of it. No file was written and no tests ran.
- **How it was caught:** `git status` afterwards showed nothing changed. Without that check, the next
  message would have claimed the red run happened.
- **Cause:** apostrophes and `$` inside a very long multi-heredoc command.
- **Change:** files are written with the editor tool, shell commands stay short, and every step is
  verified (`git status`, the test-results XML) before it is reported. Encoded in the skill's pitfalls
  (`.claude/skills/pure-logic-test-first/SKILL.md`) and in `CLAUDE.md`.

## 2. Personal data nearly went into a public repository (2026-11-24)
- **What happened:** after replacing the support phone number in a commit on top, I reported the history as clean.
  It was not: the baseline commit underneath still contained the number. A second scan then found two
  restaurant email addresses in the catering code.
- **How it was caught:** I re-checked all commits (`git grep` over `git rev-list --all`) before the first
  push, instead of only the latest tree. Nothing was ever pushed with the data in it.
- **Cause:** I scanned the working tree, not the history, and committed to the repo before deciding
  what was safe to publish.
- **Change:** unpushed history was rewritten so no commit contains the data; a scan of every commit is now a
  step before making any repo public (skill pitfall; `CLAUDE.md` rule "no real phone numbers, emails or
  names in the repo").

## 3. A test that passed for the wrong reason (2026-11-26)
- **What happened:** in the red run for the review fixes, `amount_commaAsThousandsOrMixedIsNotANumber`
  passed immediately, because the stub always answered "not a number".
- **Caught:** by reading which tests were red versus green and asking why each one was green.
- **Change:** the red-run evidence names it; the skill's step 4 says to watch for tests that pass
  only because the stub coincides with the answer.

## Not a failure
- My suspicion that the old `Double` math and the new integer math would disagree was wrong: 0 differences
  in 9.84M cases (mine) and 2M more (the reviewer's). That is a verified non-risk, not a failure.
- CI passed on its first run (`docs/evidence/ci-run-2026-11-26.md`), so there is no CI failure to report.
