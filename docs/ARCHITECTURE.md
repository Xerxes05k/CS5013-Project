# Architecture

Single Spring Boot application, split into eight modules. Each module exposes a narrow Java service interface that other modules call. **No module reads another module's tables directly** — everything goes through the interfaces below, so a module's internals can change without breaking callers.

Source: the CS5013 design doc (`docs/proposal/design-doc.tex`, 11 Sep 2026).

## Module dependency graph

```mermaid
graph TD
    Auth[Auth Module<br/>smail OAuth2 / OTP]
    IdV[ID Verification Module<br/>OpenCV + Tesseract OCR]
    Rules[Rule Engine<br/>24h hold · 3/week]
    Booking[Booking Module<br/>availability · reservation · locking]
    Txn[Pickup/Return Module<br/>passcode confirm]
    Maint[Maintenance Comment Module<br/>comment box + classifier]
    Report[Reporting Module<br/>GS / Warden weekly view]
    Persist[(Persistence Module<br/>Spring Data JPA · SQLite)]

    Booking --> Auth
    Booking --> IdV
    Booking --> Rules
    Txn --> Booking
    Txn --> Maint
    Report --> Persist
    Maint --> Persist
    Booking --> Persist
    Txn --> Persist
    Rules --> Persist
    Auth --> Persist
```

Arrows mean "calls the interface of". The resident-facing web layer (Thymeleaf controllers) sits above Auth, Booking, Txn, and Maint; the GS-facing layer sits above Report.

## Modules and interfaces

### 1. Auth Module — `auth`
Institute smail login via Spring Security + OAuth2 (OTP fallback per risk 2).

```java
ResidentSession   login(String oauthToken);
ResidentIdentity  currentResident(ResidentSession s);   // {rollNumber, smailAddress}
```

### 2. ID Verification Module — `idverify`
OpenCV preprocessing (locate card, deskew, de-glare) + Tesseract OCR on the resident's ID card photo. Runs **at booking time on the resident's own phone**, not at the stand — so a bad photo can be retaken without a guard waiting.

```java
ExtractedIdentity scanCard(byte[] imageBytes);          // {name, rollNumber, hostel, confidence}
boolean           matchesLogin(ExtractedIdentity e, ResidentIdentity r);
```

Only the extracted text is persisted. The photo is never stored.

### 3. Rule Engine — `rules`
The hostel's own booking rules, kept separate from the booking flow so they can be unit-tested in isolation and overridden by the GS (risk 4).

```java
EligibilityResult checkEligibility(long residentId);    // {allowed, reason}
```

Rules: at most **24 hours** held per rental; at most **3 rentals per rolling 7 days**.

### 4. Booking Module — `booking`
Availability, reservation, and concurrency control over the cycle pool. `createBooking` calls `IdVerificationService.matchesLogin` and `RuleEngine.checkEligibility` before committing, and takes a DB-level lock on the cycle row so two residents can't book the same cycle.

```java
List<CycleStatus>       getAvailability();
BookingConfirmation     createBooking(ResidentIdentity r, long cycleId, ExtractedIdentity scannedCard);
BookingConfirmation     createBookingWithTypedRoll(ResidentIdentity r, long cycleId, String typedRoll);
Optional<CurrentRental> currentRental(long residentId);
List<HistoryEntry>      history(long residentId);          // the History tab, newest first
```

`createBookingWithTypedRoll` is the fallback when the card will not scan. The web layer offers it only after 3 failed scans. The booking is then marked `TYPED_ROLL` and the guard is asked to check the physical card at pickup.

### 5. Pickup/Return Module — `transaction`
The guard-witnessed step at the cycle stand. The resident enters their passcode in the app; the guard watches and hands over / accepts the cycle. **The guard has no device.**

```java
CycleState confirmPickup(long bookingId, String passcode);
CycleState confirmReturn(long bookingId, String passcode, Set<Part> brokenParts, String guardConditionNote);
```

The passcode is the resident's login passcode, checked against the same BCrypt hash. At return the guard looks the cycle over; the resident ticks the parts the guard found broken (`brokenParts`, empty = none) and types anything else the guard says into `guardConditionNote`. Both go to `MaintenanceService.recordInspection` inside the same transaction, so the return and the guard's findings are saved together or not at all.

