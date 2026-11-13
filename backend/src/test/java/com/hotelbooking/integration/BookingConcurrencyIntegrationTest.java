package com.hotelbooking.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.hotelbooking.dto.CreateBookingRequest;
import com.hotelbooking.exception.SoldOutException;
import com.hotelbooking.service.BookingService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

class BookingConcurrencyIntegrationTest extends AbstractIntegrationTest {

    /** Must stay below the default Hikari pool size (10), or threads would wait for connections. */
    private static final int THREADS = 8;
    private static final long TIMEOUT_SECONDS = 60;

    private enum Outcome { BOOKED, SOLD_OUT }

    @Autowired
    private BookingService bookingService;

    @Test
    void onlyOneOfManyConcurrentRequestsGetsTheLastRoom() throws Exception {
        LocalDate checkIn = today().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(3);

        List<Outcome> outcomes = bookConcurrently(i -> request("guest" + i + "@example.com", checkIn, checkOut));

        assertThat(count(outcomes, Outcome.BOOKED)).isEqualTo(1);
        assertThat(count(outcomes, Outcome.SOLD_OUT)).isEqualTo(THREADS - 1);
        for (int night = 0; night < 3; night++) {
            assertThat(bookedRooms(checkIn.plusDays(night))).isEqualTo(1);
        }
        assertThat(confirmedBookings()).isEqualTo(1);
    }

    @Test
    void overlappingStaysNeverOversellAndNeverDeadlock() throws Exception {
        LocalDate firstCheckIn = today().plusDays(20);

        // Stay i covers [first + i, first + i + 3): neighbours overlap, so at most some can win.
        List<Outcome> outcomes = bookConcurrently(i -> request("guest" + i + "@example.com",
                firstCheckIn.plusDays(i), firstCheckIn.plusDays(i + 3L)));

        assertThat(count(outcomes, Outcome.BOOKED)).isGreaterThanOrEqualTo(1);
        for (int day = 0; day < THREADS + 3; day++) {
            LocalDate night = firstCheckIn.plusDays(day);
            assertThat(bookedRooms(night)).isLessThanOrEqualTo(1);
            assertThat(bookedRooms(night)).isEqualTo(confirmedBookingsCovering(night));
        }
    }

    @Test
    void independentRoomTypesCanBookConcurrently() throws Exception {
            var hotel = hotelRepository.findById(testHotelId).orElseThrow();
            ReflectionTestUtils.setField(hotel, "roomTypes", "Standard Room,Deluxe");
            ReflectionTestUtils.setField(hotel, "roomTypePrices", "Standard Room=1000,Deluxe=1200");
            ReflectionTestUtils.setField(hotel, "roomTypeCounts", "Standard Room=1,Deluxe=1");
            ReflectionTestUtils.setField(hotel, "roomTypeDetails",
                    "Standard Room|Double|Queen|Standard;Deluxe|Double|King|Deluxe");
            hotelRepository.saveAndFlush(hotel);
            inventorySeeder.seedMissingInventory();

            LocalDate checkIn = today().plusDays(30);
            LocalDate checkOut = checkIn.plusDays(2);
            List<Outcome> outcomes = bookConcurrently(i -> new CreateBookingRequest(
                    "guest" + i, "guest" + i + "@example.com", testHotelId, checkIn, checkOut,
                    i % 2 == 0 ? "Standard Room" : "Deluxe", 2, 0, 1));

            assertThat(count(outcomes, Outcome.BOOKED)).isEqualTo(2);
            assertThat(roomTypeBooked("Standard Room", checkIn)).isEqualTo(1);
            assertThat(roomTypeBooked("Deluxe", checkIn)).isEqualTo(1);
    }

    /** Starts all threads at the same instant, so they really compete for the same rows. */
    private List<Outcome> bookConcurrently(IntFunction<CreateBookingRequest> requestForThread) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            CountDownLatch allReady = new CountDownLatch(THREADS);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Outcome>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                CreateBookingRequest request = requestForThread.apply(i);
                futures.add(pool.submit(() -> {
                    allReady.countDown();
                    go.await();
                    return tryToBook(request);
                }));
            }
            allReady.await();
            go.countDown();

            List<Outcome> outcomes = new ArrayList<>();
            for (Future<Outcome> future : futures) {
                // Any unexpected exception (e.g. a deadlock) is rethrown here and fails the test.
                outcomes.add(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }

    private Outcome tryToBook(CreateBookingRequest request) {
        try {
            bookingService.createBooking(request);
            return Outcome.BOOKED;
        } catch (SoldOutException e) {
            return Outcome.SOLD_OUT;
        }
    }

    private int roomTypeBooked(String roomType, LocalDate night) {
            return jdbc.queryForObject(
                    "SELECT booked_rooms FROM room_type_inventory "
                            + "WHERE hotel_id = ? AND room_type = ? AND stay_date = ?",
                    Integer.class, testHotelId, roomType, java.sql.Date.valueOf(night));
    }

    private long count(List<Outcome> outcomes, Outcome wanted) {
        return outcomes.stream().filter(outcome -> outcome == wanted).count();
    }
}
