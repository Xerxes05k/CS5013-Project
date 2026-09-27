# ADR 0004: Log in with smail address + passcode now; keep the OAuth2 door open

**Date:** 2026-09-27 · **Status:** Accepted

## Context

The proposal and design doc specify institute (smail) login via Spring Security + OAuth2, with "smail address plus OTP" recorded as plan B for risk 2 ("setting up institute smail OAuth login may need IT/domain approval we don't control on this timeline").

Week 1 was the point to decide. Institute OAuth2 requires a Google Cloud OAuth client registered against the `smail.iitm.ac.in` domain and a consent screen approved by institute IT. We do not control that, we have not been granted it, and waiting on it would block every downstream module (booking, rules, pickup/return) that needs to know who the resident is.

## Decision

Ship **smail address + passcode** login now, using Spring Security form login with BCrypt-hashed passcodes stored on `Resident`.

Crucially, the **smail address is the username** — the same identity key OAuth2 would produce. So the switch later is a change of authentication mechanism only: `ResidentDetailsService` resolves a resident from a smail address either way, and existing bookings keep resolving to the same person.

The passcode serves double duty: it is also the passcode the resident enters in front of the guard at pickup/return, so there is one secret for the resident to remember rather than two.

## Consequences

- Week 2 onward is unblocked; nothing waits on an IT ticket.
- We must handle passcode issuance and reset ourselves before the pilot (week 6). Currently the seed file carries one dev passcode; real per-resident passcodes are generated at pilot setup.
- If IT access does arrive, adding OAuth2 is additive: register the client, add the `oauth2Login` block to `SecurityConfig`, keep `ResidentDetailsService` as is.
- The proposal's risk-2 plan B said "OTP". A passcode is a closer fit than an emailed OTP: it is already required for the guard-witnessed step, and an OTP round-trip at the cycle stand would need working network and email exactly where coverage is weakest.
