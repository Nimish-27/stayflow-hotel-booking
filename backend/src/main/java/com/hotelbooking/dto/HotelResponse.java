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
        String roomType,
        List<String> roomTypes,
        List<RoomOption> roomOptions,
        int maxOccupancy,
        int totalRooms,
        BigDecimal pricePerNight,
        StayQuote quote) {
}