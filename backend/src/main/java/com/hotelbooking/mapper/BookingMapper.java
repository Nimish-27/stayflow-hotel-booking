package com.hotelbooking.mapper;

import com.hotelbooking.dto.BookingResponse;
import com.hotelbooking.entity.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    /** Call inside the service transaction: it reads the lazy hotel association. */
    public BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getReference(),
                booking.getHotel().getId(),
                booking.getHotel().getName(),
                booking.getGuestName(),
                booking.getGuestEmail(),
                booking.getCheckIn(),
                booking.getCheckOut(),
                booking.getNights(),
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getCancelledAt());
    }
}