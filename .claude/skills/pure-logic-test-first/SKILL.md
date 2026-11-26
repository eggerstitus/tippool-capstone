---
name: pure-logic-test-first
description: Pull business or money logic out of a UI/ViewModel into a pure function and change it test-first. Use when a bug or feature lives in code mixed with UI state (Compose, React, a ViewModel) and you want a small, verifiable, reviewable change.
---

# pure-logic-test-first

Version 1 (2026-11). Minted from the TipPool cents capstone; every step below is something that
step actually caught or would have caught there.

## When to use
- The logic you must change is tangled with screen state, and has no tests.
- It handles money, counts, dates, or parsing of what a person typed.
- NOT for a pure layout or styling change.

## Steps
1. **Find what exists before planning.** Read the function, its tests (if any), the build file and the
   test command. Run the existing suite once and record what is already red, so you do not claim or
   get blamed for it. Save that run.
2. **Write the brief's "done" list first** (need, user, checklist, non-goals), with the owner's words
   for the facts only they know (how often, who, what they do today).
3. **Extract, do not rewrite.** Create a plain file with no UI imports: input types in, a sealed result
   out (`Split` / `Rejected(reason)`), reasons as an enum. Keep the old code until the new one is proven.
4. **Stub, then tests, then red.** Stub the new functions so the tests compile (`TODO()` or an
   obviously wrong constant). Write the tests, run them, and check they fail for the *right reason*
   (assertion or `NotImplementedError`, never a compile error or a typo). Save the red output and commit.
   Tests that already pass are fine if you say so (characterization tests); watch for a test that
   passes only because the stub happens to return the right answer.
5. **Implement the smallest thing that goes green.** Commit. Check the results file, not just the
   console ("BUILD SUCCESSFUL in 5s" can hide that nothing ran).
6. **Compare old against new** with a brute-force script over many inputs before swapping the call
   site. If they disagree, that is your finding; if not, record that the suspicion was wrong.
7. **Wire it in, one idea per commit** (ViewModel, then UI). Move user-facing messages into the pure
   file too, so they are tested.
8. **Adversarial review in a fresh session.** Give it the brief and the diff only (not your plan or your
   reasoning), read-only, and ask for concrete inputs. Save its report unedited, then triage every
   finding: fix, or accept in writing. Fix test-first again.
9. **Say what you did not verify** (e.g. never ran it on a device) in the README.

## Pitfalls seen
- Doubles for money: use integer cents. Parse what people type, locale included (`,` decimals).
- A fast "green" can mean nothing ran: read the test-results XML.
- Rewriting history is fine before the first push, never after. Scan for phone numbers, emails and
  keys in *every* commit before making a repo public.
- Long multi-line shell commands can fail silently: write files with the editor tool and confirm with `git status`.

## Prompt starter
> I want to change <behavior> in <file/function>. Before editing: run the existing tests and record what
> is already failing; read the code; propose a plan with the smallest change. Then extract the logic into
> a pure file, stub it, write tests first, show me them failing for the right reason, and stop for my
> approval before implementing.
