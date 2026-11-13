package com.hotelbooking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Price and availability of a specific stay. Also used for each "alternative dates" suggestion. */
public record StayQuote(
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal gstAmount,
        BigDecimal totalPrice,
        int roomsAvailable) {
}