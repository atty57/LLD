
# Parking Lot System – Low-Level Design

## Overview

A multi-floor parking lot that assigns an incoming vehicle to the smallest free
spot that fits it, issues a ticket, and charges a fee on exit based on how long
the vehicle stayed. The design keeps spot allocation, ticketing, and pricing as
separate concerns so each can change independently.

---

## Key Design Patterns

- **Strategy Pattern:** `PricingStrategy` decouples fee calculation from the lot.
  Swap `HourlyPricingStrategy` for flat/weekend rates without touching `ParkingLot`.
- **Facade Pattern:** `ParkingLot` hides floor-walking and spot allocation behind
  simple `park()` / `exit()` calls.
- **Polymorphism via size ordering:** `VehicleType` and `SpotType` ordinals encode
  size, so "does this spot fit this vehicle?" is one comparison.

---

## Class Relationships

### Inheritance
- `Vehicle` (abstract) → `Motorcycle`, `Car`, `Truck`

### Composition
- `ParkingLot` contains `ParkingFloor[]`
- `ParkingFloor` contains `ParkingSpot[]`

### Association
- `Ticket` references a `Vehicle` and the `ParkingSpot` it occupies
- `ParkingLot` uses a `PricingStrategy` and tracks active `Ticket`s

---

## Core Workflows

1. **Entry:** `park(vehicle)` scans floors for the first spot where
   `canFit(vehicle)` holds, occupies it, and returns a `Ticket`.
2. **While parked:** the spot is marked occupied; the ticket records entry time.
3. **Exit:** `exit(ticketId)` stamps exit time, frees the spot, and returns the
   fee from the pricing strategy (minimum one hour billed).

---

## Textual Class Diagram

```
Vehicle (abstract)
├── Motorcycle
├── Car
└── Truck

ParkingLot (facade)
├── ParkingFloor[] (composition)
│   └── ParkingSpot[] (composition)
├── PricingStrategy (uses)
│   └── HourlyPricingStrategy
└── Ticket[] (active, keyed by id)

Ticket
├── Vehicle (for)
└── ParkingSpot (occupies)
```

---

## Scalability & Extensibility

- **New vehicle/spot sizes:** add an enum value; the size comparison still works.
- **New pricing:** implement `PricingStrategy` (flat, tiered, day-pass).
- **Spot lookup at scale:** replace the linear `findFreeSpot` scan with per-size
  free-lists or a counter per floor.
- **Concurrency:** a real lot needs locking around spot allocation; omitted here
  as the design focuses on structure.

---
