// Core domain for an elevator (lift) control system.
// Design reference, not a single compilable unit (several public classes per file).

import java.util.*;

enum Direction { UP, DOWN, IDLE }
enum RequestType { EXTERNAL, INTERNAL }  // hall button vs. in-car button

// A single ride request. EXTERNAL requests carry the desired direction;
// INTERNAL requests (pressed inside the car) just carry a target floor.
public class Request {
    private final int floor;
    private final Direction direction;   // IDLE for internal requests
    private final RequestType type;

    public Request(int floor, Direction direction, RequestType type) {
        this.floor = floor;
        this.direction = direction;
        this.type = type;
    }

    public int getFloor() { return floor; }
    public Direction getDirection() { return direction; }
    public RequestType getType() { return type; }
}

// One elevator car. Holds its own pending stops split by direction so it always
// serves the nearest floor in its current direction first (SCAN / "elevator
// algorithm"), which is the whole point of the design.
public class Elevator {
    private final int id;
    private int currentFloor;
    private Direction direction = Direction.IDLE;
    private final TreeSet<Integer> upStops = new TreeSet<>();    // floors above, ascending
    private final TreeSet<Integer> downStops = new TreeSet<>();  // floors below

    public Elevator(int id, int startFloor) {
        this.id = id;
        this.currentFloor = startFloor;
    }

    public void addStop(int floor) {
        if (floor == currentFloor) return;
        if (floor > currentFloor) upStops.add(floor);
        else downStops.add(floor);
        if (direction == Direction.IDLE) {
            direction = floor > currentFloor ? Direction.UP : Direction.DOWN;
        }
    }

    // Advance one stop. Returns the floor reached, or -1 if nothing to do.
    public int step() {
        if (direction == Direction.UP && !upStops.isEmpty()) {
            currentFloor = upStops.pollFirst();
        } else if (direction == Direction.DOWN && !downStops.isEmpty()) {
            currentFloor = downStops.pollLast();
        } else if (!upStops.isEmpty()) {            // reverse at the end of a sweep
            direction = Direction.UP;
            currentFloor = upStops.pollFirst();
        } else if (!downStops.isEmpty()) {
            direction = Direction.DOWN;
            currentFloor = downStops.pollLast();
        } else {
            direction = Direction.IDLE;
            return -1;
        }
        return currentFloor;
    }

    // How "costly" this car is for a request — used by the dispatcher.
    public int distanceTo(int floor) {
        return Math.abs(currentFloor - floor);
    }

    public int getId() { return id; }
    public int getCurrentFloor() { return currentFloor; }
    public Direction getDirection() { return direction; }
    public boolean isIdle() { return direction == Direction.IDLE; }
}
