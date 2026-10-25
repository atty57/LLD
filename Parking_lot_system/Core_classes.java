// Core domain for a multi-floor parking lot.
// Note: like the Shopping_system folder, these files are a design reference,
// not a single compilable unit (several public classes per file).

import java.util.*;

// Vehicle hierarchy - the thing being parked
public abstract class Vehicle {
    protected String licensePlate;
    protected VehicleType type;  // MOTORCYCLE, CAR, TRUCK

    public Vehicle(String licensePlate, VehicleType type) {
        this.licensePlate = licensePlate;
        this.type = type;
    }

    public VehicleType getType() {
        return type;
    }
}

public class Motorcycle extends Vehicle {
    public Motorcycle(String licensePlate) {
        super(licensePlate, VehicleType.MOTORCYCLE);
    }
}

public class Car extends Vehicle {
    public Car(String licensePlate) {
        super(licensePlate, VehicleType.CAR);
    }
}

public class Truck extends Vehicle {
    public Truck(String licensePlate) {
        super(licensePlate, VehicleType.TRUCK);
    }
}

// A single spot. A spot fits a vehicle if the spot size is >= vehicle size.
public class ParkingSpot {
    private final String spotId;
    private final SpotType spotType;  // SMALL, MEDIUM, LARGE
    private Vehicle currentVehicle;   // null when free

    public ParkingSpot(String spotId, SpotType spotType) {
        this.spotId = spotId;
        this.spotType = spotType;
    }

    public boolean isFree() {
        return currentVehicle == null;
    }

    public boolean canFit(Vehicle vehicle) {
        // A LARGE spot can hold anything; SMALL holds only a motorcycle, etc.
        return isFree() && spotType.ordinal() >= vehicle.getType().ordinal();
    }

    public void park(Vehicle vehicle) {
        if (!canFit(vehicle)) {
            throw new IllegalStateException("Spot " + spotId + " cannot fit vehicle");
        }
        this.currentVehicle = vehicle;
    }

    public Vehicle vacate() {
        Vehicle v = currentVehicle;
        currentVehicle = null;
        return v;
    }

    public String getSpotId() { return spotId; }
}

// One floor owns many spots and knows how to find a free one.
public class ParkingFloor {
    private final int floorNumber;
    private final List<ParkingSpot> spots;

    public ParkingFloor(int floorNumber, List<ParkingSpot> spots) {
        this.floorNumber = floorNumber;
        this.spots = spots;
    }

    // ponytail: linear scan; swap for per-SpotType free-lists if a floor has
    // thousands of spots and this becomes the hot path.
    public Optional<ParkingSpot> findFreeSpot(Vehicle vehicle) {
        return spots.stream().filter(s -> s.canFit(vehicle)).findFirst();
    }

    public int getFloorNumber() { return floorNumber; }
    public List<ParkingSpot> getSpots() { return spots; }
}

enum VehicleType { MOTORCYCLE, CAR, TRUCK }  // ordinal encodes size
enum SpotType { SMALL, MEDIUM, LARGE }
