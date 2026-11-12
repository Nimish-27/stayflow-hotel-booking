package com.hotelbooking.mapper;

import com.hotelbooking.dto.HotelResponse;
import com.hotelbooking.dto.StayQuote;
import com.hotelbooking.entity.Hotel;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class HotelMapper {

    /** quote may be null when the caller did not pass dates. */
    public HotelResponse toResponse(Hotel hotel, StayQuote quote) {
        return new HotelResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getLocation(),
                hotel.getDescription(),
                toAmenityList(hotel.getAmenities()),
                hotel.getPricePerNight(),
                quote);
    }

    private List<String> toAmenityList(String amenities) {
        return Arrays.stream(amenities.split(","))
                .map(String::trim)
                .filter(amenity -> !amenity.isEmpty())
                .toList();
    }
}