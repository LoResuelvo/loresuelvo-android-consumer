---
name: android-code-review
description: Audit Android code for maintainability, layering, state ownership, parameter explosion, stale comments, and testability, then report or fix findings within the requested scope.
metadata:
  short-description: Review Android maintainability and architecture
---

# Android code review

Use this skill for a focused Android review or before a refactor. Review the
actual code and callers first; do not infer problems from naming alone.

## Review order

1. Check Git status and preserve unrelated local changes.
2. Map the target flow from route to UI, ViewModel, domain contract, and data
   implementation.
3. Identify production functions with many unrelated parameters. Prefer a
   small immutable state/config object plus grouped action contracts when the
   parameters form a coherent boundary.
4. Check dependency direction: domain stays platform-independent, UI uses
   domain contracts, and data implementations remain behind injected
   interfaces.
5. Check state ownership, recomposition safety, lifecycle collection,
   cancellation, error mapping, and test seams.
6. Remove stale issue-tracking references from production comments. Keep
   comments that explain a non-obvious invariant or trade-off; prefer a short
   `why` over a history of which ticket introduced the code.

## Findings

Report concrete findings with severity, file/line, impact, and a minimal
fix. Distinguish behavior regressions from structural debt. Do not broaden a
review into unrelated API or backend changes. If implementing fixes was
requested, preserve behavior and add or update focused tests for each changed
contract.

## Completion

Run the relevant unit tests, `assembleDevDebug`, and `lintDevDebug`. Run
instrumented tests when the review changes UI or navigation wiring. Leave
known environment failures clearly separated from code failures, and verify
`git diff --check` before handoff.
