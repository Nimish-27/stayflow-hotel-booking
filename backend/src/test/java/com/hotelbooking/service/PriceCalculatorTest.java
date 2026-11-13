package com.hotelbooking.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.hotelbooking.dto.StayQuote;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PriceCalculatorTest {

    private final PriceCalculator calculator = new PriceCalculator(TestFixtures.RULES);

    @Test
    void nightsIsCheckOutMinusCheckIn() {
        assertThat(calculator.nights(LocalDate.of(2025, 6, 20), LocalDate.of(2025, 6, 23))).isEqualTo(3);
    }

    @Test
    void checkOutDayIsNotCountedAsANight() {
        assertThat(calculator.nights(LocalDate.of(2025, 6, 20), LocalDate.of(2025, 6, 21))).isEqualTo(1);
    }

    @Test
    void nightsCountsLeapDayInLeapYears() {
        assertThat(calculator.nights(LocalDate.of(2028, 2, 28), LocalDate.of(2028, 3, 1))).isEqualTo(2);
        assertThat(calculator.nights(LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 1))).isEqualTo(1);
    }

    @Test
    void totalIsNightsTimesPriceWithoutRoundingErrors() {
        BigDecimal total = calculator.total(new BigDecimal("1999.99"), 3);

        assertThat(total).isEqualByComparingTo("7079.96");
    }

    @Test
    void totalAlwaysHasTwoDecimalPlaces() {
        assertThat(calculator.total(new BigDecimal("3000.00"), 2).toPlainString()).isEqualTo("7080.00");
    }

    @Test
    void quoteCarriesDatesNightsTotalAndRooms() {
        LocalDate checkIn = LocalDate.of(2025, 6, 20);
        LocalDate checkOut = LocalDate.of(2025, 6, 22);

        StayQuote quote = calculator.quote(new BigDecimal("3000.00"), checkIn, checkOut, 4);

        assertThat(quote.checkIn()).isEqualTo(checkIn);
        assertThat(quote.checkOut()).isEqualTo(checkOut);
        assertThat(quote.nights()).isEqualTo(2);
        assertThat(quote.subtotal()).isEqualByComparingTo("6000.00");
        assertThat(quote.gstAmount()).isEqualByComparingTo("1080.00");
        assertThat(quote.totalPrice()).isEqualByComparingTo("7080.00");
        assertThat(quote.roomsAvailable()).isEqualTo(4);
    }
}
