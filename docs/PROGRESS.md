# Progress log

One entry per week, added before the Sunday push. Newest first. Each entry records what shipped, what AI assistants were used for (course AI-usage policy — we can explain every line), what's blocked, and what's next per [PLAN.md](../PLAN.md).

---

## Week 5 — 25 Oct 2026

*Written late, on Thu 29 Oct. It covers 12–25 Oct. The work merged from 27 Oct on goes in the Week 6 entry.*

**Mid-demo feedback (Mon 12 Oct)**
- We showed our process (prompts, git, testing) but not the system, so it looked as if we did not know it. Faculty asked for:
  - the schema of the objects and their properties;
  - the developer-side architecture and how a request flows through it;
  - a cycle's properties that decide its next state;
  - a less plain frontend: everything was one page, so add tabs;
  - a proper work split, and short commits that build up a feature instead of one large weekly push.

**Shipped**
- **`docs/SCHEMA.md`** (PR #2): ER diagram, every table and column, enums, the records passed between modules, and where each rule is enforced. The column types come from the tables Hibernate actually created, dumped from the SQLite file, not from memory. It records three gaps: SQLite has no foreign keys, "one open rental per resident" holds only because there is one DB connection, and a booking that is never picked up holds its cycle forever.
- **`docs/FLOWS.md`** (PR #2): layered architecture, who calls whom, every HTTP endpoint, and flowcharts for login, booking (rules → ID → lock), the OCR pipeline, the two-resident race, pickup, return and the usage report. Planned steps are drawn dashed so the diagrams don't claim more than the code does.
- **Work split and workflow** (PR #2): Yashas leads backend, Aditya frontend plus some backend tasks; every remaining PLAN item has an owner. From now on: one branch per feature, short commits that each pass the tests, and a PR reviewed and merged by the other person. `scripts/weekly-update.sh` now refuses to run on `main` and pushes a branch.
- **Design for the other two points** (written 12 Oct, merged later in PR #8): ADR 0005 gives each cycle 8 parts with a condition, and a broken critical part means `UNDER_REPAIR`. `docs/UI.md` adds tab navigation and page wireframes.
- **Wed 15 Oct: follow-up session with the TA.** We walked through the schema and the flows using a 13-slide deck built from SCHEMA.md and FLOWS.md.
- No code changed, so the test count is the same as week 4.

**AI-assisted**
- Claude Code wrote SCHEMA.md, FLOWS.md, ADR 0005, UI.md and the TA deck, after dumping the real schema and reading the code. Choices we should be able to defend in the viva:
  - One `cycle_parts` row per part rather than 8 columns on `cycles`, so the classifier, the guard checklist and the GS screen loop over one list.
  - The classifier may only mark a part `REPORTED`; only the guard or the GS can mark it `BROKEN` (ADR 0003: a human decides).
  - The missing foreign keys are written down as a gap rather than silently fixed; turning them on is a PLAN item.

**Blocked**
- **No code was merged from 14 to 25 Oct**, so none of the week-5 items started on time: classifier, cycle parts, GS role.
- The design docs stayed uncommitted on one laptop until 27 Oct, which held up the backend contract that depends on them.

**Next (week 6)**
- Frontend: tab bar, cycle cards, guard confirmation panel, History tab, maintenance list.
- Backend: the ADR 0005 contract first, then cycle parts and the classifier.
- Off localhost: live on a public URL by Fri 30 Oct, then the pilot.

---

## Week 4 — 11 Oct 2026

**Shipped**
- **Mid-demo moved to Mon 12 Oct** (PLAN.md updated). This week's build was done the day before, so it adds features without touching the booking or OCR paths.
- **Guard's condition note at return** (`transaction`, `maintenance`). `confirmReturn` takes its third argument, `guardConditionNote`. The return form has an optional box: the resident asks the guard whether anything is wrong and types what the guard says. A non-blank note becomes a `GUARD` flag on the cycle, with the guard's words in the new `MaintenanceFlag.note` column. It is saved in the same transaction as the return, so either both are saved or neither is. A note over 500 characters is refused before anything changes, the same way a wrong passcode is.
- **Maintenance Comment Module v1** (`maintenance`): `MaintenanceService.submitComment` stores the resident's comment trimmed but otherwise as written. It accepts one comment per booking, only after the return, and refuses blank or over-2000-character text. It raises no flags yet; that is the week-5 classifier. After a confirmed return the resident lands on a new **How was JAM-xx?** page with a comment box and a "Nothing to report, skip" button. The booking id for the comment comes from the session set by the return, never from the form.
- **Labelled phrase dataset started:** `app/src/main/resources/maintenance/labelled-comments.tsv` has **120 rows** across 8 issue labels (chain, brakes, tyre, seat, pedal, handlebar, lock, bell) plus `none`. Every label has ≥ 10 examples, and 9 rows carry two labels. All rows are marked `source=seed`: written for this file, not collected from residents.
- Checked against a running server on a copy of the week-3 demo database: the new `note` column was added to the existing table on startup. Pickup, then return with the note "front brake loose", gave a GUARD flag on JAM-02. An empty comment was refused, and "chain keeps slipping off" was stored against the returned booking. The comment page then closes. The return and comment screens were also checked at phone width (375 px), along with the skip button.
- Tests: **61 across 16 classes, all green** (up from 47). New: 6 Maintenance (raw text stored and no flags yet; refused before return; second comment refused; blank and too long refused; guard note → GUARD flag; blank note → nothing), 3 more Pickup/Return (guard note raises a flag; blank note raises none; too-long note changes nothing), 3 web tests for the comment box (the note reaches `confirmReturn`; the comment goes to the booking just returned; posting a comment without a return, even with a forged `bookingId`, does nothing), and 2 dataset checks (≥ 100 rows, known labels, ≥ 10 per label, no duplicates).

**AI-assisted**
- Claude Code wrote the week-4 code, tests and templates, wrote the 120 seed phrases, and updated this entry, ARCHITECTURE.md and PLAN.md. Choices we should be able to defend in the viva:
  - Any non-blank guard note becomes a flag straight away, without waiting for the classifier. When the guard says something is wrong, the GS should see it whatever the words are.
  - The guard note does **not** move the cycle to `UNDER_REPAIR`. Nothing can clear that state yet (GS tools come in week 5), so a mistyped note would take a cycle out of the pool. The GS decides.
  - The seed phrases were written by the assistant, so classifier numbers measured on them will be optimistic. Week 5 should report precision/recall separately on `pilot` rows once we have them.
- The original Week 4 entry, written earlier today by the scheduled push routine, said "no commits this week". That is replaced by this one.

**Blocked**
- The mid-demo hasn't happened yet (Mon 12 Oct), so "fix whatever broke" stays unticked until there is feedback to log.
- Carried over: the mid-demo rehearsal on a real phone, ≥ 10 consented real ID photos for an accuracy run, and booking cancel / pickup-window expiry. A no-show still blocks the resident; for the demo, finish each booking with pickup and return.
- The comment box is a session gate. If the resident closes the page, the chance to comment on that rental is lost. That is acceptable for v1.

**Next (week 5)**
- Log the mid-demo feedback here and fix what broke.
- CommentClassifier: Apache OpenNLP model trained on `labelled-comments.tsv`, with precision/recall recorded here; `submitComment` then returns real flags.
- Reporting v2: maintenance list combining guard notes and auto-flags, always next to the raw text (ADR 0003); GS-only role; GS clears `UNDER_REPAIR`.
- Booking cancel / pickup-window expiry.

---

## Week 3 — 4 Oct 2026

**Shipped**
- **ID Verification wired into booking** (`idverify`, `booking`). Booking is now two steps: pick a cycle, then photograph your ID card (`capture="environment"` opens the phone's back camera). `IdVerificationService.matchesLogin` accepts the card only if OCR read the resident's own roll number at confidence ≥ 70. The resident sees one of two messages: "could not read it, retake" or "this card shows a different roll number". The photo is held in memory for one request and never written to disk. The multipart threshold equals the size limit, so Tomcat does not spill it to a temp file either.
- **Typed-roll fallback:** after 3 failed scans in a session, the page also offers typing the roll number. Such bookings are stored as `IdCheck.TYPED_ROLL`, and the pickup screen tells the guard to look at the physical card. The controller refuses a typed-roll POST before 3 failed scans.
- **Pickup/Return Module** (`transaction`): `confirmPickup` (BOOKED → ISSUED) and `confirmReturn` (ISSUED → AVAILABLE, sets `returnedAt` and `lastReturnedAt`). The passcode is the resident's login passcode, checked against the BCrypt hash. A wrong passcode or an out-of-order step (return before pickup, pickup twice) is refused and changes nothing. The new **My cycle** page (`/rental`) shows the booking, the due-back time in IST, an overdue warning, and the passcode form for the next step. The booking id always comes from the logged-in resident's open booking, never from the form.
- **Reporting Module v1** (`reporting`): `getWeeklyUsageReport` gives hours out per cycle over the last 7 days. Usage runs from pickup to return. Rentals that straddle the window edge count only their hours inside it, and a cycle still out counts up to now. Page at `/report`; it shows cycles and hours only, no resident names.
- Checked against a running server, with real OCR on synthetic card photos: someone else's card → "different roll number", attempt 1 of 3; low-light photo → "retake", attempt 2 of 3; own card → booked JAM-01; wrong passcode → refused, still awaiting pickup; right passcode → issued, due back in 24 h; return → JAM-01 free again; report lists 1 rental for JAM-01.
- Tests: **39 across 11 classes, all green** (up from 21). New: 5 ID cross-check (own card, threshold edge, someone else's card, unreadable, non-image bytes), 4 more Booking (wrong card, low confidence, typed roll accepted and flagged, wrong typed roll), 6 Pickup/Return state transitions, 1 Reporting (seeded rentals → exactly 4.5 / 4.0 / 5.0 / 0 hours, including window clipping and a cycle still out), and 2 web tests for the 3-failed-scans gate.
- **OCR fixed for a real ID card (6 Oct).** The first real card tried (Yashas's own, used with consent and not committed) was refused as unreadable, even though every character was read correctly. Three causes, all fixed:
  - Tesseract's whole-word score was 42 because a speck was read as a full stop. The roll number is now scored by its weakest character.
  - Full-size phone photos were never shrunk, so the letters were too big for the threshold step. Every photo is now resized to 1600 px wide.
  - The week-2 clean-up alone read the real card at only 3 of 10 sizes and JPEG qualities. OCR now reads three versions (greyscale, contrast-boosted, cleaned) and takes a majority vote.

  On that card the vote reads the roll correctly at **9 of 10** sizes and qualities. The tenth reads CE24B125, which does not match the login and is refused. A scan now takes ~2 s. Synthetic results: blur improved to 3/3 accepted, everything else unchanged, still 0 confidently wrong. Tests: 47, all green.

**AI-assisted**
- Claude Code wrote the week-3 code, tests, templates and this entry, and ran the end-to-end check above with curl against a local server. Choices it made that we should be able to defend in the viva:
  - The Tesseract scanner is created on first use, so the app and tests still start on a machine without Tesseract. There, every scan fails and residents reach the typed-roll fallback.
  - OCR calls are serialised, because one Tesseract instance is not thread-safe.
  - Rules are checked before the ID card, so a resident who can't book anyway is not told to retake a photo.
- The failed-scan counter lives in the HTTP session. That is a usability gate, not a security boundary: someone can reset it by logging in again. The real check for typed-roll bookings is the guard looking at the card.

**Blocked**
- **Mid-demo rehearsal on a real phone + guard flow not done yet.** That PLAN item stays unticked until we actually run it before Fri 9 Oct.
- Real-photo OCR accuracy rests on **one** real card so far (9/10 above). The ≥10 consented real photos are still needed. That card also showed per-character confidence does not catch misreads (an 8 read as a 5 at 94+). The real guard against a wrong booking is the exact match against the logged-in roll number, not the confidence threshold.
- No way yet to cancel a booking, and no expiry for a booking that is never picked up. Until then, a resident who books and doesn't show up stays blocked by "already holding". The architecture's pickup window covers this; it is not built.
- The usage report is open to any logged-in resident. A GS-only role comes with the maintenance list in week 5.

**Next (week 4)**
- Fix whatever breaks in the mid-demo, logged item by item.
- Maintenance Comment Module v1: post-return comment box; guard condition note at return (`confirmReturn` gains its third argument).
- Start the labelled phrase dataset for the classifier (≥100 comments across ~8 labels).
- Booking cancel / pickup-window expiry, so a no-show cannot lock a resident out.

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
