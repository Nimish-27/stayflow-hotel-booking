package com.hotelbooking.service;

import com.hotelbooking.config.BookingRules;
import com.hotelbooking.entity.Hotel;
import com.hotelbooking.entity.RoomTypeInventory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Function;
import org.springframework.test.util.ReflectionTestUtils;

/** Shared test data: a fixed "today" (10 June 2025, Asia/Kolkata), real rules and small builders. */
final class TestFixtures {

    static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    static final Clock CLOCK = Clock.fixed(Instant.parse("2025-06-10T06:00:00Z"), ZONE);
    static final LocalDate TODAY = LocalDate.of(2025, 6, 10);
    static final BookingRules RULES = new BookingRules(30, 365, 372, 7, 3, 48, 2,
            new BigDecimal("0.18"));
    static final Long HOTEL_ID = 1L;
    static final LocalDate CHECK_IN = TODAY.plusDays(10);

    private TestFixtures() {
    }

    /** A hotel with price 3000.00 per night. The id is set by reflection because it is DB-generated. */
    static Hotel hotel(int totalRooms) {
        Hotel hotel = new Hotel("Test Hotel", "Goa", "Nice place", "WiFi,Pool",
                new BigDecimal("3000.00"), totalRooms);
        ReflectionTestUtils.setField(hotel, "id", HOTEL_ID);
        return hotel;
    }

    static List<RoomTypeInventory> typeInventory(LocalDate from, LocalDate toExclusive, int booked) {
        return typeInventory(from, toExclusive, night -> booked);
    }

    static List<RoomTypeInventory> typeInventory(LocalDate from, LocalDate toExclusive,
                                                  Function<LocalDate, Integer> bookedOn) {
        return from.datesUntil(toExclusive)
                .map(night -> new RoomTypeInventory(HOTEL_ID, "Standard Room", night,
                        bookedOn.apply(night)))
                .toList();
    }

    /** True if the night belongs to the stay [checkIn, checkOut). */
    static boolean isNightOf(LocalDate night, LocalDate checkIn, LocalDate checkOut) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }
}
