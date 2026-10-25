// Core domain for a movie ticket booking system (BookMyShow-style).
// Design reference, not a single compilable unit (several public classes per file).

import java.util.*;

public class Movie {
    private final String movieId;
    private final String title;
    private final int durationMinutes;

    public Movie(String movieId, String title, int durationMinutes) {
        this.movieId = movieId;
        this.title = title;
        this.durationMinutes = durationMinutes;
    }

    public String getMovieId() { return movieId; }
    public String getTitle() { return title; }
}

// A physical seat in a screen. Immutable identity; its booked-ness lives on the
// Show, not here, because the same seat is free for one show and taken for another.
public class Seat {
    private final String seatId;      // e.g. "A12"
    private final SeatType type;      // REGULAR, PREMIUM, RECLINER

    public Seat(String seatId, SeatType type) {
        this.seatId = seatId;
        this.type = type;
    }

    public String getSeatId() { return seatId; }
    public SeatType getType() { return type; }
}

// A screen (auditorium) in a theatre.
public class Screen {
    private final String screenId;
    private final List<Seat> seats;

    public Screen(String screenId, List<Seat> seats) {
        this.screenId = screenId;
        this.seats = seats;
    }

    public List<Seat> getSeats() { return seats; }
    public String getScreenId() { return screenId; }
}

public class Theatre {
    private final String theatreId;
    private final String name;
    private final String city;
    private final List<Screen> screens;

    public Theatre(String theatreId, String name, String city, List<Screen> screens) {
        this.theatreId = theatreId;
        this.name = name;
        this.city = city;
        this.screens = screens;
    }

    public String getCity() { return city; }
    public List<Screen> getScreens() { return screens; }
}

enum SeatType { REGULAR, PREMIUM, RECLINER }
enum SeatState { AVAILABLE, HELD, BOOKED }
