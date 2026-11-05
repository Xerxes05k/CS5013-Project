# ADR 0007: Host on an Azure for Students VM instead of Oracle Cloud

**Date:** 2026-11-05 · **Status:** Accepted (the first deploy confirms it) · **Supersedes:** the host choice in [ADR 0006](0006-host-on-oracle-always-free-vm.md)

## Context

ADR 0006 chose an Oracle Cloud Always Free VM. Oracle asks for a credit card at sign-up, even though nothing is charged. Neither of us has one, so we can't sign up.

The three conditions from ADR 0006 still hold:
- it must cost the hostel nothing;
- the disk must survive restarts (the database is one SQLite file);
- it must serve HTTPS.

We looked again on 5 Nov 2026 for a host that meets all three **without a card**:

| Host | Card needed? | Disk survives restarts? | Notes |
|---|---|---|---|
| Oracle Cloud Always Free | **Yes** | Yes | The reason for this ADR |
| Render (free) | No | **No** | Free services have no persistent disk |
| Fly.io, Koyeb, Google Cloud, DigitalOcean (student pack) | **Yes** | — | |
| Alwaysdata (free) | No | Yes, but 100 MB in total | Too small for the jar plus Tesseract; no root, so we can't install Tesseract |
| **Azure for Students** | **No.** You verify with a university email | **Yes.** A managed disk, like any VM | $100 credit for 12 months; B1s VMs are in the free services |
| Hostel or department machine | No | Yes | Nobody keeps one on for us |

## Decision

1. **Run on an Azure for Students VM:** Ubuntu 24.04, size **B1s** (1 vCPU, 1 GiB RAM, x86), region Central India. One of us signs up with their smail address; IIT Madras students qualify.
2. **Everything else in ADR 0006 stays the same:**
   - the jar under `systemd`, no Docker;
   - Caddy in front for HTTPS;
   - the database in `/var/lib/cyclebooking/`;
   - the `prod` profile;
   - the resident importer.

   `docs/DEPLOY.md` changes only in steps 1 and 2.
3. **The name is the VM's free Azure DNS label** (`jamuna-cycles.centralindia.cloudapp.azure.com`), set on its public IP. Let's Encrypt issues certificates for it, so DuckDNS isn't needed.
4. **A 2 GB swap file**, because 1 GiB is tight for Java plus Tesseract. The JVM is capped at `-Xmx512m` (already in the service file).

**Fallback, for the demo only:** if the Azure student check fails, run the `prod` jar on a laptop and expose it with a Cloudflare quick tunnel (`cloudflared tunnel --url http://localhost:8080`). That needs no account and gives an HTTPS `trycloudflare.com` address. The laptop has to stay on, though, and the address changes on every restart. That is fine for a demo, but not for a pilot.

## Consequences

- **Cost to the hostel: zero, and no card from anyone.** The B1s, its disk and its public IP fit in the free services, and the $100 credit covers anything over. When the credit runs out the subscription stops; it does not bill.
- **It lasts a year at a time.** Azure for Students runs 12 months and can be renewed while the account holder is a student. For a handover to future GSs, the hostel would have to move the database file (one file, `docs/DEPLOY.md` → Backups) to whoever hosts it next.
- **x86, not ARM,** so ADR 0006's worry about OCR native libraries on ARM no longer applies.
- **1 GiB of RAM** is the tightest part (the other free sizes, B2ats v2 and B2pts v2, also have 1 GiB). If the app is killed for memory, `journalctl` shows it; the fix is a bigger swap file or a lower `-Xmx`.
