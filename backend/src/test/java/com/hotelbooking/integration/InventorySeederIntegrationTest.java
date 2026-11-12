package com.hotelbooking.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.hotelbooking.config.BookingRules;
import java.sql.Date;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InventorySeederIntegrationTest extends AbstractIntegrationTest {

    private static final int MISSING_DAYS = 5;

    @Autowired
    private BookingRules rules;

    @Test
    void hotelGetsTheConfiguredNumberOfNightsAndSeedingAgainAddsNothing() {
        // the base class already seeded the new test hotel once
        assertThat(seededNights()).isEqualTo(rules.inventorySeedDays());

        assertThat(inventorySeeder.seedMissingInventory()).isZero();

        assertThat(seededNights()).isEqualTo(rules.inventorySeedDays());
    }

    @Test
    void topUpAddsOnlyTheMissingLastNights() {
        Date firstRemoved = Date.valueOf(today().plusDays(rules.inventorySeedDays() - MISSING_DAYS));
        jdbc.update("DELETE FROM room_inventory WHERE hotel_id = ? AND stay_date >= ?", testHotelId, firstRemoved);

        assertThat(inventorySeeder.seedMissingInventory()).isEqualTo(MISSING_DAYS);

        assertThat(seededNights()).isEqualTo(rules.inventorySeedDays());
    }

    private int seededNights() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM room_inventory WHERE hotel_id = ?",
                Integer.class, testHotelId);
    }
}