### 6. Maintenance Comment Module — `maintenance`
Post-return free-text comment box. Comments go through a small OpenNLP classifier trained on our labeled phrase set. The raw comment is always shown alongside the flags (ADR 0003).

```java
List<MaintenanceFlag> submitComment(long bookingId, String text);
CycleState            recordInspection(long bookingId, Set<Part> brokenParts, String note);  // from confirmReturn
List<PartStatus>      partsOf(long cycleId);                          // ADR 0005, with cycle_parts
CycleState            recordRepair(long cycleId, Part part, String note);  // GS, ADR 0005, with cycle_parts
List<String>          classify(String text);          // CommentClassifier (week 5)
```

v1 (week 4) stores the comment, one per returned booking, and turns the guard's findings into `GUARD` flags on the cycle: one per broken part, labelled with the part (`brakes`, `bell`, …), or one `guard_note` flag for a note that names no part, with the guard's words in `MaintenanceFlag.note`. `partsOf` and `recordRepair` are declared for the cycle-parts work (ADR 0005) and throw until the `cycle_parts` table exists; until then `recordInspection` returns `AVAILABLE`. `submitComment` raises no flags until the week-5 classifier. The resident gets the comment box right after a confirmed return. Its booking id comes from the session, never from the form. Training data for the classifier is in `app/src/main/resources/maintenance/labelled-comments.tsv`.

### 7. Reporting Module — `reporting`
GS/Warden-facing weekly view.

```java
UsageReport           getWeeklyUsageReport();
List<MaintenanceItem> getMaintenanceList();   // open flags, each with the guard's or resident's words
```

### 8. Persistence Module — `persistence`
Spring Data JPA over SQLite. Repositories: `ResidentRepository`, `CycleRepository`, `BookingRepository`, `CommentRepository`, `MaintenanceFlagRepository`. Used by every module; bypassed by none.

## Cycle state machine

```
AVAILABLE ──createBooking──▶ BOOKED ──confirmPickup──▶ ISSUED ──confirmReturn──▶ AVAILABLE
                                                                    │
                                                  (flag from comment/guard note)
                                                                    ▼
                                                              UNDER_REPAIR ──GS clears──▶ AVAILABLE
```

A booking that isn't picked up within its window returns the cycle to `AVAILABLE`; a cycle held past 24 h is flagged `OVERDUE` on the booking (cycle stays `ISSUED`).

## Data model (entities)

Full schema, with the ER diagram, column types and constraints, is in [SCHEMA.md](SCHEMA.md). Request-by-request flowcharts are in [FLOWS.md](FLOWS.md).

| Entity | Key fields |
|---|---|
| `Resident` | id, rollNumber, name, hostel, smailAddress, passcodeHash |
| `Cycle` | id, label, state (`CycleState`), lastReturnedAt, version |
| `Booking` | id, resident, cycle, createdAt, pickedUpAt, returnedAt, idCheck (`CARD_SCAN` / `TYPED_ROLL`). `overdue` is computed from the timestamps, not stored |
| `Comment` | id, booking, text, createdAt |
| `MaintenanceFlag` | id, cycle, comment (nullable), label, source (`COMMENT` / `GUARD`), note, createdAt, resolvedAt |

## Test plan (one per module)

| Module | Test |
|---|---|
| Auth | unauthenticated request to a protected route → 401; valid token → correct `ResidentIdentity` |
| ID Verification | known-good photo → expected roll number, confidence ≥ threshold; blurred photo → low confidence, not a confident wrong match |
| Rule Engine | 3 bookings this week → 4th denied; 23 h held → fine, 25 h → overdue |
| Booking | two threads book the same cycle → exactly one succeeds, other gets "already booked" |
| Pickup/Return | wrong passcode → state unchanged; correct → `BOOKED→ISSUED`, later `ISSUED→RETURNED` |
| Maintenance | "chain keeps slipping off" → chain flag; "cycle is totally fine" → no flags |
| Reporting | seeded bookings → exact expected usage hours per cycle |
| Persistence | save `Booking`, fetch by id → all fields round-trip |
