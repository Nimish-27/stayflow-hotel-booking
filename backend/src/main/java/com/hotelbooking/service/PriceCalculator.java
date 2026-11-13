package com.hotelbooking.service;

import com.hotelbooking.dto.StayQuote;
import com.hotelbooking.config.BookingRules;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/** Nights and money. All prices are computed here, on the server, with BigDecimal. */
@Component
public class PriceCalculator {

    private static final int MONEY_SCALE = 2;
    private final BigDecimal gstRate;

    public PriceCalculator(BookingRules rules) {
        this.gstRate = rules.gstRate();
    }

    /** The check-out day is not a night: [checkIn, checkOut). */
    public int nights(LocalDate checkIn, LocalDate checkOut) {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public BigDecimal total(BigDecimal pricePerNight, int nights) {
        BigDecimal subtotal = subtotal(pricePerNight, nights);
        return subtotal.add(gst(subtotal)).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal subtotal(BigDecimal pricePerNight, int nights) {
        return pricePerNight.multiply(BigDecimal.valueOf(nights)).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal gst(BigDecimal subtotal) {
        return subtotal.multiply(gstRate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal gstRate() {
        return gstRate;
    }

    public StayQuote quote(BigDecimal pricePerNight, LocalDate checkIn, LocalDate checkOut, int roomsAvailable) {
        return quote(pricePerNight, checkIn, checkOut, roomsAvailable, 1);
    }

    public StayQuote quote(BigDecimal pricePerNight, LocalDate checkIn, LocalDate checkOut,
                           int roomsAvailable, int rooms) {
        int nights = nights(checkIn, checkOut);
        BigDecimal subtotal = subtotal(pricePerNight, nights)
                .multiply(BigDecimal.valueOf(rooms))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal gstAmount = gst(subtotal);
        return new StayQuote(checkIn, checkOut, nights, subtotal, gstAmount,
                subtotal.add(gstAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP), roomsAvailable);
    }
}
