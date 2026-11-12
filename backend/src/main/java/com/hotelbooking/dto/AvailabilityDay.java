package com.hotelbooking.dto;

import java.time.LocalDate;

public record AvailabilityDay(LocalDate date, int roomsAvailable, String level) {
}
