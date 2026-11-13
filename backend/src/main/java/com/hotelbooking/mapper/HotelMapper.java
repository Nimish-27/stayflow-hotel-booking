package com.hotelbooking.mapper;

import com.hotelbooking.dto.HotelResponse;
import com.hotelbooking.dto.StayQuote;
import com.hotelbooking.dto.RoomOption;
import com.hotelbooking.entity.Hotel;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class HotelMapper {

    /** quote may be null when the caller did not pass dates. */
    public HotelResponse toResponse(Hotel hotel, StayQuote quote) {
        return toResponse(hotel, quote, hotel.getPricePerNight());
    }

    public HotelResponse toResponse(Hotel hotel, StayQuote quote, BigDecimal pricePerNight) {
        return new HotelResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getLocation(),
                hotel.getDescription(),
                toAmenityList(hotel.getAmenities()),
                hotel.getRoomType(),
                toAmenityList(hotel.getRoomTypes()),
                toRoomOptions(hotel),
                hotel.getMaxOccupancy(),
                hotel.getTotalRooms(),
                pricePerNight,
                quote);
    }

    private List<String> toAmenityList(String amenities) {
        return Arrays.stream(amenities.split(","))
                .map(String::trim)
                .filter(amenity -> !amenity.isEmpty())
                .toList();
    }

    private List<RoomOption> toRoomOptions(Hotel hotel) {
        if (hotel.getRoomTypePrices() == null || hotel.getRoomTypePrices().isBlank()) {
            return List.of(new RoomOption(hotel.getRoomType(), "Double", "Queen",
                    "Comfortable room with essential amenities", hotel.getPricePerNight(), hotel.getTotalRooms()));
        }
        Map<String, BigDecimal> prices = Arrays.stream(hotel.getRoomTypePrices().split(","))
                .map(option -> option.split("=", 2))
                .filter(option -> option.length == 2)
                .collect(LinkedHashMap::new,
                        (result, option) -> result.put(option[0].trim(), new BigDecimal(option[1].trim())),
                        Map::putAll);
        Map<String, String[]> details = new LinkedHashMap<>();
        if (hotel.getRoomTypeDetails() != null && !hotel.getRoomTypeDetails().isBlank()) {
            Arrays.stream(hotel.getRoomTypeDetails().split(";"))
                .map(option -> option.split("\\|", 4))
                .filter(option -> option.length == 4)
                .forEach(option -> details.put(option[0].trim(), option));
        }
        return prices.entrySet().stream()
                .map(option -> {
                    String[] detail = details.get(option.getKey());
                    String occupancy = detail == null ? "Double" : detail[1].trim();
                    String bed = detail == null ? "Queen" : detail[2].trim();
                    String description = detail == null
                            ? "Comfortable room with essential amenities"
                            : detail[3].trim();
                    return new RoomOption(option.getKey(), occupancy, bed, description,
                            option.getValue(), hotel.roomsForRoomType(option.getKey()));
                })
                .toList();
    }
}