// Ticketing, pricing and the top-level ParkingLot facade.

import java.util.*;

// Issued on entry, closed on exit. Drives the fee calculation.
public class Ticket {
    private final String ticketId;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final long entryTimeMillis;
    private long exitTimeMillis;  // 0 until vehicle leaves

    public Ticket(Vehicle vehicle, ParkingSpot spot) {
        this.ticketId = UUID.randomUUID().toString();
        this.vehicle = vehicle;
        this.spot = spot;
        this.entryTimeMillis = System.currentTimeMillis();
    }

    public void markExit() {
        this.exitTimeMillis = System.currentTimeMillis();
    }

    public long getParkedMillis() {
        long end = exitTimeMillis == 0 ? System.currentTimeMillis() : exitTimeMillis;
        return end - entryTimeMillis;
    }

    public Vehicle getVehicle() { return vehicle; }
    public ParkingSpot getSpot() { return spot; }
    public String getTicketId() { return ticketId; }
}

// Strategy for turning time + vehicle type into a fee. One implementation
// here; add others (flat rate, weekend rate) without touching ParkingLot.
public interface PricingStrategy {
    double calculate(Ticket ticket);
}

public class HourlyPricingStrategy implements PricingStrategy {
    private final Map<VehicleType, Double> ratePerHour;

    public HourlyPricingStrategy(Map<VehicleType, Double> ratePerHour) {
        this.ratePerHour = ratePerHour;
    }

    @Override
    public double calculate(Ticket ticket) {
        long hours = Math.max(1, ticket.getParkedMillis() / (1000 * 60 * 60));  // round up to 1h min
        return hours * ratePerHour.getOrDefault(ticket.getVehicle().getType(), 0.0);
    }
}

// Facade: park() finds a spot across floors, exit() frees it and prices it.
public class ParkingLot {
    private final List<ParkingFloor> floors;
    private final PricingStrategy pricing;
    private final Map<String, Ticket> activeTickets = new HashMap<>();

    public ParkingLot(List<ParkingFloor> floors, PricingStrategy pricing) {
        this.floors = floors;
        this.pricing = pricing;
    }

    public Ticket park(Vehicle vehicle) {
        for (ParkingFloor floor : floors) {
            Optional<ParkingSpot> spot = floor.findFreeSpot(vehicle);
            if (spot.isPresent()) {
                spot.get().park(vehicle);
                Ticket ticket = new Ticket(vehicle, spot.get());
                activeTickets.put(ticket.getTicketId(), ticket);
                return ticket;
            }
        }
        throw new IllegalStateException("Parking full for " + vehicle.getType());
    }

    public double exit(String ticketId) {
        Ticket ticket = activeTickets.remove(ticketId);
        if (ticket == null) {
            throw new NoSuchElementException("Unknown ticket " + ticketId);
        }
        ticket.markExit();
        ticket.getSpot().vacate();
        return pricing.calculate(ticket);
    }
}
