package com.hotelbooking.dto;

import java.math.BigDecimal;

public record RoomOption(String name, String occupancyType, String bedType,
                         String description, BigDecimal pricePerNight, int totalRooms) {
}
