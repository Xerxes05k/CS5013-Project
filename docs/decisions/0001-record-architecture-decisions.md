# ADR 0001: Record architecture decisions

**Date:** 2026-09-20 · **Status:** Accepted

## Context

This is a two-person project built with heavy use of AI coding assistants over ~7 weeks. Decisions made in one session (by a person or an assistant) are easily forgotten or re-litigated in the next. The course also requires us to explain every design choice in a viva.

## Decision

We record significant decisions as short ADRs in `docs/decisions/`, numbered sequentially. Each has Context / Decision / Consequences. A superseding decision gets a new ADR that links back; old ADRs are never edited, only marked superseded.

AI assistants are instructed (in `CLAUDE.md`) to read ADRs before proposing changes to an already-decided approach.

## Consequences

- Small overhead per decision (~5 minutes).
- Viva prep is mostly reading this directory.
- Assistants stop suggesting "why not use PostgreSQL / React / a Python OCR service" once the ADR exists.
