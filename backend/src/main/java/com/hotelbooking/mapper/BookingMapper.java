package com.hotelbooking.mapper;

import com.hotelbooking.dto.BookingResponse;
import com.hotelbooking.entity.Booking;
import com.hotelbooking.service.CancellationPolicy;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {
    private final CancellationPolicy cancellationPolicy;

    public BookingMapper(CancellationPolicy cancellationPolicy) {
        this.cancellationPolicy = cancellationPolicy;
    }

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
                booking.getSubtotalAmount(),
                booking.getGstRate(),
                booking.getGstAmount(),
                booking.getTotalAmount(),
                booking.getRoomType(),
                booking.getAdults(),
                booking.getChildren(),
                booking.getRooms(),
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getCancelledAt(),
                cancellationPolicy.isCancellable(booking),
                cancellationPolicy.deadline(booking));
    }
}