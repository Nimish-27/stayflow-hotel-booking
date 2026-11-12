package com.hotelbooking.service;

import com.hotelbooking.config.BookingRules;
import com.hotelbooking.exception.InvalidStayException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/** The date rules of a stay. The only class that knows what "valid dates" means. */
@Component
public class StayValidator {

    private final BookingRules rules;
    private final Clock clock;

    public StayValidator(BookingRules rules, Clock clock) {
        this.rules = rules;
        this.clock = clock;
    }

    public void validate(LocalDate checkIn, LocalDate checkOut) {
        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidStayException("Check-out must be after check-in");
        }
        if (isInPast(checkIn)) {
            throw new InvalidStayException("Check-in cannot be in the past");
        }
        if (ChronoUnit.DAYS.between(checkIn, checkOut) > rules.maxNights()) {
            throw new InvalidStayException("Stay cannot be longer than " + rules.maxNights() + " nights");
        }
        if (endsAfterWindow(checkOut)) {
            throw new InvalidStayException(
                    "Bookings are limited to the next " + rules.windowDays() + " days");
        }
    }

    /** Used by alternative dates to skip candidate stays that could never be booked. */
    public boolean isInsideBookingWindow(LocalDate checkIn, LocalDate checkOut) {
        return !isInPast(checkIn) && !endsAfterWindow(checkOut);
    }

    private boolean isInPast(LocalDate checkIn) {
        return checkIn.isBefore(today());
    }

    private boolean endsAfterWindow(LocalDate checkOut) {
        return checkOut.isAfter(today().plusDays(rules.windowDays()));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
