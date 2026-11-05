# Handover: using and running the cycle portal

For the residents, guards and GS of Jamuna Hostel, and for whoever looks after the server next. How to set up and redeploy the server is in [DEPLOY.md](DEPLOY.md); this page covers day-to-day use.

---

## Residents

Open the site on your phone and log in with your smail address and the passcode the GS gave you. The passcode is also what you type at the cycle stand, so keep it to yourself.

**Book a cycle**
1. **Cycles** tab: each card says whether the cycle is Free, Booked, In use or Under repair. Known problems (e.g. "Known issues: bell") show on the card, and tapping a card shows all 8 parts.
2. Tap **Book** on a free cycle, then photograph your IIT Madras ID card. Keep it flat, filling the frame, in good light. The photo is read once and never saved.
3. If the card won't read after 3 tries, you can type your roll number instead. The guard will then check your physical card at pickup.

**Rules:** one cycle at a time, at most 24 hours per rental, at most 3 rentals in any 7 days.

**Changed your mind?** On **My cycle**, tap **Cancel this booking** any time before pickup. The cycle is freed and the booking doesn't count towards your 3.

**Pickup:** at the stand, open **My cycle** and type your passcode while the guard watches. A large green panel tells the guard to hand over the cycle.

**Return**
1. Bring it back within 24 hours and ask the guard whether anything is wrong with it.
2. Tick the parts the guard says are broken (leave all unticked if nothing). Type anything else the guard says in the box.
3. Type your passcode while the guard watches. The green panel tells the guard the return is recorded.
4. You can then leave a comment about the ride ("chain keeps slipping"), or skip.

**History** tab: all your past rentals.

## Guards

You don't need a phone or a login. The resident does everything on theirs, in front of you.

- **At pickup:** wait for the large green panel naming the cycle, then hand that cycle over. If the resident's page says *"Guard: this booking was made without a card scan"*, look at their ID card first.
- **At return:** look the cycle over and tell the resident which parts are broken: tyre, brakes, chain, seat, pedal, handlebar, lock or bell. They tick them on their phone. Wait for the green panel before they leave.
- If any of the first six (tyre to handlebar) is broken, the cycle goes to *Under repair* and nobody can book it until the GS records the repair. A broken lock or bell only shows as a warning.

## GS (and Warden)

Your smail address is set on the server as the GS (DEPLOY.md §4), so you see a **Report** tab that other residents don't.

- **Usage:** hours each cycle was out in the last 7 days. Cycles and hours only, no resident names.
- **Maintenance:** every open problem, grouped by cycle, with the guard's or resident's own words.
  - Once a part is fixed, press **Repaired** next to it. The part goes back to OK, the problem leaves the list, and the page tells you whether the cycle is free to book again. It stays under repair if another critical part is still broken.
  - A free-text guard note has no Repaired button. It is there for you to read.
  - Resident comments are stored and shown but don't raise problems by themselves. The comment classifier was not built (PROGRESS.md, final entry).

**Adding cycles and residents:** this is done on the server by whoever runs it ([DEPLOY.md §5](DEPLOY.md#5-load-cycles-and-residents-first-deploy-then-whenever-the-list-changes)).
- Cycles: one line each.
- Residents: a CSV of `roll_number,name,smail_address` from the hostel list. The importer gives each new resident a passcode and writes them to a file for you to hand out. Residents already loaded keep their passcode.

**Not built yet:**
- **No passcode reset.** The importer skips residents who are already loaded, so it can't issue a new passcode. Keep the passcodes file the importer wrote somewhere safe.
- **No GS override** for the 24-hour or 3-a-week rules.

## Whoever runs the server

- Redeploy, backups and restarts: [DEPLOY.md](DEPLOY.md).
- Everything is in one file, `/var/lib/cyclebooking/cyclebooking.db`. Copying that file (the nightly backup does) is a full backup.
- The hosting runs on an Azure for Students subscription that lasts 12 months (ADR 0007). Before it runs out, move the database file to the next host.
