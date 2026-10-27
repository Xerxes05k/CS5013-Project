# Frontend design: tabs and pages

**Status: planned.** From the mid-demo feedback (12 Oct): the UI was one list page with links out to separate pages, there was no way to move between sections, and it showed nothing about a cycle beyond its state. Owner: Aditya (frontend lead), see `PLAN.md`.

Still server-rendered Thymeleaf and our own CSS, with no JS framework or build step (Java-only, zero cost, works on any phone).

---

## 1. Navigation: a tab bar on every page

A bottom tab bar on phones, where the thumb reaches it. At ≥ 720 px wide it moves to the top. The active tab is highlighted; "My cycle" shows a dot while the resident has an open rental.

| Tab | URL | Who sees it | Shows |
|---|---|---|---|
| **Cycles** | `/cycles` | everyone | All cycles as cards: state chip, known issues, Book button |
| **My cycle** | `/rental` | everyone | Current rental: due-by countdown, pickup / return steps |
| **History** | `/history` | everyone | The resident's past rentals: date, cycle, hours, overdue or not |
| **GS** | `/gs/usage`, `/gs/maintenance`, `/gs/fleet` | GS role only (week 5) | Sub-tabs: Usage · Maintenance · Fleet |

Pages that aren't tabs (cycle detail, ID scan, comment box) keep the tab bar, with their parent tab highlighted.

```
┌──────────────────────────────┐
│  Jamuna cycles        ⎋ out  │  ← header: hostel name, log out
├──────────────────────────────┤
│                              │
│        page content          │
│                              │
├──────────────────────────────┤
│ 🚲 Cycles │ ● My cycle │ 🕘 History │  ← tab bar (GS tab added for the GS)
└──────────────────────────────┘
```

**How it's built** (one fragment, no duplication):
- `templates/fragments/layout.html` holds `th:fragment="tabs(active)"`. Each page includes it with `th:replace="~{fragments/layout :: tabs('cycles')}"`.
- A `@ControllerAdvice` class in `web` adds two model attributes to every page: `hasOpenRental` (for the dot) and `isGs` (to show the GS tab). This keeps each controller from repeating them.

---

## 2. Pages

### Cycles tab: list of cards

Each card shows the cycle's properties, not just its state. The chip uses colour **and** text, so it still reads in sunlight or for colour-blind users.

```
┌──────────────────────────────┐
│ 3 of 5 free right now        │
│ ┌──────────────────────────┐ │
│ │ JAM-01          [ Free ] │ │
│ │ ✓ all parts OK    [Book] │ │
│ └──────────────────────────┘ │
│ ┌──────────────────────────┐ │
│ │ JAM-04          [ Free ] │ │
│ │ ⚠ bell reported   [Book] │ │  ← non-critical issue: still bookable
│ └──────────────────────────┘ │
│ ┌──────────────────────────┐ │
│ │ JAM-05  [ Under repair ] │ │
│ │ ✕ brakes broken          │ │  ← critical: no Book button
│ └──────────────────────────┘ │
└──────────────────────────────┘
```

Tapping a card opens **cycle detail** (`/cycles/{id}`).

### Cycle detail: the 8 parts

The parts from ADR 0005, as a grid. This is where the properties that decide the state are visible.

```
┌──────────────────────────────┐
│ ← Cycles     JAM-05          │
│ [ Under repair ]             │
│ Last returned Sun 11 Oct 6pm │
│                              │
│ Parts                        │
│ ┌────────┬────────┬────────┐ │
│ │ Brakes │ Tyre   │ Chain  │ │
│ │ ✕ BROKEN│ ✓ OK  │ ✓ OK   │ │
│ ├────────┼────────┼────────┤ │
│ │ Seat   │ Pedal  │Handlebar│ │
│ │ ✓ OK   │ ✓ OK   │ ✓ OK   │ │
│ ├────────┼────────┼────────┤ │
│ │ Lock   │ Bell   │        │ │
│ │ ✓ OK   │⚠ REPORTED│      │ │
│ └────────┴────────┴────────┘ │
│ Critical parts are marked ★  │
└──────────────────────────────┘
```

