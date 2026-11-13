package com.hotelbooking.dto;

import com.hotelbooking.util.InputNormalizer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Format checks only. Rules that need the clock or the database (past dates, max nights,
 * availability) are checked in the service. Name and email are cleaned in the constructor,
 * so validation and the service always see the trimmed values.
 */
public record CreateBookingRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String guestName,

        @NotBlank(message = "Email is required")
        @Size(max = 254, message = "Email must be at most 254 characters")
        @Pattern(regexp = "^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$", message = "Email format is invalid")
        String guestEmail,

        @NotNull(message = "Hotel is required")
        Long hotelId,

        @NotNull(message = "Check-in date is required")
        LocalDate checkIn,

        @NotNull(message = "Check-out date is required")
        LocalDate checkOut,
        String roomType,
        Integer adults,
        Integer children,
        Integer rooms) {

    public CreateBookingRequest(String guestName, String guestEmail, Long hotelId,
                                LocalDate checkIn, LocalDate checkOut) {
        this(guestName, guestEmail, hotelId, checkIn, checkOut, null, 2, 0, 1);
    }

    public CreateBookingRequest(String guestName, String guestEmail, Long hotelId,
                                LocalDate checkIn, LocalDate checkOut, String roomType) {
        this(guestName, guestEmail, hotelId, checkIn, checkOut, roomType, 2, 0, 1);
    }

    public CreateBookingRequest {
        guestName = InputNormalizer.trim(guestName);
        guestEmail = InputNormalizer.email(guestEmail);
        adults = adults == null ? 2 : adults;
        children = children == null ? 0 : children;
        rooms = rooms == null ? 1 : rooms;
    }
}