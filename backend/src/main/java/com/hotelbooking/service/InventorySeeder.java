package com.hotelbooking.service;

import com.hotelbooking.config.BookingRules;
import com.hotelbooking.entity.Hotel;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.RoomTypeInventoryRepository;
import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps room-type inventory filled from today up to today + inventory-seed-days.
 * Idempotent: it only adds nights after each hotel's latest existing night, so running it
 * again changes nothing and it never touches rows that bookings may be locking.
 */
@Service
public class InventorySeeder {

    private static final Logger log = LoggerFactory.getLogger(InventorySeeder.class);

    private final HotelRepository hotelRepository;
    private final RoomTypeInventoryRepository roomTypeInventoryRepository;
    private final BookingRules rules;
    private final Clock clock;

    public InventorySeeder(HotelRepository hotelRepository,
                           RoomTypeInventoryRepository roomTypeInventoryRepository, BookingRules rules, Clock clock) {
        this.hotelRepository = hotelRepository;
        this.roomTypeInventoryRepository = roomTypeInventoryRepository;
        this.rules = rules;
        this.clock = clock;
    }

    /** Returns how many night rows were added. */
    @Transactional
    public int seedMissingInventory() {
        LocalDate today = LocalDate.now(clock);
        LocalDate lastNight = today.plusDays(rules.inventorySeedDays() - 1L);
        int inserted = 0;
        for (Hotel hotel : hotelRepository.findAll()) {
            for (String type : hotel.getRoomTypes().split(",")) {
                inserted += seedType(hotel.getId(), type.trim(), today, lastNight);
            }
        }

        log.info("Inventory top-up finished: {} night rows added (up to {})", inserted, lastNight);
        return inserted;
    }

    private int seedType(Long hotelId, String roomType, LocalDate today, LocalDate lastNight) {
            LocalDate first = roomTypeInventoryRepository.findLastStayDate(hotelId, roomType)
                    .map(last -> last.plusDays(1)).orElse(today);
            int inserted = 0;
            for (LocalDate night = first.isAfter(today) ? first : today; !night.isAfter(lastNight); night = night.plusDays(1)) {
                inserted += roomTypeInventoryRepository.insertNight(hotelId, roomType, night);
            }
        return inserted;
    }

}
