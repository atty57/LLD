// Dispatch strategy + the top-level controller that owns all cars.

import java.util.*;

// Strategy for choosing which car serves an external request. Swap in
// zoned/energy-optimal dispatchers without touching the controller.
public interface DispatchStrategy {
    Elevator selectElevator(List<Elevator> elevators, Request request);
}

// Nearest-car dispatch: pick the car with the smallest distance to the request
// floor, preferring one already heading that way or idle.
public class NearestCarStrategy implements DispatchStrategy {
    @Override
    public Elevator selectElevator(List<Elevator> elevators, Request request) {
        Elevator best = null;
        int bestCost = Integer.MAX_VALUE;
        for (Elevator e : elevators) {
            int cost = e.distanceTo(request.getFloor());
            // Penalise cars moving the opposite way; they'd have to finish a sweep first.
            if (!e.isIdle() && e.getDirection() != request.getDirection()) {
                cost += 1000;
            }
            if (cost < bestCost) {
                bestCost = cost;
                best = e;
            }
        }
        return best;
    }
}

// The building's controller: routes requests to cars and steps the simulation.
public class ElevatorController {
    private final List<Elevator> elevators;
    private final DispatchStrategy dispatcher;

    public ElevatorController(List<Elevator> elevators, DispatchStrategy dispatcher) {
        this.elevators = elevators;
        this.dispatcher = dispatcher;
    }

    // Hall button pressed on some floor.
    public void handleExternalRequest(Request request) {
        Elevator car = dispatcher.selectElevator(elevators, request);
        if (car == null) throw new IllegalStateException("No elevator available");
        car.addStop(request.getFloor());
    }

    // Button pressed inside a specific car.
    public void handleInternalRequest(int elevatorId, int targetFloor) {
        elevators.stream()
            .filter(e -> e.getId() == elevatorId)
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("No car " + elevatorId))
            .addStop(targetFloor);
    }

    // One tick of the simulation: every car advances by one stop.
    public void step() {
        for (Elevator e : elevators) {
            e.step();
        }
    }
}