### My cycle tab

Before pickup: the passcode step, as today. After pickup: a countdown to the 24 h limit, then the return form with the **parts checklist**.

```
┌──────────────────────────────┐
│ JAM-04 · picked up 2:10 pm   │
│ Due back in  5 h 12 min      │  ← turns red when overdue
│                              │
│ Return at the stand          │
│ Ask the guard to look it over│
│ What did the guard find      │
│ broken? (leave all unticked  │
│ if nothing)                  │
│ ☐ Brakes ★  ☐ Tyre ★         │
│ ☐ Chain ★   ☐ Seat ★         │
│ ☐ Pedal ★   ☐ Handlebar ★    │
│ ☐ Lock      ☐ Bell           │
│ Guard's words (optional) [ ] │
│ Passcode [••••••]            │
│ [   Confirm return   ]       │
└──────────────────────────────┘
```

**Guard confirmation screen.** After a pickup or return succeeds, the page shows a full-width green panel, readable at arm's length: "✓ PICKUP CONFIRMED · JAM-04 · 2:10 pm". The guard has no device and only looks at the resident's phone, so this is the one screen the guard has to read.

### History tab

```
┌──────────────────────────────┐
│ Your rentals (2 of 3 used    │
│ this week)                   │
│ Sun 11 Oct  JAM-04  3.5 h  ✓ │
│ Thu  8 Oct  JAM-01  26 h   ⚠ │  ← ⚠ = returned overdue
│ Mon  5 Oct  JAM-02  1 h    ✓ │
└──────────────────────────────┘
```

Uses `BookingRepository.findByResidentIdOrderByCreatedAtDesc`, which already exists, through a new `BookingService.history(residentId)` that returns records.

### GS tab (GS role only)

Sub-tabs across the top of the content:

- **Usage**: today's `/report` page, moved here.
- **Maintenance**: open flags grouped by cycle. Each shows the part, who reported it (guard / classifier / GS), and the **raw comment or guard's words** next to it (ADR 0003), with a "Repaired" button that calls `recordRepair`.
- **Fleet**: every cycle with its 8 parts in one table, to spot which cycles keep breaking.

---

## 3. Visual style

| What | Rule |
|---|---|
| State chips | Free = green, Booked = amber, In use = blue, Under repair = red; always with the word, never colour alone |
| Part condition | ✓ OK · ⚠ REPORTED · ✕ BROKEN, with ★ on critical parts |
| Cards | One per cycle, 12 px rounded corners, full width on phones |
| Messages | Success in a green bar, error in a red bar, both at the top of the content |
| Touch targets | Buttons and checkboxes at least 44 px tall |
| Empty states | Every list says what to do when empty ("No rentals yet. Book from the Cycles tab.") |

All in `static/css/app.css`; no CSS framework, so every rule can be explained.

---

## 4. Endpoints this adds

Added to [FLOWS.md §3](FLOWS.md#3-http-endpoints) when built.

| Method | URL | Handler | Service calls |
|---|---|---|---|
| GET | `/cycles/{id}` | `CycleController.detail` | `getAvailability`, `MaintenanceService.partsOf` |
| GET | `/history` | `HistoryController.history` | `BookingService.history` |
| GET | `/gs/usage` | `GsController.usage` | `ReportingService.getWeeklyUsageReport` |
| GET | `/gs/maintenance` | `GsController.maintenance` | `ReportingService.getMaintenanceList` |
| POST | `/gs/cycles/{id}/parts/{part}/repaired` | `GsController.repaired` | `MaintenanceService.recordRepair` |
| GET | `/gs/fleet` | `GsController.fleet` | `getAvailability`, `partsOf` |
