package com.hotelbooking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.hotelbooking.dto.BookingResponse;
import com.hotelbooking.entity.BookingStatus;
import com.hotelbooking.exception.BookingNotFoundException;
import com.hotelbooking.exception.SoldOutException;
import com.hotelbooking.service.BookingService;
import com.hotelbooking.service.HotelService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BookingFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String ASHA = "asha@example.com";

    @Autowired
    private BookingService bookingService;
    @Autowired
    private HotelService hotelService;

    @Test
    void bookRetrieveAndCancelFlow() {
        LocalDate checkIn = today().plusDays(5);
        LocalDate checkOut = checkIn.plusDays(3);

        // book
        BookingResponse booked = bookingService.createBooking(request(ASHA, checkIn, checkOut));
        assertThat(booked.reference()).matches("HB-[A-HJ-NP-Z2-9]{8}");
        assertThat(booked.totalAmount()).isEqualByComparingTo("3540.00");
        assertThat(booked.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(bookedRooms(checkIn)).isEqualTo(1);
        assertThat(bookedRooms(checkOut.minusDays(1))).isEqualTo(1);
        assertThat(bookedRooms(checkOut)).isZero(); // the check-out day is not a booked night
        assertThat(hotelService.getHotel(testHotelId, checkIn, checkOut).quote().roomsAvailable()).isZero();

        // retrieve: reference and email are case and whitespace insensitive
        BookingResponse found = bookingService.getBooking(booked.reference().toLowerCase(), "  ASHA@Example.com ");
        assertThat(found.reference()).isEqualTo(booked.reference());

        // wrong email and unknown reference look exactly the same
        Throwable wrongEmail = catchThrowable(
                () -> bookingService.getBooking(booked.reference(), "someone.else@example.com"));
        Throwable unknownReference = catchThrowable(() -> bookingService.getBooking("HB-ZZZZZZZZ", ASHA));
        assertThat(wrongEmail).isInstanceOf(BookingNotFoundException.class)
                .hasMessage(unknownReference.getMessage());

        // back-to-back stay (starts on the first one's check-out day) is allowed, an overlap is not
        bookingService.createBooking(request("ben@example.com", checkOut, checkOut.plusDays(2)));
        assertThatThrownBy(() -> bookingService.createBooking(
                request("cara@example.com", checkIn.plusDays(2), checkOut.plusDays(1))))
                .isInstanceOf(SoldOutException.class);

        // cancel releases exactly the nights of the first booking
        BookingResponse cancelled = bookingService.cancelBooking(booked.reference(), ASHA);
        assertThat(cancelled.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(cancelled.cancelledAt()).isNotNull();
        assertThat(bookedRooms(checkIn)).isZero();
        assertThat(bookedRooms(checkOut.minusDays(1))).isZero();
        assertThat(bookedRooms(checkOut)).isEqualTo(1); // the back-to-back booking is untouched

        // cancelling again is harmless and does not release anything twice
        BookingResponse cancelledAgain = bookingService.cancelBooking(booked.reference(), ASHA);
        assertThat(cancelledAgain.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(bookedRooms(checkIn)).isZero();
        assertThat(bookedRooms(checkOut)).isEqualTo(1);

        // the released nights can be booked again
        BookingResponse rebooked = bookingService.createBooking(request("dev@example.com", checkIn, checkOut));
        assertThat(rebooked.reference()).isNotEqualTo(booked.reference());
    }
}
