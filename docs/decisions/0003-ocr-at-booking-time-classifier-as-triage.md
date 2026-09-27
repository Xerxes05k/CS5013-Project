# ADR 0003: OCR runs at booking time on the resident's phone; the comment classifier is a triage aid, not a gate

**Date:** 2026-09-20 · **Status:** Accepted

## Context

Two of the riskiest components are the ID-card OCR pipeline and the self-trained maintenance-comment classifier. Both will sometimes be wrong: OCR fails on glare, tilt, worn cards; a small classifier trained on ~100 comments will miss issues and produce false positives on short or mixed-language text.

An earlier draft of the design had the guard scanning the card at the cycle stand with a shared tablet. That put the OCR failure mode inside a live, 10-second, guard-is-waiting interaction, and required hardware the hostel doesn't have.

## Decision

1. **OCR happens at booking time, on the resident's own phone.** A bad photo is simply retaken. After N failed scans the resident can type their roll number manually; it is still cross-checked against the resident database and the smail login. The guard's step is only the passcode confirmation, which has no vision component.
2. **The ID photo is never stored.** Only the extracted name / roll number / hostel is persisted.
3. **The classifier never hides the comment.** `MaintenanceService` returns flags *and* the raw text, and the GS's maintenance list always shows both. The classifier prioritises; a human decides.
4. **The GS can override rule-engine limits** case by case (e.g. a cycle stuck past 24 h because of a broken lock).

## Consequences

- The 10-second acceptance criterion applies only to the passcode step, which is realistic.
- OCR accuracy becomes a quality-of-life metric (how often residents need a retake) rather than a correctness gate. We record it in `docs/PROGRESS.md` from week 2.
- Classifier precision/recall is reported honestly; a mediocre number does not make the feature useless because the raw comment is right there.
- No guard device, no hardware cost — consistent with the Warden's condition.
