# Hostel Cycle Booking Web Portal

CS5013 course project (IIT Madras, Jul–Nov 2026) by **Yashas Katyal (CE24B128)** and **Aditya Gautam (CE24B001)**.

A Java/Spring Boot web portal that replaces Jamuna Hostel's paper cycle register. Residents log in with their institute (smail) account, verify identity by scanning their ID card (OpenCV + Tesseract OCR), and book a shared cycle from their phone. The data layer enforces the hostel's rules (24-hour hold cap, 3 rentals/week). Pickup and return are confirmed by a passcode entered in front of the guard. Post-return comments are run through a small self-trained classifier that flags maintenance issues ("chain keeps slipping", "needs oiling") for the General Secretary.

**Stakeholders:** Warden, Jamuna & Godavari Hostels; General Secretary, Jamuna Hostel.

## Status

| Milestone | Date | Status |
|---|---|---|
| Proposal | 21 Aug 2026 | Submitted, approved after scope revision |
| Design doc | 11 Sep 2026 | Submitted |
| Mid-demo | 9 Oct 2026 | In progress — see [PLAN.md](PLAN.md) |
| Final submission | 6 Nov 2026 | — |

Weekly progress is logged in [docs/PROGRESS.md](docs/PROGRESS.md). The repo is pushed every week.

## Repository layout

```
CS5013-Project/
├── README.md              this file
├── PLAN.md                week-by-week plan with checkboxes (source of truth for what's next)
├── CLAUDE.md              context + conventions for AI coding assistants working in this repo
├── docs/
│   ├── ARCHITECTURE.md    the 8 modules, their interfaces, and the dependency graph
│   ├── REPO_MAP.md        auto-generated directory map (scripts/repo-map.sh)
│   ├── PROGRESS.md        weekly log: what shipped, what AI was used for, what's next
│   ├── decisions/         architecture decision records (ADRs)
│   └── proposal/          proposal + design doc sources (LaTeX)
├── scripts/
│   ├── repo-map.sh        regenerates docs/REPO_MAP.md
│   └── weekly-update.sh   the weekly commit-and-push routine
└── app/                   (from week 1) the Spring Boot application
```

A full, current map is in [docs/REPO_MAP.md](docs/REPO_MAP.md).

## Tech stack

- **Language:** Java 21+ (course policy), built with Maven
- **Framework:** Spring Boot — Spring MVC + Thymeleaf (server-rendered, mobile-friendly, no app install)
- **Storage:** SQLite via `sqlite-jdbc` + Spring Data JPA
- **Auth:** Spring Security + OAuth2 for institute smail login (OTP fallback if OAuth access is blocked)
- **ID verification:** OpenCV (`javacv`/`bytedeco`) for card detection/deskew, Tesseract OCR (`Tess4j`) for text
- **Maintenance-comment classifier:** Apache OpenNLP, trained on our own labeled phrase set
- **Hosting:** free-tier cloud host (Render/Railway) — no cost to the hostel

## Running (from week 1 onward)

```bash
cd app && ./mvnw spring-boot:run
```

Tests:

```bash
cd app && ./mvnw test
```

## How we work with AI

This is a course on programming with AI. Conventions that keep AI assistants (Claude Code, Copilot, etc.) effective on this codebase:

- **`CLAUDE.md`** at the root gives any assistant the project context, commands, and rules in one place.
- **`docs/ARCHITECTURE.md`** defines module boundaries and interfaces. Assistants are told to read it before changing any cross-module interface.
- **`docs/REPO_MAP.md`** is a regenerated directory map so an assistant can orient without crawling the tree.
- **ADRs in `docs/decisions/`** record *why* decisions were made, so an assistant doesn't re-litigate them.
- **`docs/PROGRESS.md`** logs, per week, what AI was used for — every line of submitted code is code we can explain in the viva.

## Documents

- Proposal: `docs/proposal/main.tex`
- Design doc: `docs/proposal/design-doc.tex`
