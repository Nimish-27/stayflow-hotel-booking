package com.hotelbooking.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.hotelbooking.dto.CreateBookingRequest;
import com.hotelbooking.entity.Hotel;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.service.InventorySeeder;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Starts the whole app against the test schema. Before each test it wipes all bookings, resets all
 * counters, and creates a fresh ONE-room hotel ("IT ..."), so a single booking fills it up.
 * Tests are deliberately NOT @Transactional: concurrent threads need real, committed transactions.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
abstract class AbstractIntegrationTest {

    protected static final BigDecimal TEST_HOTEL_PRICE = new BigDecimal("1000.00");

    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected HotelRepository hotelRepository;
    @Autowired
    protected InventorySeeder inventorySeeder;
    @Autowired
    protected Clock clock;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    protected Long testHotelId;

    @BeforeEach
    void resetTestData() {
        assertThat(datasourceUrl)
                .as("these tests delete data, so they must run on the test schema")
                .contains("hotel_booking_test");

        jdbc.update("DELETE FROM bookings");
        jdbc.update("DELETE FROM room_inventory WHERE hotel_id IN (SELECT id FROM hotels WHERE name LIKE 'IT %')");
        jdbc.update("DELETE FROM hotels WHERE name LIKE 'IT %'");
        jdbc.update("UPDATE room_inventory SET booked_rooms = 0");

        testHotelId = hotelRepository.save(new Hotel("IT Last Room Hotel", "Testville",
                "Single-room hotel used by integration tests", "WiFi", TEST_HOTEL_PRICE, 1)).getId();
        inventorySeeder.seedMissingInventory();
    }

    protected LocalDate today() {
        return LocalDate.now(clock);
    }

    protected CreateBookingRequest request(String email, LocalDate checkIn, LocalDate checkOut) {
        return new CreateBookingRequest("Test Guest", email, testHotelId, checkIn, checkOut);
    }

    protected int bookedRooms(LocalDate night) {
        return jdbc.queryForObject(
                "SELECT booked_rooms FROM room_inventory WHERE hotel_id = ? AND stay_date = ?",
                Integer.class, testHotelId, Date.valueOf(night));
    }

    protected int confirmedBookingsCovering(LocalDate night) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM bookings
                WHERE hotel_id = ? AND status = 'CONFIRMED' AND check_in <= ? AND check_out > ?
                """, Integer.class, testHotelId, Date.valueOf(night), Date.valueOf(night));
    }

    protected int confirmedBookings() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE hotel_id = ? AND status = 'CONFIRMED'",
                Integer.class, testHotelId);
    }
}
