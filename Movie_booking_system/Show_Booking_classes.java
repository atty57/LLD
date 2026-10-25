// Show, seat-hold concurrency, and the booking service.

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// A screening of a movie at a time. Owns the per-seat state for THIS show and
// the locking that prevents two users booking the same seat.
public class Show {
    private final String showId;
    private final Movie movie;
    private final Screen screen;
    private final long startTimeMillis;
    private final Map<String, SeatState> seatStates = new ConcurrentHashMap<>();
    private final Map<String, Long> holdExpiry = new ConcurrentHashMap<>();  // seatId -> epoch ms

    private static final long HOLD_MILLIS = 5 * 60 * 1000;  // 5-minute hold

    public Show(String showId, Movie movie, Screen screen, long startTimeMillis) {
        this.showId = showId;
        this.movie = movie;
        this.screen = screen;
        this.startTimeMillis = startTimeMillis;
        for (Seat s : screen.getSeats()) seatStates.put(s.getSeatId(), SeatState.AVAILABLE);
    }

    // Try to hold seats for a user. All-or-nothing, and atomic across callers.
    // ponytail: single synchronized block per show — correct and simple. Shard by
    // seat-row only if one blockbuster show becomes a contention hot spot.
    public synchronized boolean hold(List<String> seatIds) {
        long now = System.currentTimeMillis();
        for (String id : seatIds) {
            if (!isFree(id, now)) return false;
        }
        for (String id : seatIds) {
            seatStates.put(id, SeatState.HELD);
            holdExpiry.put(id, now + HOLD_MILLIS);
        }
        return true;
    }

    public synchronized void confirm(List<String> seatIds) {
        for (String id : seatIds) {
            if (seatStates.get(id) != SeatState.HELD) {
                throw new IllegalStateException("Seat " + id + " not held (hold may have expired)");
            }
        }
        for (String id : seatIds) {
            seatStates.put(id, SeatState.BOOKED);
            holdExpiry.remove(id);
        }
    }

    public synchronized void release(List<String> seatIds) {
        for (String id : seatIds) {
            if (seatStates.get(id) == SeatState.HELD) {
                seatStates.put(id, SeatState.AVAILABLE);
                holdExpiry.remove(id);
            }
        }
    }

    private boolean isFree(String seatId, long now) {
        SeatState state = seatStates.get(seatId);
        if (state == SeatState.AVAILABLE) return true;
        // A HELD seat whose hold has expired is effectively free again.
        return state == SeatState.HELD && holdExpiry.getOrDefault(seatId, 0L) < now;
    }

    public List<String> availableSeats() {
        long now = System.currentTimeMillis();
        List<String> free = new ArrayList<>();
        for (String id : seatStates.keySet()) if (isFree(id, now)) free.add(id);
        return free;
    }

    public String getShowId() { return showId; }
    public Movie getMovie() { return movie; }
}

public class Booking {
    private final String bookingId;
    private final Show show;
    private final String userId;
    private final List<String> seatIds;
    private BookingStatus status;

    public Booking(Show show, String userId, List<String> seatIds) {
        this.bookingId = UUID.randomUUID().toString();
        this.show = show;
        this.userId = userId;
        this.seatIds = seatIds;
        this.status = BookingStatus.PENDING;
    }

    void markConfirmed() { this.status = BookingStatus.CONFIRMED; }
    void markCancelled() { this.status = BookingStatus.CANCELLED; }
    public BookingStatus getStatus() { return status; }
    public List<String> getSeatIds() { return seatIds; }
}

// Orchestrates the hold -> pay -> confirm flow.
public class BookingService {
    // Reserve seats and create a PENDING booking. Caller then collects payment.
    public Booking reserve(Show show, String userId, List<String> seatIds) {
        if (!show.hold(seatIds)) {
            throw new IllegalStateException("One or more seats are no longer available");
        }
        return new Booking(show, userId, seatIds);
    }

    // Payment succeeded -> lock the seats in.
    public void confirm(Show show, Booking booking) {
        show.confirm(booking.getSeatIds());
        booking.markConfirmed();
    }

    // User abandoned or payment failed -> free the seats.
    public void cancel(Show show, Booking booking) {
        show.release(booking.getSeatIds());
        booking.markCancelled();
    }
}

enum BookingStatus { PENDING, CONFIRMED, CANCELLED }
