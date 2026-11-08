# Hostel Cycle Booking Web Portal

CS5013 course project (IIT Madras, Jul–Nov 2026) by **Yashas Katyal (CE24B128)** and **Aditya Gautam (CE24B001)**.

A Java/Spring Boot web portal that replaces Jamuna Hostel's paper cycle register. Residents log in with their institute (smail) account, verify identity by scanning their ID card (OpenCV + Tesseract OCR), and book a shared cycle from their phone. The data layer enforces the hostel's rules (24-hour hold cap, 3 rentals/week). Pickup and return are confirmed by a passcode entered in front of the guard. At return the guard says which parts are broken. A broken critical part (brakes, tyre, chain, …) takes the cycle out of the pool until the General Secretary records the repair (ADR 0005). Post-return comments are stored and shown to the GS; the classifier that would flag issues from them was planned but cut (see PROGRESS.md).

**Stakeholders:** Warden, Jamuna & Godavari Hostels; General Secretary, Jamuna Hostel.

## Status

| Milestone | Date | Status |
|---|---|---|
| Proposal | 21 Aug 2026 | Submitted, approved after scope revision |
| Design doc | 11 Sep 2026 | Submitted |
| Mid-demo | 12 Oct 2026 | Done. Feedback and what changed: [PROGRESS.md](docs/PROGRESS.md), Week 5. Schema: [SCHEMA.md](docs/SCHEMA.md). Flows: [FLOWS.md](docs/FLOWS.md) |
| Live on a public URL | 30 Oct 2026 | Missed, then **live on 8 Nov 2026: https://3-26-193-82.sslip.io** (AWS Free plan EC2 instance, [ADR 0008](docs/decisions/0008-host-on-aws-free-plan-ec2.md)). Loaded with placeholder residents CE24B001-CE24B130 and 8 sample cycles until the GS sends the real list |
| Final submission | 6 Nov 2026 | Code and docs final on `main` (5 Nov). What was built and what was cut: [PROGRESS.md](docs/PROGRESS.md), final entry. How to use it: [HANDOVER.md](docs/HANDOVER.md) |

Weekly progress is logged in [docs/PROGRESS.md](docs/PROGRESS.md). Since the mid-demo, every change goes in through a short pull request, reviewed by the other team member until 5 Nov, when Aditya handed the last work to Yashas (see "Team and work split" in [PLAN.md](PLAN.md)).

## Repository layout

```
CS5013-Project/
├── README.md              this file
├── PLAN.md                week-by-week plan with checkboxes (source of truth for what's next)
├── CLAUDE.md              context + conventions for AI coding assistants working in this repo
├── deploy/                systemd unit and Caddyfile for the live server
├── docs/
│   ├── ARCHITECTURE.md    the 8 modules, their interfaces, and the dependency graph
│   ├── SCHEMA.md          tables, records and where each rule is enforced
│   ├── FLOWS.md           request flowcharts and the endpoint table
│   ├── DEPLOY.md          setting up and redeploying the live server
│   ├── HANDOVER.md        how residents, guards and the GS use it
│   ├── REPO_MAP.md        auto-generated directory map (scripts/repo-map.sh)
│   ├── PROGRESS.md        weekly log: what shipped, what AI was used for, what's next
│   ├── decisions/         architecture decision records (ADRs)
│   └── proposal/          proposal + design doc sources (LaTeX)
├── scripts/
│   ├── repo-map.sh        regenerates docs/REPO_MAP.md
│   └── weekly-update.sh   the weekly commit-and-push routine
└── app/                   the Spring Boot application
    └── src/main/java/in/ac/iitm/cs5013/cyclebooking/
        ├── persistence/   entities + repositories
        ├── auth/          smail login (ADR 0004)
        ├── idverify/      ID card OCR + check against the login
        ├── rules/         24-hour hold, 3 per rolling week
        ├── booking/       availability, booking, the race-safe lock
        ├── transaction/   passcode pickup and return
        ├── maintenance/   comments, guard findings, cycle parts and repairs
        ├── reporting/     usage hours and the GS maintenance list
        └── web/           Thymeleaf controllers
```

A full, current map is in [docs/REPO_MAP.md](docs/REPO_MAP.md).

## Tech stack

- **Language:** Java 25 (course policy), built with Maven
- **Framework:** Spring Boot — Spring MVC + Thymeleaf (server-rendered, mobile-friendly, no app install)
- **Storage:** SQLite via `sqlite-jdbc` + Spring Data JPA
- **Auth:** Spring Security form login with smail address + BCrypt passcode (ADR 0004; institute OAuth2 was not available)
- **ID verification:** OpenCV (`javacv`/`bytedeco`) for card detection/deskew, Tesseract OCR (`Tess4j`) for text
- **Maintenance-comment classifier:** Apache OpenNLP on our own labelled phrase set (planned, cut; the 120-row set is in the repo)
- **Hosting:** AWS Free plan EC2 instance, jar under systemd, Caddy for HTTPS, no cost to the hostel (ADR 0006, ADR 0008, [DEPLOY.md](docs/DEPLOY.md))

## Running

```bash
cd app && ./mvnw spring-boot:run
```

Then open http://localhost:8080 — it redirects to the login page. A dev resident and five cycles are seeded on an empty database; the credentials are in `app/src/main/resources/data.sql`. No global Maven install is needed (the wrapper handles it), but Java 25 is.

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
- Deploying: [docs/DEPLOY.md](docs/DEPLOY.md)
- Using it (residents, guards, GS): [docs/HANDOVER.md](docs/HANDOVER.md)
