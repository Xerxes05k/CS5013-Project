# ADR 0005: A cycle's parts decide whether it can be booked

**Date:** 2026-10-12 · **Status:** Proposed (to confirm with the team before week 5 code)

## Context

At the mid-demo (12 Oct) faculty pointed out that a cycle had no properties beyond its label and state, so nothing in the data explained *why* it would move to its next state. `UNDER_REPAIR` existed in the enum but nothing set it. A guard's note became a `maintenance_flags` row and the cycle went back to `AVAILABLE` regardless, even if the guard had said the brakes had failed.

The classifier training data (`labelled-comments.tsv`) already names the 8 parts residents complain about: tyre, brakes, chain, seat, pedal, handlebar, lock, bell.

## Decision

1. **Every cycle has 8 parts, each with a condition.** One `cycle_parts` row per (cycle, part), with condition `OK`, `REPORTED` or `BROKEN`. The parts are the same 8 labels the classifier uses, so a classifier label maps straight to a part.
2. **Some parts are critical.** Brakes, tyre, chain, pedal, handlebar and seat are critical: a cycle with one of these broken isn't safe or can't be ridden. Bell and lock aren't critical: the resident is warned, but the cycle can still go out.
3. **One rule decides the state, in one place:** if any critical part is `BROKEN`, the cycle is `UNDER_REPAIR`; otherwise it's `AVAILABLE`. The rule runs at return and when the GS records a repair. It never runs while a cycle is `BOOKED` or `ISSUED`, so a cycle is never taken from someone mid-rental.
4. **Who may set what** (keeps ADR 0003's "classifier is triage, a human decides"):

   | Source | May set a part to |
   |---|---|
   | Resident comment, via classifier | `REPORTED` only |
   | Guard at return (resident ticks what the guard found) | `BROKEN` |
   | GS | `OK` after a repair, or `BROKEN` |

5. **`maintenance_flags` stays the history; `cycle_parts` is the current condition.** Every change to a part also writes a flag row, so the GS can always see who said what and when.

## Consequences

- The state machine has real guards: `ISSUED → UNDER_REPAIR` when the guard finds a broken critical part, `UNDER_REPAIR → AVAILABLE` when the GS repairs the last broken critical part.
- Residents see known issues before booking ("bell reported"), not just "free".
- `MaintenanceService.recordGuardNote(bookingId, note)` becomes `recordInspection(bookingId, brokenParts, note)` and returns the cycle's next state. `confirmReturn` sets that state instead of always `AVAILABLE`. This is a change to the backend–frontend contract, so it goes in its own PR (CLAUDE.md, workflow).
- The return form gains a checklist of parts. It must stay quick: by default nothing is ticked, which means the guard found nothing wrong.
- Adding a ninth part means changing the `Part` enum and the classifier labels together.
- Rejected alternative: 8 condition columns on `cycles` (`brakes_condition`, …). Simpler to read, but the classifier, the guard checklist and the GS screen would each need a branch per column; one row per part lets all three loop over the same list.

## To confirm

- Is the critical / non-critical split right? (Is a broken lock really OK to ride with?)
- Should two or more `REPORTED` comments on the same critical part prompt the GS, or is the GS list enough?
