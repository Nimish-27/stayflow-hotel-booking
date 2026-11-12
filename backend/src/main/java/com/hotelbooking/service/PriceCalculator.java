package com.hotelbooking.service;

import com.hotelbooking.dto.StayQuote;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/** Nights and money. All prices are computed here, on the server, with BigDecimal. */
@Component
public class PriceCalculator {

    private static final int MONEY_SCALE = 2;

    /** The check-out day is not a night: [checkIn, checkOut). */
    public int nights(LocalDate checkIn, LocalDate checkOut) {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public BigDecimal total(BigDecimal pricePerNight, int nights) {
        return pricePerNight.multiply(BigDecimal.valueOf(nights)).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public StayQuote quote(BigDecimal pricePerNight, LocalDate checkIn, LocalDate checkOut, int roomsAvailable) {
        int nights = nights(checkIn, checkOut);
        return new StayQuote(checkIn, checkOut, nights, total(pricePerNight, nights), roomsAvailable);
    }
}
