package com.hotelbooking.service;

import static com.hotelbooking.service.TestFixtures.HOTEL_ID;
import static org.assertj.core.api.Assertions.assertThat;

import com.hotelbooking.entity.RoomInventory;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AvailabilityCalculatorTest {

    private static final LocalDate D10 = LocalDate.of(2025, 7, 10);
    private static final LocalDate D11 = D10.plusDays(1);
    private static final LocalDate D12 = D10.plusDays(2);
    private static final LocalDate D13 = D10.plusDays(3);

    private final AvailabilityCalculator calculator = new AvailabilityCalculator();

    @Test
    void allRoomsAreFreeWhenNothingIsBooked() {
        Map<LocalDate, Integer> booked = Map.of(D10, 0, D11, 0, D12, 0);

        assertThat(calculator.roomsAvailable(3, booked, D10, D13)).isEqualTo(3);
    }

    @Test
    void availabilityIsLimitedByTheBusiestNight() {
        Map<LocalDate, Integer> booked = Map.of(D10, 0, D11, 2, D12, 1);

        assertThat(calculator.roomsAvailable(3, booked, D10, D13)).isEqualTo(1);
    }

    @Test
    void stayIsSoldOutWhenAnySingleNightIsFull() {
        Map<LocalDate, Integer> booked = Map.of(D10, 0, D11, 3, D12, 0);

        assertThat(calculator.roomsAvailable(3, booked, D10, D13)).isZero();
        assertThat(calculator.isAvailable(3, booked, D10, D13)).isFalse();
    }

    @Test
    void backToBackStaysDoNotConflict() {
        // A one-room hotel with a booking for [D10, D12): nights D10 and D11 are taken.
        Map<LocalDate, Integer> booked = Map.of(D10, 1, D11, 1, D12, 0, D13, 0);

        assertThat(calculator.isAvailable(1, booked, D10, D12)).isFalse();
        assertThat(calculator.isAvailable(1, booked, D12, D13.plusDays(1))).isTrue(); // starts on the checkout day
    }

    @Test
    void stayThatOverlapsTheLastBookedNightConflicts() {
        Map<LocalDate, Integer> booked = Map.of(D10, 1, D11, 1, D12, 0, D13, 0);

        assertThat(calculator.isAvailable(1, booked, D11, D13)).isFalse();
    }

    @Test
    void nightWithoutInventoryRowIsTreatedAsUnavailable() {
        Map<LocalDate, Integer> booked = Map.of(D10, 0, D11, 0); // no row for D12

        assertThat(calculator.roomsAvailable(3, booked, D10, D13)).isZero();
    }

    @Test
    void bookedByNightMapsInventoryRowsByDate() {
        List<RoomInventory> rows = List.of(
                new RoomInventory(HOTEL_ID, D10, 2), new RoomInventory(HOTEL_ID, D11, 0));

        assertThat(calculator.bookedByNight(rows)).containsEntry(D10, 2).containsEntry(D11, 0).hasSize(2);
    }
}
