# Progress log

One entry per week, added before the Sunday push. Newest first. Each entry records what shipped, what AI assistants were used for (course AI-usage policy — we can explain every line), what's blocked, and what's next per [PLAN.md](../PLAN.md).

---

## Week 2 — 4 Oct 2026

**Shipped**
- **Rule Engine** (`rules`): `RuleService.checkEligibility` enforces at most 3 bookings per rolling 7 days and the 24-hour hold. A resident still holding a cycle is refused, with a separate "overdue" reason once it is past 24 h. Time comes from an injected `Clock`, so the tests hit each boundary exactly.
- **Booking Module** (`booking`): `BookingService.getAvailability` / `createBooking`. The lock is one conditional `UPDATE … WHERE state = AVAILABLE` (`CycleRepository.claimIfAvailable`): when two residents race, the database lets exactly one statement match the row. Residents see rejections as readable messages.
- **Auth Module:** `AuthService.currentResident` maps the logged-in smail address to a resident id, so the web layer no longer touches repositories.
- **Availability page:** free cycles now have a **Book** button (POST, CSRF-protected, post-redirect-get), with success and refusal messages shown on the page. Checked against a running server: log in, book JAM-01 → "Booked JAM-01"; try JAM-02 → refused with "You already have a cycle booked or in use".
- **ID Verification prototype** (`idverify`, not yet wired into booking): OpenCV preprocessing (upscale, find the card outline and deskew, median denoise, CLAHE, adaptive threshold), then Tesseract via Tess4j, then a roll-number parser that fixes O/0, I/1, S/5 and B/8 confusions by position. The photo bytes are never written anywhere.
- OCR accuracy on **18 synthetic card photos** (3 fake cards × 6 conditions, drawn in memory by the test — no real ID photos), confidence threshold 70:

| Condition | Roll read correctly | Accepted (conf ≥ 70) | Mean conf | Confidently wrong |
|---|---|---|---|---|
| Good light | 3/3 | 3/3 | 91 | 0 |
| Glare | 3/3 | 3/3 | 92 | 0 |
| Tilt (7°) | 3/3 | 3/3 | 90 | 0 |
| Blur (mild) | 3/3 | 2/3 | 75 | 0 |
| Low light + noise | 0/3 | 0/3 | 0 | 0 |
| Severe blur | 0/3 | 0/3 | 0 | 0 |

  The safety property held: no photo produced a wrong roll number at or above the threshold. Failed photos come back empty or low-confidence, which in week 3 becomes "retake the photo".
- Tests: 21 across 7 classes, all green — 5 Rule Engine (3rd vs 4th booking, 7-day window edge, 23 h vs 25 h), 4 Booking including the two-thread race (exactly one wins), 4 OCR text parser, and the OCR accuracy run (skipped rather than failed on machines without Tesseract).

**AI-assisted**
- Claude Code wrote the rules, booking, auth-service and idverify code, their tests, and this entry. It debugged two problems rather than guessing. First, Tess4j 5.20's lept4j 1.24 binds Leptonica symbols newer than Homebrew's 1.85 (`UnsatisfiedLinkError: pixFindBaselinesGen`), so lept4j is pinned to 1.21.1. Second, it looked at the preprocessed images to find why tilt failed: the deskew measured angles from all dark pixels, including the table under the card. It now measures the card outline. The same image review showed glare failing because of a global (Otsu) threshold, now an adaptive one.
- Tuning was time-boxed. Moving the denoise ahead of the upscale was tried, made tilt and blur worse, and was reverted. Low light remains unsolved.

**Blocked**
- The OCR numbers are from **synthetic** cards. Real-photo accuracy needs ≥10 consented photos of real ID cards, run locally and never committed (`docs/samples/real/` is gitignored). We have not collected them yet, so the PLAN item stays unticked.
- Low-light photos fail outright (0/3). They fail safely, but week 3 needs either a better denoise or the manual roll-number fallback to carry this case.
- Tesseract is a native dependency (`brew install tesseract`). The free-tier host chosen in week 6 must provide it; this goes into ADR 0005.

**Next (week 3)**
- ID Verification integrated into booking: `matchesLogin` cross-check against the smail identity; manual roll-number fallback after N failed scans; `createBooking` takes the `ExtractedIdentity`.
- Pickup/Return Module (`confirmPickup` / `confirmReturn` with passcode) and Reporting v1 (usage hours per cycle).
- Collect consented real ID photos for a real accuracy run.
- Rehearse the mid-demo on a real phone; push before Fri 9 Oct.

