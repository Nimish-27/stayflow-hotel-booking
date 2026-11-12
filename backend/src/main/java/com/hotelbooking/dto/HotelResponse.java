package com.hotelbooking.dto;

import java.math.BigDecimal;
import java.util.List;

/** quote is null when the request had no dates. */
public record HotelResponse(
        Long id,
        String name,
        String location,
        String description,
        List<String> amenities,
        BigDecimal pricePerNight,
        StayQuote quote) {
}