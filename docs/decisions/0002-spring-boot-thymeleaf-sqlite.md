# ADR 0002: Spring Boot + Thymeleaf + SQLite, everything in one Java process

**Date:** 2026-09-20 · **Status:** Accepted

## Context

Course policy mandates Java. The Warden's condition is zero cost to the hostel. Residents will use their own phones; the guard has no device. We are two people with ~7 weeks, and every line must be explainable in a viva.

## Decision

- One Spring Boot application, server-rendered with **Thymeleaf**. No separate frontend build, no app store install — residents open a URL on their phone.
- **SQLite** via `sqlite-jdbc` + Spring Data JPA. A single file database is enough for one hostel's cycle pool and removes a managed-database dependency on the free-tier host.
- **OCR (Tess4j + OpenCV) and NLP (OpenNLP) run inside the same JVM.** No Python sidecar, even though Python tooling for both is more common — it would break the Java-only policy and add a second deployable.
- Maven with the wrapper (`./mvnw`) so the build works on any machine without a global Maven install.

## Consequences

- Simplest possible deploy: one jar, one SQLite file.
- SQLite's write serialisation is actually a feature here — it makes the "two residents book the same cycle" lock straightforward (row lock inside a transaction).
- Tess4j/OpenCV native binaries bloat the jar (~100 MB). Acceptable on Render/Railway free tiers; we will verify in week 6.
- If the free-tier host has no persistent disk, the SQLite file needs a volume — checked in ADR 0005 (hosting, week 6).
