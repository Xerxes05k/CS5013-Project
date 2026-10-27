# Developer Architecture and Flows

How a request actually moves through the code, module by module. Every box below names a real class or method in `app/src/main/java/in/ac/iitm/cs5013/cyclebooking/`. Steps that are **planned but not built yet** are drawn dashed and marked *(planned)*.

The data these flows read and write is in [SCHEMA.md](SCHEMA.md). The module interfaces are in [ARCHITECTURE.md](ARCHITECTURE.md).

**Contents**

1. [Layered architecture](#1-layered-architecture)
2. [Who calls whom](#2-who-calls-whom)
3. [HTTP endpoints](#3-http-endpoints)
4. [End-to-end journey](#4-end-to-end-journey)
5. [Login](#5-login)
6. [Booking a cycle](#6-booking-a-cycle) (with the OCR pipeline and the booking race)
7. [Pickup at the stand](#7-pickup-at-the-stand)
8. [Return, guard note and comment](#8-return-guard-note-and-comment)
9. [State machines](#9-state-machines)
10. [Usage report](#10-usage-report)
11. [Transactions and concurrency](#11-transactions-and-concurrency)
12. [Built vs planned](#12-built-vs-planned)

---

## 1. Layered architecture

One Spring Boot process, one SQLite file. The resident's phone browser is the only client. The guard has no device: they watch the resident's screen.

```mermaid
flowchart TB
    subgraph Client["Client"]
        Phone["Resident's phone browser<br/>HTML from Thymeleaf, no JS app"]
    end

    subgraph App["Spring Boot app (one JVM)"]
        direction TB
        Sec["Spring Security filter chain<br/>SecurityConfig: form login, session cookie,<br/>every page except /login needs a login"]
        subgraph Web["Web layer, package web"]
            CC["CycleController"]
            RC["RentalController"]
            RpC["ReportController"]
        end
        subgraph Svc["Service layer: one interface per module"]
            Auth["AuthService"]
            IdV["IdVerificationService"]
            Rules["RuleService"]
            Book["BookingService"]
            Txn["TransactionService"]
            Maint["MaintenanceService"]
            Rep["ReportingService"]
        end
        subgraph Per["Persistence layer, package persistence"]
            Repos["Spring Data JPA repositories<br/>Resident, Cycle, Booking, Comment, MaintenanceFlag"]
        end
        OCR["Tess4j (Tesseract) + OpenCV<br/>native libraries, in-process"]
        Clock["java.time.Clock bean<br/>(tests swap in a fixed clock)"]
    end

    DB[("SQLite file<br/>app/data/cyclebooking.db")]

    Phone -- "HTTPS form posts and GETs" --> Sec --> Web
    Web --> Svc
    IdV --> OCR
    Svc --> Repos --> DB
    Svc -.-> Clock
```

**Rules every layer follows**

- Controllers only translate HTTP into service calls and pick a template. No business logic, no repository access.
- Each module exposes one `*Service` interface; the `Default*Service` implementation is package-private, so other packages can't depend on it.
- Services return `record`s (`CycleStatus`, `CurrentRental`, …), never JPA entities, to the web layer.
- Time always comes from the injected `Clock`, never `Instant.now()`, so tests can sit exactly on the 24 h and 7-day edges.

---

## 2. Who calls whom

Taken from the constructors (what Spring injects into each class). An arrow means "holds a reference to and calls".

```mermaid
flowchart LR
    CC[CycleController] --> Book[BookingService]
    CC --> IdV[IdVerificationService]
    CC --> Auth[AuthService]

    RC[RentalController] --> Book
    RC --> Txn[TransactionService]
    RC --> Maint[MaintenanceService]
    RC --> Auth

    RpC[ReportController] --> Rep[ReportingService]

    Book --> Rules[RuleService]
    Book --> IdV
    Txn --> Maint

    Auth --> RR[(ResidentRepository)]
    Book --> CR[(CycleRepository)]
    Book --> RR
    Book --> BR[(BookingRepository)]
    Rules --> BR
    Txn --> BR
    Maint --> BR
    Maint --> ComR[(CommentRepository)]
    Maint --> FR[(MaintenanceFlagRepository)]
    Rep --> BR
    Rep --> CR

    Txn -. "PasswordEncoder (BCrypt)" .-> Auth
```

`IdVerificationService` touches no repository: it reads a photo and returns text. Nothing calls back up the graph, so there are no cycles between modules.

---

## 3. HTTP endpoints

Every `POST` ends in a redirect (post-redirect-get), so refreshing a page never repeats a booking or a return. Success and error messages travel as one-shot flash attributes.

| Method | URL | Handler | Service calls | Success → | Failure → |
|---|---|---|---|---|---|
| GET | `/login` | `CycleController.login` | — | `login.html` | |
| POST | `/login` | Spring Security | `ResidentDetailsService.loadUserByUsername`, BCrypt check | `/cycles` | `/login?error` |
| GET | `/` | `CycleController.home` | — | `/cycles` | |
| GET | `/cycles` | `CycleController.availability` | `getAvailability`, `currentResident`, `currentRental` | `cycles.html` | |
| GET | `/cycles/{id}/verify` | `CycleController.verifyPage` | `getAvailability` | `verify.html` | `/cycles` if not bookable |
| POST | `/cycles/{id}/book` (photo) | `CycleController.book` | `scanCard`, `createBooking` | `/rental` | `/cycles/{id}/verify` (ID failed) or `/cycles` (rule/lock) |
| POST | `/cycles/{id}/book-typed` | `CycleController.bookWithTypedRoll` | `createBookingWithTypedRoll` | `/rental` | same as above |
| GET | `/rental` | `RentalController.myCycle` | `currentRental` | `rental.html` | `/cycles` if none |
| POST | `/rental/pickup` (passcode) | `RentalController.pickup` | `confirmPickup` | `/rental` | `/rental` with error |
| POST | `/rental/return` (passcode, guardNote) | `RentalController.giveBack` | `confirmReturn` | `/rental/comment` | `/rental` with error |
| GET | `/rental/comment` | `RentalController.commentBox` | — | `comment.html` | `/cycles` if no return in session |
| POST | `/rental/comment` (text) | `RentalController.comment` | `submitComment` | `/cycles` | `/rental/comment` with error |
| POST | `/rental/comment/skip` | `RentalController.skipComment` | — | `/cycles` | |
| GET | `/history` | `HistoryController.history` | `currentResident`, `history` | `history.html` | |
| GET | `/report` | `ReportController.usage` | `getWeeklyUsageReport` | `report.html` | |

**The booking id is never read from a form.** Pickup and return act on the logged-in resident's own open booking (`currentRental`); the comment acts on the booking id kept in the server-side session. A resident can't act on someone else's booking by editing a hidden field.

---

## 4. End-to-end journey

The whole rental as the resident and the guard experience it, with the module that handles each step.

```mermaid
flowchart TD
    A([Resident opens portal on phone]) --> B["Log in: smail + passcode<br/><i>Auth</i>"]
    B --> C["See cycle list<br/><i>Booking.getAvailability</i>"]
    C --> D{Has an open rental?}
    D -- yes --> R["My cycle page<br/><i>Booking.currentRental</i>"]
    D -- no --> E[Tap an AVAILABLE cycle]
    E --> F["Photograph own ID card<br/><i>IdVerification.scanCard</i>"]
    F --> G["Rules → ID match → lock<br/><i>Booking.createBooking</i>"]
    G -- refused --> C
    G -- ID not matched --> F
    G -- booked --> R
    R --> H["At the stand: type passcode,<br/>guard watches it accepted<br/><i>Transaction.confirmPickup</i>"]
    H --> I[Guard hands over cycle<br/>cycle ISSUED, 24 h clock starts]
    I --> J([Resident rides])
    J --> K["Back at stand: guard looks cycle over,<br/>resident types guard's remark + passcode<br/><i>Transaction.confirmReturn</i>"]
    K --> L["Guard note becomes a GUARD flag<br/><i>Maintenance.recordGuardNote</i>"]
    L --> M["Optional comment box<br/><i>Maintenance.submitComment</i>"]
    M --> C
    I -.-> N["GS/Warden: weekly usage hours<br/><i>Reporting</i>"]
```

---

## 5. Login

Spring Security's form login, with our `ResidentDetailsService` supplying the user. No custom login code. ADR 0004 explains why it's passcode now and smail OAuth2 later.

```mermaid
sequenceDiagram
    autonumber
    participant P as Phone
    participant S as Spring Security
    participant RDS as ResidentDetailsService
    participant RR as ResidentRepository
    participant DB as SQLite

    P->>S: GET /cycles (no session)
    S-->>P: 302 → /login
    P->>S: POST /login (smail, passcode)
    S->>RDS: loadUserByUsername(smail)
    RDS->>RR: findBySmailAddressIgnoreCase
    RR->>DB: SELECT … FROM residents
    DB-->>RDS: row with passcode_hash
    RDS-->>S: UserDetails(smail, hash, ROLE_RESIDENT)
    S->>S: BCrypt.matches(passcode, hash)
    alt match
        S-->>P: 302 → /cycles + JSESSIONID cookie
    else no match / unknown smail
        S-->>P: 302 → /login?error
    end
    Note over P,S: Later requests: the session cookie gives a Principal whose name is the smail address.<br/>AuthService.currentResident(principal) turns that into ResidentIdentity(id, roll, smail).
```

---

## 6. Booking a cycle

### 6a. Controller and service flow

Two pages: pick a cycle (`/cycles`), then scan the card (`/cycles/{id}/verify`). The order of checks inside `createBooking` is deliberate: **rules first** (no point asking for a better photo from someone who can't book), **then ID**, **then the lock** (only claim the cycle once everything else has passed).

```mermaid
flowchart TD
    Start(["POST /cycles/{id}/book<br/>with photo"]) --> P{Photo attached?}
    P -- no --> Back1["flash error<br/>→ /cycles/{id}/verify"]
    P -- yes --> Who["AuthService.currentResident(principal)<br/>→ ResidentIdentity"]
    Who --> Scan["IdVerificationService.scanCard(bytes)<br/>→ ExtractedIdentity<br/>(photo kept in memory only)"]
    Scan --> TX

    subgraph TX["BookingService.createBooking — one @Transactional"]
        direction TB
        R1["RuleService.checkEligibility(residentId)"] --> R2{allowed?}
        R2 -- "no: OVERDUE_RETURN /<br/>ALREADY_HOLDING / WEEKLY_LIMIT" --> X1[[throw BookingRejectedException]]
        R2 -- yes --> M1{"matchesLogin?<br/>roll ≠ null AND<br/>confidence ≥ 70 AND<br/>roll = login roll"}
        M1 -- no --> X2[[throw IdCheckFailedException]]
        M1 -- yes --> L1["CycleRepository.claimIfAvailable(id)<br/>UPDATE … WHERE state='AVAILABLE'"]
        L1 --> L2{rows changed?}
        L2 -- 0 --> X3[[throw BookingRejectedException<br/>'no longer available']]
        L2 -- 1 --> S1["INSERT bookings<br/>(resident, cycle, now, CARD_SCAN)"]
        S1 --> OK[return BookingConfirmation]
    end

    X1 --> E1["flash reason → /cycles"]
    X3 --> E1
    X2 --> F1["session.failedIdScans += 1<br/>flash 'retake photo' or 'not your card'<br/>→ /cycles/{id}/verify"]
    F1 --> F2{failedIdScans ≥ 3?}
    F2 -- yes --> Typed["verify page also shows<br/>'type your roll number'<br/>→ POST /book-typed<br/>→ createBookingWithTypedRoll<br/>(same rules + lock, IdCheck = TYPED_ROLL)"]
    OK --> Done["clear failedIdScans<br/>flash 'Booked JAM-04…'<br/>→ /rental"]
```

Any exception inside the transaction rolls it back, so a refused booking leaves no row and no change to the cycle.

### 6b. OCR pipeline inside `scanCard`

`IdCardPreprocessor` (OpenCV) → `IdCardScanner` (Tesseract) → `IdCardTextParser` (regex). Three differently-cleaned copies of the photo are read and the roll number most of them agree on wins. This came from the week-3 real-card failure (PROGRESS, Week 3).

```mermaid
flowchart TD
    In["photo bytes (JPEG/PNG from phone)"] --> Dec["imdecode as greyscale<br/>empty? → 'Not an image'"]
    Dec --> Rs["resize to 1600 px wide<br/>(letters ~30-50 px tall)"]
    Rs --> St["straighten: find card outline,<br/>rotate it level"]
    St --> V1["v1: plain greyscale"]
    St --> V2["v2: CLAHE local contrast<br/>(lifts text out of glare/shadow)"]
    St --> V3["v3: median blur + adaptive threshold<br/>black text on white"]

    V1 --> T1
    V2 --> T2
    V3 --> T3
    subgraph Each["for each version: IdCardScanner.read"]
        T1["Tesseract.doOCR, PSM 6"]
        T2["Tesseract.doOCR, PSM 6"]
        T3["Tesseract.doOCR, PSM 6"]
    end
    T1 & T2 & T3 --> Parse["IdCardTextParser:<br/>regex LL DD L DDD, e.g. CE24B128<br/>fix O↔0, I↔1, S↔5, B↔8 by position<br/>+ 'Name:' and 'Hostel:' lines"]
    Parse --> Conf["rollConfidence = confidence of the<br/>roll number's WEAKEST character"]
    Conf --> Vote["vote: most common roll number wins;<br/>tie → higher confidence"]
    Vote --> Out["ExtractedIdentity(name, roll, hostel, confidence)"]

    Dec -. "any exception, or<br/>Tesseract not installed" .-> Un["ExtractedIdentity.unreadable()<br/>roll = null, confidence = 0"]
```

`scanCard` is `synchronized`: one Tesseract instance isn't thread-safe, and a scan takes about a second, so a second scan at the same moment just waits.

The confidence threshold doesn't stop every misread (an 8 read as a 5 scored 95+). What actually stops a wrong booking is the last check in `matchesLogin`: the roll number read must **equal** the logged-in resident's own.

### 6c. Two residents, one cycle

Why the lock is a single conditional `UPDATE` and not "read the state, then write it": between someone's read and write, another request could read the same `AVAILABLE`. The database runs one `UPDATE` at a time on that row, so only the first one finds `state = 'AVAILABLE'`.

```mermaid
sequenceDiagram
    autonumber
    participant A as Resident A
    participant B as Resident B
    participant BS as BookingService
    participant DB as SQLite

    A->>BS: createBooking(cycle 4)
    B->>BS: createBooking(cycle 4)
    Note over BS: both pass rules and ID check
    BS->>DB: A: UPDATE cycles SET state='BOOKED' WHERE id=4 AND state='AVAILABLE'
    DB-->>BS: 1 row changed
    BS->>DB: A: INSERT INTO bookings …
    BS-->>A: Booked JAM-04
    BS->>DB: B: UPDATE cycles SET state='BOOKED' WHERE id=4 AND state='AVAILABLE'
    DB-->>BS: 0 rows changed (state is BOOKED now)
    BS-->>B: "That cycle is no longer available. Pick another."
```

Tested by `DefaultBookingServiceTest.createBooking_rejectsSecondConcurrentBookingOfSameCycle` with two real threads on real SQLite.

---

## 7. Pickup at the stand

The passcode is the same one used to log in, checked against the same BCrypt hash. Typing it in front of the guard ties the person at the stand to the account that booked.

```mermaid
flowchart TD
    S(["POST /rental/pickup (passcode)"]) --> C["RentalController: currentRental(own id)"]
    C --> N{open booking?}
    N -- no --> E0["'You have no cycle booked' → /cycles"]
    N -- yes --> T

    subgraph T["TransactionService.confirmPickup — @Transactional"]
        direction TB
        F["load booking + cycle"] --> St{"pickedUpAt == null<br/>AND cycle.state == BOOKED?"}
        St -- no --> X1[["reject 'already picked up'"]]
        St -- yes --> Pw{"BCrypt.matches(passcode,<br/>resident.passcodeHash)?"}
        Pw -- no --> X2[["reject 'Wrong passcode.<br/>Nothing was changed'"]]
        Pw -- yes --> W["booking.pickedUpAt = now<br/>cycle.state = ISSUED"]
    end

    W --> OK["'Pickup confirmed… return within 24 h' → /rental<br/>(page shows due-by time)"]
    X1 --> E1["error → /rental"]
    X2 --> E1
    OK -.-> G["Guard sees the confirmation on screen<br/>and hands over the cycle.<br/>If idCheck = TYPED_ROLL the page asks<br/>the guard to look at the ID card first."]
```

The order matters: state checks, then passcode, then changes. A wrong passcode always leaves the booking and the cycle exactly as they were.

---

## 8. Return, guard note and comment

The return and the guard's note are saved in **one transaction**: if saving the note fails, the return is rolled back too, so a cycle can't come back `AVAILABLE` with the guard's warning lost.

```mermaid
flowchart TD
    S(["POST /rental/return<br/>(passcode, guardNote)"]) --> C["currentRental(own id)"]
    C --> T

    subgraph T["TransactionService.confirmReturn — one @Transactional"]
        direction TB
        A{pickedUpAt set?} -- no --> X1[["reject 'not picked up yet'"]]
        A -- yes --> B{"returnedAt null AND<br/>state == ISSUED?"}
        B -- no --> X2[["reject 'already returned'"]]
        B -- yes --> L{"note ≤ 500 chars?"}
        L -- no --> X3[["reject 'note too long'"]]
        L -- yes --> P{passcode OK?}
        P -- no --> X4[["reject 'Wrong passcode'"]]
        P -- yes --> W["booking.returnedAt = now<br/>cycle.lastReturnedAt = now<br/>cycle.state = AVAILABLE"]
        W --> G["MaintenanceService.recordGuardNote"]
        G --> GB{note blank?}
        GB -- yes --> GD[nothing saved]
        GB -- no --> GF["INSERT maintenance_flags<br/>(cycle, comment=null, 'guard_note',<br/>GUARD, now, note)"]
    end

    X1 & X2 & X3 & X4 --> E["error → /rental"]
    GD & GF --> Sess["session.returnedRental =<br/>(bookingId, cycleLabel)<br/>→ /rental/comment"]

    Sess --> CB{resident chooses}
    CB -- skip --> Clr["remove returnedRental → /cycles"]
    CB -- submit text --> SC

    subgraph SC["MaintenanceService.submitComment — @Transactional"]
        direction TB
        V1{"1–2000 chars?"} -- no --> Y1[[reject]]
        V1 -- yes --> V2{booking returned?}
        V2 -- no --> Y1
        V2 -- yes --> V3{no comment yet?}
        V3 -- no --> Y1
        V3 -- yes --> Ins["INSERT comments"]
        Ins -.-> Cls["CommentClassifier (OpenNLP)<br/>→ COMMENT flags<br/><i>(planned, week 5)</i>"]
    end

    Y1 --> CE["error → /rental/comment"]
    Ins --> Fin["remove returnedRental<br/>'Thanks! … goes to the GS' → /cycles"]

    style Cls stroke-dasharray: 5 5
```

---

## 9. State machines

### Cycle (`cycles.state`)

```mermaid
stateDiagram-v2
    [*] --> AVAILABLE
    AVAILABLE --> BOOKED: createBooking · claimIfAvailable
    BOOKED --> ISSUED: confirmPickup · passcode OK
    ISSUED --> AVAILABLE: confirmReturn · passcode OK
    BOOKED --> AVAILABLE: no-show expiry / cancel · (planned)
    AVAILABLE --> UNDER_REPAIR: GS marks for repair · (planned)
    UNDER_REPAIR --> AVAILABLE: GS clears · (planned)
```

Built today: the three transitions in the top cycle. A guard flag **does not** change the cycle's state; it only adds a row to `maintenance_flags`. `UNDER_REPAIR` exists in the enum and the seed data but nothing in the code sets or clears it yet.

### Booking (derived from its timestamps)

A booking has no `state` column; its stage is read from which timestamps are set.

```mermaid
stateDiagram-v2
    [*] --> Booked: created_at set
    Booked --> PickedUp: picked_up_at set
    PickedUp --> Overdue: now − picked_up_at > 24 h · (computed, not stored)
    PickedUp --> Returned: returned_at set
    Overdue --> Returned: returned_at set
    Returned --> Commented: comment row added · (optional)
    Returned --> [*]
    Commented --> [*]
```

| Stage | `picked_up_at` | `returned_at` | Counts as "open"? |
|---|---|---|---|
| Booked | null | null | yes |
| Picked up | set | null | yes |
| Overdue | set, > 24 h ago | null | yes. New bookings refused with `OVERDUE_RETURN` |
| Returned | set | set | no |

---

## 10. Usage report

`GET /report` → `ReportingService.getWeeklyUsageReport()` → window = last 7 days ending now.

```mermaid
flowchart TD
    A["window = [now − 7 d, now)"] --> B["findRentalsOverlapping(from, to):<br/>picked up before window ends AND<br/>(still out OR returned after window starts)"]
    B --> C["for each cycle (by label)"]
    C --> D["for each of its rentals:<br/>start = max(pickedUpAt, from)<br/>stop = min(returnedAt or now, to)<br/>hours += stop − start"]
    D --> E["CycleUsage(label, rentals, hours)"]
    E --> F["UsageReport → report.html<br/>no resident names shown"]
```

Usage counts from pickup to return, the time the cycle was actually away from the stand. Time booked but not yet picked up isn't usage.

---

## 11. Transactions and concurrency

| Operation | Transaction | What is atomic | Concurrency control |
|---|---|---|---|
| `createBooking` / `createBookingWithTypedRoll` | `@Transactional` | rule check + claim + insert booking | conditional `UPDATE`, rows-changed = 1 |
| `confirmPickup` | `@Transactional` | booking.pickedUpAt + cycle.state | `@Version` on `Cycle` (stale write fails) |
| `confirmReturn` | `@Transactional` | return + cycle state + guard flag | `@Version` on `Cycle` |
| `submitComment` | `@Transactional` | checks + insert | one comment per booking checked in the same transaction |
| reads (`getAvailability`, `currentRental`, reports, rules) | `readOnly = true` | — | — |
| `scanCard` | none (no DB) | — | `synchronized` around Tesseract |

The DB connection pool has one connection (`spring.datasource.hikari.maximum-pool-size=1`): SQLite allows one writer at a time anyway, and a single connection avoids `SQLITE_BUSY` errors under load.

---

## 12. Built vs planned

| Piece | Status | Week |
|---|---|---|
| Login (smail + passcode) | built | 1 |
| Availability page | built | 1 |
| Rule engine (24 h, 3/week, one open rental) | built | 2 |
| Booking with race-safe lock | built | 2 |
| OCR ID check + typed-roll fallback | built | 3 |
| Passcode pickup / return | built | 3 |
| Usage report v1 | built | 3 |
| Guard note at return → GUARD flag | built | 4 |
| Post-return comment box | built | 4 |
| Comment classifier → COMMENT flags | planned | 5 |
| GS maintenance list, resolve flags, `UNDER_REPAIR` | planned | 5 |
| GS-only role for `/report` | planned | 5 |
| Booking cancel / no-show expiry | not yet scheduled | — |
| GS override of rule limits | planned | 7 |
| smail OAuth2 instead of passcode | depends on IT approval (ADR 0004) | — |
