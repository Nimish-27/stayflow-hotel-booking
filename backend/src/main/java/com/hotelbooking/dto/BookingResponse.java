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
        BigDecimal subtotalAmount,
        BigDecimal gstRate,
        BigDecimal gstAmount,
        BigDecimal totalAmount,
        String roomType,
        int adults,
        int children,
        int rooms,
        BookingStatus status,
        Instant createdAt,
        Instant cancelledAt,
        boolean cancellable,
        Instant cancellableUntil) {
}