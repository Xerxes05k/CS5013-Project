# Plan

Week-by-week plan from the design doc to the final submission. Work lands through small pull requests during the week; each Sunday adds the PROGRESS entry. Checkboxes are ticked as work lands; the weekly entry in [docs/PROGRESS.md](docs/PROGRESS.md) records what actually happened.

Module names refer to [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

**Fixed dates:** mid-demo **Mon 12 Oct 2026** (moved from Fri 9 Oct), final submission **Fri 6 Nov 2026**.

## Team and work split

From week 4 on, after the mid-demo feedback, work is split by layer.

| | Yashas Katyal (CE24B128) | Aditya Gautam (CE24B001) |
|---|---|---|
| Role | **Backend lead** | **Frontend lead**, plus some backend tasks |
| Owns | Service packages `auth`, `idverify`, `rules`, `booking`, `transaction`, `maintenance`, `reporting`, `persistence`; `data.sql`; `application.properties`; deployment | `web/` controllers, `templates/`, `static/css/`, phone and guard-screen UX, controller tests |
| Reviews | Aditya's PRs | Yashas's PRs |

Every item below from week 4 on has a tag: **[BE]** backend, **[FE]** frontend, plus the owner's name. Aditya also takes some **[BE]** items, so both of us write service-layer Java. Owners can swap: change the name in the same PR.

**The contract between the two sides** is the `*Service` interfaces and the records they return ([SCHEMA.md §4](docs/SCHEMA.md#4-objects-that-are-not-tables)). Controllers call only those interfaces, and services never hand JPA entities to the web layer.

**How we coordinate**

1. **Interface first.** For a feature that needs both sides, the backend owner first opens a small PR that adds the interface method and the record it returns. Once it's merged, both sides build in parallel: the backend implements it, the frontend builds the page against it.
2. **One branch per feature, not per module**, named `be/<feature>`, `fe/<feature>` or `docs/<task>`, cut from an up-to-date `main`. A feature is one thing a user or the GS can do, e.g. "GS sees open maintenance flags" or "resident cancels a booking". A module like `maintenance` is built over several such branches.
3. **Short commits that add up to the feature.** Each commit is one step that compiles and passes the tests, e.g. interface method → query → implementation → test → page. Subject in the imperative, body says why. If the subject needs "and", split the commit. Commit during the week as each step works, not in one batch on Sunday.
4. **Pull request, reviewed by the other person**, with `./mvnw test` green. Only the reviewer merges. Nobody pushes to `main` directly.
5. **Changing a contract.** A PR that changes a `*Service` interface or a record must also update the controllers that use it and tag the other person.
6. **New endpoints** get a row in the endpoint table in [docs/FLOWS.md](docs/FLOWS.md#3-http-endpoints) in the same PR.

---

## Week 0 — 20 Sep 2026 (this push)

- [x] Repo scaffolding: README, PLAN, CLAUDE.md, docs/, scripts/
- [x] Architecture doc with module graph, ADRs 0001–0003
- [x] Weekly push routine defined (`scripts/weekly-update.sh`)
- [x] Yashas added as collaborator on the repo (Aditya)
- [ ] Maven installed locally (`brew install maven`)

## Week 1 — 21–27 Sep

**Goal: a running Spring Boot skeleton with the data model persisted.**

- [x] `app/` Spring Boot project via Maven wrapper (`./mvnw`), Thymeleaf, Spring Data JPA, SQLite
- [x] **Persistence Module:** entities `Resident`, `Cycle`, `Booking`, `Comment`, `MaintenanceFlag`; repositories; round-trip test
- [x] **Auth Module:** decide OAuth2 vs OTP (ADR 0004) — try smail OAuth2 first; wire whichever works; 401-on-protected-route test
- [x] Availability page (read-only list of cycles) renders on a phone
- [x] Sunday push + PROGRESS entry

## Week 2 — 28 Sep–4 Oct

**Goal: booking works end-to-end with rules enforced; OCR proven outside the app.**

- [x] **Rule Engine:** `checkEligibility` — 24-hour hold, 3-per-rolling-week; unit tests for 3rd-vs-4th booking and 23h-vs-25h
- [x] **Booking Module:** `getAvailability`, `createBooking` with DB-level lock; two-thread concurrency test (exactly one wins)
- [ ] **ID Verification Module (prototype):** Tess4j + OpenCV pipeline run on ≥10 sample ID photos (good light / glare / tilt / blur); record extraction accuracy in PROGRESS
- [x] Sunday push + PROGRESS entry

## Week 3 — 5–9 Oct  ← **mid-demo Fri 9 Oct**

**Goal: the mid-demo workflow — login → scan → book → passcode pickup/return → usage logged.**

- [x] **ID Verification Module:** integrated into booking; `matchesLogin` cross-check against smail identity; manual roll-number fallback after N failed scans
- [x] **Pickup/Return Module:** `confirmPickup` / `confirmReturn` with passcode; state-transition tests (wrong passcode = no change)
- [x] **Reporting Module (v1):** raw usage-hours-per-cycle
- [ ] Mid-demo script rehearsed on a real phone + the guard flow acted out
- [x] Push before the demo + PROGRESS entry

## Week 4 — 10–16 Oct

**Goal: absorb mid-demo feedback; start the maintenance side.**

- [ ] Fix whatever broke in the mid-demo (log each item here)
  - Mid-demo feedback (12 Oct): show the technical side, not just process; split work properly; commit step by step, not weekly
  - [x] Data schema: ER diagram, tables, enums, records, integrity rules (`docs/SCHEMA.md`)
  - [x] Developer architecture and flowcharts for every request (`docs/FLOWS.md`)
  - [x] Work split: backend / frontend owners and the PR workflow (see "Team and work split" above)
  - [ ] Switch to small commits per change, merged through reviewed PRs: first PR is these docs
- [x] **Maintenance Comment Module (v1):** post-return comment box, stored; guard condition note at check-in
- [x] Start the labeled phrase dataset for the classifier (target ≥100 comments across ~8 issue labels)
- [ ] Sunday PROGRESS entry (PR)

## Week 5 — 17–23 Oct

**Goal: the classifier.**

- [ ] **[BE · Yashas] CommentClassifier:** Apache OpenNLP model trained on the labeled set; precision/recall recorded in PROGRESS
- [ ] **[BE · Yashas]** `MaintenanceService.submitComment` → flags (ADR 0003)
- [ ] **[FE · Aditya]** after a comment, show the raw comment next to the flags it raised
- [ ] **[BE · Aditya] Reporting Module (v2):** `getMaintenanceList` combining guard notes + auto-flags; GS can mark a flag resolved
- [ ] **[FE · Aditya]** GS maintenance list page: open flags per cycle, raw comment / guard words, "resolved" button
- [ ] **[BE · Yashas]** GS role in `SecurityConfig`: only the GS sees `/report` and the maintenance list
- [ ] Sunday PROGRESS entry (PR)

## Week 6 — 24–30 Oct

**Goal: deployed and piloted.**

- [ ] **[BE · Yashas]** Deploy to free-tier host (Render/Railway); ADR 0005 on hosting choice
- [ ] **[FE · Aditya]** Phone pass before the pilot: every page on a real phone, camera opens straight from the ID-scan page, guard can read the pickup/return confirmation at arm's length
- [ ] **[both]** Pilot with the Jamuna GS + one guard shift; log issues
- [ ] **[BE · Aditya]** Verify usage-hours report against a week of the paper register (verification-plan fixture)
- [ ] Sunday PROGRESS entry (PR)

## Week 7 — 31 Oct–6 Nov  ← **final Fri 6 Nov**

**Goal: hardening and handover.**

- [ ] **[BE · Yashas]** GS manual override for rule limits (plan B, risk 4): service method + test
- [ ] **[FE · Aditya]** GS override page
- [ ] **[both]** Fix pilot issues, each in its own PR; final test pass (one test per module, all green)
- [ ] **[both]** Handover doc for GS/guard (how to run, how to add a cycle, how to reset a limit)
- [ ] Final PROGRESS entry; README status table updated

## Not scheduled yet

Gaps found while writing [docs/SCHEMA.md](docs/SCHEMA.md#5-integrity-rules-and-where-each-is-enforced). Pick a week for each or drop it, and say so here.

- [ ] **[BE]** Booking cancel and no-show expiry: a booking never picked up holds the cycle forever. **[FE]** cancel button on "My cycle".
- [ ] **[BE]** Turn on SQLite foreign keys (`PRAGMA foreign_keys = ON`) or record in an ADR why not

---

## Standing weekly routine (every Sunday)

Code goes in through PRs during the week. Sunday is only for the log:

1. `git switch main && git pull && git switch -c docs/week-N`
2. Append the week's entry to `docs/PROGRESS.md` (shipped / AI-assisted / blocked / next), with a line per person
3. Tick boxes above
4. `scripts/weekly-update.sh N "<summary>"`: regenerates `docs/REPO_MAP.md`, runs the tests, commits, pushes the branch
5. Open the PR; the other person reviews and merges
