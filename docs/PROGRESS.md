# Progress log

One entry per week, added before the Sunday push. Newest first. Each entry records what shipped, what AI assistants were used for (course AI-usage policy — we can explain every line), what's blocked, and what's next per [PLAN.md](../PLAN.md).

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
