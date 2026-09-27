# Plan

Week-by-week plan from the design doc to the final submission. Each week ends with a push (Sunday). Checkboxes are ticked as work lands; the weekly entry in [docs/PROGRESS.md](docs/PROGRESS.md) records what actually happened.

Module names refer to [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

**Fixed dates:** mid-demo **Fri 9 Oct 2026**, final submission **Fri 6 Nov 2026**.

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

- [ ] **Rule Engine:** `checkEligibility` — 24-hour hold, 3-per-rolling-week; unit tests for 3rd-vs-4th booking and 23h-vs-25h
- [ ] **Booking Module:** `getAvailability`, `createBooking` with DB-level lock; two-thread concurrency test (exactly one wins)
- [ ] **ID Verification Module (prototype):** Tess4j + OpenCV pipeline run on ≥10 sample ID photos (good light / glare / tilt / blur); record extraction accuracy in PROGRESS
- [ ] Sunday push + PROGRESS entry

## Week 3 — 5–9 Oct  ← **mid-demo Fri 9 Oct**

**Goal: the mid-demo workflow — login → scan → book → passcode pickup/return → usage logged.**

- [ ] **ID Verification Module:** integrated into booking; `matchesLogin` cross-check against smail identity; manual roll-number fallback after N failed scans
- [ ] **Pickup/Return Module:** `confirmPickup` / `confirmReturn` with passcode; state-transition tests (wrong passcode = no change)
- [ ] **Reporting Module (v1):** raw usage-hours-per-cycle
- [ ] Mid-demo script rehearsed on a real phone + the guard flow acted out
- [ ] Push before the demo + PROGRESS entry

## Week 4 — 10–16 Oct

**Goal: absorb mid-demo feedback; start the maintenance side.**

- [ ] Fix whatever broke in the mid-demo (log each item here)
- [ ] **Maintenance Comment Module (v1):** post-return comment box, stored; guard condition note at check-in
- [ ] Start the labeled phrase dataset for the classifier (target ≥100 comments across ~8 issue labels)
- [ ] Sunday push + PROGRESS entry

## Week 5 — 17–23 Oct

**Goal: the classifier.**

- [ ] **CommentClassifier:** Apache OpenNLP model trained on the labeled set; precision/recall recorded in PROGRESS
- [ ] `MaintenanceService.submitComment` → flags; raw comment always shown next to flags (ADR 0003)
- [ ] **Reporting Module (v2):** maintenance list combining guard notes + auto-flags
- [ ] Sunday push + PROGRESS entry

## Week 6 — 24–30 Oct

**Goal: deployed and piloted.**

- [ ] Deploy to free-tier host (Render/Railway); ADR 0005 on hosting choice
- [ ] Pilot with the Jamuna GS + one guard shift; log issues
- [ ] Verify usage-hours report against a week of the paper register (verification-plan fixture)
- [ ] Sunday push + PROGRESS entry

## Week 7 — 31 Oct–6 Nov  ← **final Fri 6 Nov**

**Goal: hardening and handover.**

- [ ] GS manual override for rule limits (plan B, risk 4)
- [ ] Fix pilot issues; final test pass (one test per module, all green)
- [ ] Handover doc for GS/guard (how to run, how to add a cycle, how to reset a limit)
- [ ] Final push + PROGRESS entry; README status table updated

---

## Standing weekly routine (every Sunday)

1. `git pull`
2. `scripts/repo-map.sh` — regenerate `docs/REPO_MAP.md`
3. Append the week's entry to `docs/PROGRESS.md` (shipped / AI-assisted / blocked / next)
4. Tick boxes above
5. `git add -A && git commit && git push` (`scripts/weekly-update.sh` does 1–2 and 5)
