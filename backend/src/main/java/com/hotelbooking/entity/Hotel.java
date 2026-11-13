package com.hotelbooking.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import com.hotelbooking.exception.InvalidStayException;

/** Read-only reference data: the app never changes a hotel, so there are no setters. */
@Entity
@Table(name = "hotels")
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String location;
    private String description;
    private String amenities; // comma-separated, split in HotelMapper
    private String roomType;
    private String roomTypes; // comma-separated, split in HotelMapper
    private String roomTypePrices; // comma-separated name=price pairs
    private String roomTypeDetails; // semicolon-separated name|occupancy|bed|description records
    private String roomTypeCounts; // comma-separated name=count pairs
    private int maxOccupancy;
    private BigDecimal pricePerNight;
    private int totalRooms;

    protected Hotel() {
        // required by JPA
    }

    public Hotel(String name, String location, String description, String amenities,
                 BigDecimal pricePerNight, int totalRooms) {
        this(name, location, description, amenities, "Standard Room", 2, pricePerNight, totalRooms);
    }

    public Hotel(String name, String location, String description, String amenities,
                 String roomType, int maxOccupancy, BigDecimal pricePerNight, int totalRooms) {
        this.name = name;
        this.location = location;
        this.description = description;
        this.amenities = amenities;
        this.roomType = roomType;
        this.roomTypes = roomType;
        this.roomTypePrices = roomType + "=" + pricePerNight;
        this.roomTypeDetails = roomType + "|Double|Queen|Comfortable room with essential amenities";
        this.roomTypeCounts = roomType + "=" + totalRooms;
        this.maxOccupancy = maxOccupancy;
        this.pricePerNight = pricePerNight;
        this.totalRooms = totalRooms;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
    public String getAmenities() { return amenities; }
    public String getRoomType() { return roomType; }
    public String getRoomTypes() { return roomTypes; }
    public String getRoomTypePrices() { return roomTypePrices; }
    public String getRoomTypeDetails() { return roomTypeDetails; }
    public String getRoomTypeCounts() { return roomTypeCounts; }
    public int roomsForRoomType(String requestedRoomType) {
        if (roomTypeCounts == null || roomTypeCounts.isBlank()) {
            return totalRooms;
        }
        return Arrays.stream(roomTypeCounts.split(","))
                .map(option -> option.split("=", 2))
                .filter(option -> option.length == 2 && option[0].trim().equals(requestedRoomType))
                .mapToInt(option -> parsePositiveCount(option[1].trim(), requestedRoomType))
                .findFirst()
                .orElse(totalRooms);
    }
    public String resolveRoomType(String requestedRoomType) {
            validateRoomConfiguration();
            if (requestedRoomType == null || requestedRoomType.isBlank()) return roomType;
            boolean known = Arrays.stream(roomTypePrices == null ? new String[0] : roomTypePrices.split(","))
                    .map(option -> option.split("=", 2)[0].trim())
                    .anyMatch(requestedRoomType.trim()::equals);
            if (!known) throw new InvalidStayException("Unknown room type: " + requestedRoomType);
            return requestedRoomType.trim();
        }
    public int maxOccupancyForRoomType(String requestedRoomType) {
            if (roomTypeDetails == null) return maxOccupancy;
            return Arrays.stream(roomTypeDetails.split(";"))
                    .map(option -> option.split("\\|", 4))
                    .filter(option -> option.length == 4 && option[0].trim().equals(requestedRoomType))
                    .map(option -> switch (option[1].trim()) {
                        case "Single" -> 1; case "Double" -> 2; case "Triple" -> 3; case "Quad" -> 4;
                        default -> throw new InvalidStayException("Invalid occupancy for room type: " + requestedRoomType);
                    }).findFirst().orElse(maxOccupancy);
    }
    public BigDecimal priceForRoomType(String requestedRoomType) {
        if (roomTypePrices == null || roomTypePrices.isBlank()) {
            return pricePerNight;
        }
        Map<String, BigDecimal> prices = Arrays.stream(roomTypePrices.split(","))
                .map(option -> option.split("=", 2))
                .filter(option -> option.length == 2)
                .collect(Collectors.toMap(option -> option[0].trim(),
                        option -> parsePrice(option[1].trim()),
                        (first, second) -> { throw new InvalidStayException("Duplicate room type price"); }));
        return prices.getOrDefault(requestedRoomType, pricePerNight);
    }

    private int parsePositiveCount(String value, String type) {
        try {
            int count = Integer.parseInt(value);
            if (count < 1) throw new NumberFormatException();
            return count;
        } catch (NumberFormatException ex) {
            throw new InvalidStayException("Invalid room count for room type: " + type);
        }
    }

    private BigDecimal parsePrice(String value) {
        try {
            BigDecimal price = new BigDecimal(value);
            if (price.signum() <= 0) throw new NumberFormatException();
            return price;
        } catch (NumberFormatException ex) {
            throw new InvalidStayException("Invalid room type price");
        }
    }

    public void validateRoomConfiguration() {
        if (roomTypePrices == null || roomTypePrices.isBlank()) return;
        String[] prices = roomTypePrices.split(",");
        for (String option : prices) {
            String[] pair = option.split("=", 2);
            if (pair.length != 2 || pair[0].trim().isEmpty()) {
                throw new InvalidStayException("Invalid room type configuration");
            }
            parsePrice(pair[1].trim());
            roomsForRoomType(pair[0].trim());
        }
    }
    public int getMaxOccupancy() { return maxOccupancy; }
    public BigDecimal getPricePerNight() { return pricePerNight; }
    public int getTotalRooms() { return totalRooms; }
}