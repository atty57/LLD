
# Movie Ticket Booking System – Low-Level Design

## Overview

A BookMyShow-style system where a user browses shows of a movie at a theatre,
picks seats, and books them. The central problem is concurrency: two users must
never book the same seat. The design solves this with a per-show seat-state map
guarded by a short-lived hold, so a seat is reserved while the user pays and
released automatically if they don't.

---

## Key Design Patterns

- **Seat state on the Show, not the Seat:** the same physical `Seat` is free for
  one show and booked for another, so booked-ness is per-`Show` state.
- **Hold / confirm / release lifecycle:** `reserve` holds seats (5-min TTL),
  `confirm` locks them after payment, `cancel` frees them — the classic two-phase
  booking that prevents double-booking without holding a DB row forever.
- **Service orchestration:** `BookingService` coordinates the flow so `Show` only
  owns seat state and locking.

---

## Class Relationships

### Composition
- `Theatre` contains `Screen[]`; `Screen` contains `Seat[]`

### Association
- `Show` references a `Movie` and the `Screen` it plays on
- `Booking` references its `Show` and the held seat ids
- `BookingService` holds/confirms/releases seats on a `Show`

---

## Core Workflows

1. **Browse:** list `Show`s for a movie/city; `availableSeats()` reflects current,
   non-expired state.
2. **Reserve:** `BookingService.reserve` calls `Show.hold` — an atomic,
   all-or-nothing hold on the chosen seats — and returns a PENDING `Booking`.
3. **Pay & confirm:** on payment success, `confirm` flips the held seats to BOOKED.
4. **Abandon:** on failure/timeout, `cancel` releases the seats; expired holds are
   also treated as free on the next read.

---

## Textual Class Diagram

```
Theatre
└── Screen[] (composition)
    └── Seat[] (composition)

Show
├── Movie (screens)
├── Screen (in)
└── seatStates: seatId → AVAILABLE | HELD | BOOKED
    + holdExpiry: seatId → epoch ms

BookingService
├── reserve → Show.hold → Booking (PENDING)
├── confirm → Show.confirm → Booking (CONFIRMED)
└── cancel  → Show.release → Booking (CANCELLED)
```

---

## Concurrency Notes

- `Show.hold/confirm/release` are `synchronized` so the check-then-set on seats is
  atomic — the key correctness property.
- Holds carry an expiry; an expired HELD seat reads as free, so a crashed/abandoned
  checkout never strands a seat. A background sweep could reset state eagerly.
- **Scaling ceiling:** one lock per show. If a single blockbuster show becomes a
  contention hot spot, shard the lock by seat row/block, or move seat state to a
  store with per-seat optimistic concurrency.

---

## Scalability & Extensibility

- **Pricing:** add a strategy keyed on `SeatType`/time, mirroring the parking-lot
  pricing design.
- **Payments:** plug in the same payment abstraction used by the shopping system.
- **Search:** index shows by city/movie/time for browse queries.

---
