package com.hotelbooking.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Business limits, read from the app.booking.* properties. */
@Validated
@ConfigurationProperties(prefix = "app.booking")
public record BookingRules(
        @Min(1) int maxNights,
        @Min(1) int windowDays,
        @Min(1) int inventorySeedDays,
        @Min(1) int alternativeShiftDays,
        @Min(1) int maxAlternatives,
        @Min(1) int freeCancellationHours) {

    public BookingRules {
        if (inventorySeedDays < windowDays) {
            throw new IllegalArgumentException(
                    "app.booking.inventory-seed-days must be >= app.booking.window-days");
        }
    }
}