---

## Week 1 — 27 Sep 2026

**Shipped**
- `app/` — Spring Boot 4.1.1 on Java 25, Maven wrapper, Thymeleaf, Spring Data JPA, SQLite (`sqlite-jdbc` + Hibernate community dialect).
- **Persistence Module:** `Resident`, `Cycle`, `Booking`, `Comment`, `MaintenanceFlag` entities with the five repositories. `Cycle` carries a JPA `@Version` for the week-2 booking race; `Booking.isOverdue` derives the 24-hour rule rather than storing a stale flag. `BookingRepository` already has the count query the Rule Engine needs.
- **Auth Module:** smail-address + BCrypt passcode form login (`SecurityConfig`, `ResidentDetailsService`). ADR 0004 records why, not OAuth2, and how the switch stays cheap.
- **Availability page:** `/cycles` renders the pool phone-first (single column, touch-sized rows, no CSS framework); `/login` is the only public route.
- 7 tests, all green: Booking round-trip + open-booking lifecycle + the 23h/25h overdue boundary, and 3 auth tests (anonymous redirect, login public, authenticated render).
- Verified end-to-end against a running server: form login → redirect to `/cycles` → seeded pool renders "3 of 5 free" with correct per-cycle states.

**AI-assisted**
- Claude Code wrote the entity/repository/config/test code and this entry. Three failures it had to diagnose rather than guess: Spring Initializr labels Boot `4.1.1.RELEASE` but Maven Central publishes `4.1.1`; Boot 4 moved the test-slice annotations (`DataJpaTest` → `org.springframework.boot.data.jpa.test.autoconfigure`, `WebMvcTest` → `org.springframework.boot.webmvc.test.autoconfigure`); and SQLite will not create a missing parent directory for its file.
- Decision made by us, not the assistant: tests run against real SQLite (`@AutoConfigureTestDatabase(replace = NONE)` + shared-cache in-memory) instead of H2, because the week-2 concurrency behaviour is dialect-specific and H2 would test the wrong database.

**Blocked**
- Nothing blocking. Institute OAuth2 remains unavailable (ADR 0004) but is no longer on the critical path.
- Maven is still not installed globally; the wrapper (`./mvnw`) covers it, so this is not worth fixing.

**Next (week 2)**
- Rule Engine (`checkEligibility`: 24-hour hold, 3-per-rolling-week) with the 3rd-vs-4th and 23h-vs-25h unit tests.
- Booking Module `createBooking` with the two-thread concurrency test (exactly one wins).
- ID Verification prototype: Tess4j + OpenCV over ≥10 sample ID photos, extraction accuracy recorded here.

---

## Week 0 — 20 Sep 2026

**Shipped**
- Repository scaffolding: `README.md`, `PLAN.md` (weeks 0–7, tied to mid-demo 9 Oct and final 6 Nov), `CLAUDE.md`.
- `docs/ARCHITECTURE.md` — the 8 modules and interfaces from the design doc, plus the module dependency graph, cycle state machine, entity table, and per-module test plan.
- ADRs 0001–0003 (recording decisions; Spring Boot + Thymeleaf + SQLite in one JVM; OCR at booking time and classifier as triage).
- `scripts/repo-map.sh` (regenerates `docs/REPO_MAP.md`) and `scripts/weekly-update.sh` (the Sunday routine).
- Proposal and design doc LaTeX sources under `docs/proposal/`.

**AI-assisted**
- Claude Code drafted the README, PLAN, CLAUDE.md, ARCHITECTURE.md, ADRs, and both scripts from the proposal and design doc. Reviewed by Yashas.

**Blocked**
- Push access: Yashas's GitHub account (`yashas236`) is not yet a collaborator on `Xerxes05k/CS5013-Project`. Aditya to add.
- Maven not installed on Yashas's machine (`brew install maven`); week 1 will use the Maven wrapper so this only matters for generating the project.

**Next (week 1)**
- Spring Boot skeleton under `app/`, Persistence Module entities + round-trip test, Auth Module decision (OAuth2 vs OTP → ADR 0004), read-only availability page on a phone.
