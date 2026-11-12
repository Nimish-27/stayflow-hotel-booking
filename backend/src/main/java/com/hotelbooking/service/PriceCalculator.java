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
    private static final BigDecimal GST_RATE = new BigDecimal("0.18");

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
        return subtotal.multiply(GST_RATE).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public StayQuote quote(BigDecimal pricePerNight, LocalDate checkIn, LocalDate checkOut, int roomsAvailable) {
        int nights = nights(checkIn, checkOut);
        BigDecimal subtotal = subtotal(pricePerNight, nights);
        BigDecimal gstAmount = gst(subtotal);
        return new StayQuote(checkIn, checkOut, nights, subtotal, gstAmount,
                subtotal.add(gstAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP), roomsAvailable);
    }
}
