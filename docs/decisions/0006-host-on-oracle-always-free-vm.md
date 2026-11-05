# ADR 0006: Host on an Oracle Cloud Always Free VM, jar under systemd, Caddy for HTTPS

**Date:** 2026-11-03 · **Status:** Superseded in part by [ADR 0007](0007-host-on-azure-for-students-vm.md) (5 Nov 2026): Oracle needs a credit card, which neither of us has, so the VM is on Azure for Students. Decisions 2-7 below still apply.

## Context

Until now the app has only run on our laptops. Residents use their own phones, so the pilot needs a public address. PLAN.md (week 6) sets three conditions for the host:

1. **Costs the hostel nothing** (the Warden's condition).
2. **Keeps files across restarts and redeploys.** The database is one SQLite file on disk (ADR 0002). A host that wipes its disk on restart loses every booking.
3. **Serves HTTPS.** Passcodes are sent at login and at the stand.

We checked the free tiers on 3 Nov 2026. Comparison sites disagree on details, so the first deploy is the real check.

| Host | Free? | Disk survives restarts? | Notes |
|---|---|---|---|
| Render (free web service) | Yes | **No.** Free services have no persistent disk | Also sleeps after 15 min idle |
| Fly.io | **No** for new accounts (short trial only) | Volumes, but paid | Old free allowance only for older accounts |
| Koyeb | Card required, no standing free compute | — | Changed in 2026 |
| **Oracle Cloud Always Free VM** | **Yes**, no time limit | **Yes.** Up to 200 GB of block storage | A card is needed to sign up, for verification only |
| Hostel or department machine | Yes | Yes | Needs someone to keep it on; we don't control one |

## Decision

1. **Run on an Oracle Cloud Always Free VM** with Ubuntu 24.04. This is the only free option found that keeps a disk.
2. **No Docker.** Install Java 25 and `tesseract-ocr` on the VM, copy the jar built by `./mvnw package`, and run it as a `systemd` service (`deploy/cyclebooking.service`). It restarts on crash and at boot. Fewer moving parts than a container, and each one can be explained in the viva.
3. **Caddy in front for HTTPS** (`deploy/Caddyfile`). It gets and renews a free Let's Encrypt certificate by itself and forwards to the app on `127.0.0.1:8080`. The app is not reachable except through Caddy.
4. **A free DuckDNS name** (e.g. `jamuna-cycles.duckdns.org`) points at the VM, because Let's Encrypt needs a domain name.
5. **The database lives in `/var/lib/cyclebooking/`**, which `systemd` creates, outside the app folder, so a redeploy (replacing the jar) never touches it.
6. **Production runs with the `prod` profile** (`application-prod.properties`):
   - no `data.sql` seeding, so the dev login with its published passcode never reaches the server;
   - the database path above;
   - secure session cookies;
   - trust Caddy's forwarded headers.
7. **Residents are loaded with the resident importer:** a CSV in, a random passcode per resident out (`docs/DEPLOY.md`). Cycles are added with one SQL line each.

**Fallback:** if Oracle signup fails, run the same jar on a hostel or department machine, with a free Cloudflare Tunnel for HTTPS. Steps 2 and 5–7 stay the same.

## Consequences

- **Cost to the hostel: zero.** One of us signs up with a card for verification; nothing is charged within Always Free limits.
- **The VM is ARM (Ampere).** Java and Tesseract run fine there, but OpenCV's bundled native library might not. If OCR fails to load, `scanCard` already turns that into "unreadable", and residents reach the typed-roll fallback after 3 tries. The smoke test must try a real scan to find out.
- **Our own server to look after:** OS updates and disk space. For a few weeks' pilot that is acceptable, and `docs/DEPLOY.md` lists the commands.
- **Backups are manual:** copy the one `.db` file. A nightly `cp` to a dated file is in the guide.
