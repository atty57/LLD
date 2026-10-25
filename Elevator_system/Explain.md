
# Elevator System – Low-Level Design

## Overview

A control system for a bank of elevators in a building. Hall buttons (external
requests) and in-car buttons (internal requests) feed a controller that picks
the best car for each request. Each car serves its pending stops using the SCAN
("elevator") algorithm — sweep up serving all stops, then sweep down — which
minimises direction changes.

---

## Key Design Patterns

- **Strategy Pattern:** `DispatchStrategy` decides which car answers a hall call.
  `NearestCarStrategy` is the default; zoned or energy-optimal strategies drop in.
- **State-driven movement:** each `Elevator` tracks a `Direction` and two sorted
  stop sets; `step()` is a small state machine that reverses at the end of a sweep.
- **Separation of request sources:** external vs. internal requests are modelled
  explicitly (`RequestType`) because they carry different information.

---

## Class Relationships

### Composition
- `ElevatorController` owns `Elevator[]`

### Association
- `ElevatorController` uses a `DispatchStrategy`
- `DispatchStrategy` selects an `Elevator` for a `Request`

---

## Core Workflows

1. **Hall call (external):** `handleExternalRequest` asks the dispatcher for the
   lowest-cost car, then adds the stop to that car.
2. **Car button (internal):** `handleInternalRequest` adds the target floor to a
   specific car's stop set.
3. **Movement:** each `step()` advances every car by one stop, honouring its
   current direction and reversing only when the sweep is exhausted.

---

## Textual Class Diagram

```
ElevatorController
├── Elevator[] (composition)
│   ├── upStops (TreeSet)
│   └── downStops (TreeSet)
└── DispatchStrategy (uses)
    └── NearestCarStrategy

Request (floor, direction, type) ──routed by──> ElevatorController
```

---

## Scalability & Extensibility

- **New dispatch policy:** implement `DispatchStrategy` (zoning, express cars,
  energy minimisation) with no controller changes.
- **Larger buildings:** the per-car `TreeSet` stop handling scales to many floors;
  dispatch cost can add wait-time and load factors.
- **Concurrency:** a real system runs each car on its own thread/actor with a
  request queue; omitted here to keep the structure clear.

---
