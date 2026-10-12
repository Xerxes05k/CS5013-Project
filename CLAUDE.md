# CLAUDE.md

Context for AI coding assistants working in this repo. Read this first.

## What this is

Hostel Cycle Booking Web Portal — a CS5013 (IIT Madras) course project. Java/Spring Boot web app that replaces Jamuna Hostel's paper cycle register. See `README.md` for the summary, `docs/ARCHITECTURE.md` for the module design, `PLAN.md` for what's being built this week.

## Hard constraints

- **Language is Java** (course policy). No Python/Node services. The OCR (Tess4j/OpenCV) and NLP (OpenNLP) parts must stay in Java too.
- **Zero cost to the hostel** (Warden's condition). Free-tier hosting only; no hardware; residents use their own phones; the guard has no device.
- **Every line of code must be explainable by the team in a viva.** Don't generate clever abstractions the team can't defend. Prefer plain, readable Spring code over frameworks-on-frameworks.

## Before you change anything

1. Read `docs/ARCHITECTURE.md`. Modules talk only through the listed service interfaces — no cross-module repository access.
2. Check `docs/decisions/` before proposing to change an approach that already has an ADR. If you think an ADR is wrong, write a new ADR that supersedes it; don't silently diverge.
3. Check `PLAN.md` for the current week — don't build week-5 features in week 1.

## Commands

```bash
cd app && ./mvnw spring-boot:run     # run
cd app && ./mvnw test                # tests
scripts/repo-map.sh                  # regenerate docs/REPO_MAP.md
scripts/weekly-update.sh             # weekly commit + push routine
```

## Conventions

- Package root: `in.ac.iitm.cs5013.cyclebooking`. One sub-package per module: `auth`, `idverify`, `rules`, `booking`, `transaction`, `maintenance`, `reporting`, `persistence`.
- Each module exposes one `*Service` interface; implementations are package-private where possible.
- Tests live in `app/src/test/java/...` mirroring main. Every module has at least one test (rubric requirement). Name tests after behaviour: `createBooking_rejectsSecondConcurrentBookingOfSameCycle`.
- Commit messages: short imperative subject, body says *why*. The Sunday log commit uses `Week N: <summary>`.
- Workflow (from week 4): one branch per feature, never a whole module at once (`be/…`, `fe/…`, `docs/…`). Make short commits, one step each (interface → query → implementation → test → page), each compiling and passing tests. Commit after each step; never one large commit at the end. PR into `main` reviewed and merged by the other person. Never commit or push to `main` directly. Owners and the backend/frontend split are in `PLAN.md` → "Team and work split".
- The `*Service` interfaces and the records they return are the contract between backend and frontend. If you change one, update the controllers that use it in the same PR and say so in the PR description.
- Don't store ID card photos. Only the OCR-extracted text (name, roll number, hostel) is persisted. This is a privacy commitment in the proposal.
- SQLite file `app/data/cyclebooking.db` is gitignored. Seed/test data goes in `app/src/main/resources/data.sql`.

## When you finish a piece of work

- Tick the box in `PLAN.md`.
- If it's a decision worth remembering, add an ADR in `docs/decisions/`.
- On Sundays, add the weekly entry to `docs/PROGRESS.md` — including a line on what AI was used for.
