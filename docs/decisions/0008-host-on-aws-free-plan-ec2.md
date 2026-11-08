# ADR 0008: Host on an AWS Free plan EC2 instance instead of Azure for Students

**Date:** 2026-11-08 · **Status:** Accepted (the first deploy confirms it) · **Supersedes:** the host choice in [ADR 0007](0007-host-on-azure-for-students-vm.md)

## Context

ADR 0007 chose an Azure for Students VM because it needs no card. The sign-up failed with "access denied", most likely because the institute's Microsoft tenant blocks students from creating Azure subscriptions.

We also looked again at card-free, Vercel-style hosts:
- **Vercel** can't run a long-lived Java server.
- **Render and Hugging Face Spaces** run our Docker image for free, but their free disks are wiped on restart, so every booking would be lost.
- **AWS Educate and the Academy Learner Lab** need no card, but sessions stop after a few hours, so nothing stays up.

The team can provide a card for verification, so the AWS Free plan is now possible:
- accounts opened since July 2025 get $100 of credit at sign-up, plus up to $100 more;
- the Free plan never charges the card;
- the account closes after about 6 months unless upgraded.

## Decision

1. **Run on one EC2 instance under the AWS Free plan:**
   - region Asia Pacific (Mumbai), `ap-south-1`;
   - Ubuntu Server 24.04 LTS, x86;
   - size **`t3.small`** (2 vCPU, 2 GiB), Free-plan eligible;
   - x86, so OpenCV's bundled native library loads; the ARM risk in ADR 0006 doesn't apply.
2. **An Elastic IP** keeps the address fixed across stops and starts. A **free DuckDNS name** points at it, because Let's Encrypt needs a domain name and AWS's own `amazonaws.com` host names aren't a reliable fit for a certificate.
3. **Open only what is needed.** The security group allows SSH (22) from our own IP, and HTTP (80) and HTTPS (443) from anywhere.
4. **Everything else in ADR 0006 stays the same:**
   - the jar under `systemd`, no Docker on the server;
   - Caddy for HTTPS;
   - the database in `/var/lib/cyclebooking/`;
   - the `prod` profile;
   - the resident importer;
   - the GS address in the service file.

   `docs/DEPLOY.md` changes in steps 1 and 2 and in the host name.
5. **A zero-spend budget alert** in AWS Billing, so anything that would cost money shows up by email even though the Free plan uses credits first.

## Consequences

- **Cost to the hostel: zero.** It runs on AWS credits. Our rough estimate is about $20 a month for the instance, its public IPv4 address and a 20 GB disk, so $100 covers the pilot weeks with room to spare. AWS's Billing page shows the real burn.
- **It ends.** After about 6 months the Free plan account closes unless someone upgrades it to paid. Before then, the hostel needs a longer-term home: a department machine, or Azure if the tenant restriction is lifted. Backups (`docs/DEPLOY.md`) make moving easy, since everything is one `.db` file.
- **A team member's card is on the account.** Credits are used first and the Free plan doesn't charge the card, but the budget alert is there in case.
