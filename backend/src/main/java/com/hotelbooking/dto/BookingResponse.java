package com.hotelbooking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.hotelbooking.entity.BookingStatus;

public record BookingResponse(
        String reference,
        Long hotelId,
        String hotelName,
        String guestName,
        String guestEmail,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal totalAmount,
        BookingStatus status,
        Instant createdAt,
        Instant cancelledAt) {
}