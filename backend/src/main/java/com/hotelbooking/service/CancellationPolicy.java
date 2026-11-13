package com.hotelbooking.service;

import com.hotelbooking.config.BookingRules;
import com.hotelbooking.entity.Booking;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class CancellationPolicy {

    private final Clock clock;
    private final BookingRules rules;

    public CancellationPolicy(Clock clock, BookingRules rules) {
        this.clock = clock;
        this.rules = rules;
    }

    public Instant deadline(Booking booking) {
        ZoneId zone = clock.getZone();
        Instant standard = booking.getCheckIn().atStartOfDay(zone)
                .minusHours(rules.freeCancellationHours()).toInstant();
        Instant grace = booking.getCreatedAt()
                .plus(Duration.ofHours(rules.cancellationGraceHours()));
        Instant checkIn = booking.getCheckIn().atStartOfDay(zone).toInstant();
        return booking.getCreatedAt().isAfter(standard)
                ? (grace.isBefore(checkIn) ? grace : checkIn)
                : standard;
    }

    public boolean isCancellable(Booking booking, Instant now) {
        return !booking.isCancelled() && now.isBefore(deadline(booking));
    }

    public boolean isCancellable(Booking booking) {
        return isCancellable(booking, Instant.now(clock));
    }
}
