package com.hotelbooking.service;

import static com.hotelbooking.service.TestFixtures.CLOCK;
import static com.hotelbooking.service.TestFixtures.RULES;
import static com.hotelbooking.service.TestFixtures.TODAY;
import static com.hotelbooking.service.TestFixtures.ZONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hotelbooking.exception.InvalidStayException;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StayValidatorTest {

    private final StayValidator validator = new StayValidator(RULES, CLOCK);

    @Test
    void acceptsANormalStay() {
        assertThatCode(() -> validator.validate(TODAY.plusDays(5), TODAY.plusDays(8)))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsCheckInToday() {
        assertThatCode(() -> validator.validate(TODAY, TODAY.plusDays(1))).doesNotThrowAnyException();
    }

    @Test
    void rejectsSameDayCheckInAndCheckOut() {
        assertThatThrownBy(() -> validator.validate(TODAY.plusDays(5), TODAY.plusDays(5)))
                .isInstanceOf(InvalidStayException.class)
                .hasMessageContaining("after check-in");
    }

    @Test
    void rejectsCheckOutBeforeCheckIn() {
        assertThatThrownBy(() -> validator.validate(TODAY.plusDays(5), TODAY.plusDays(3)))
                .isInstanceOf(InvalidStayException.class)
                .hasMessageContaining("after check-in");
    }

    @Test
    void rejectsCheckInInThePast() {
        assertThatThrownBy(() -> validator.validate(TODAY.minusDays(1), TODAY.plusDays(2)))
                .isInstanceOf(InvalidStayException.class)
                .hasMessageContaining("past");
    }

    @Test
    void acceptsExactlyTheMaximumNumberOfNights() {
        assertThatCode(() -> validator.validate(TODAY.plusDays(1), TODAY.plusDays(1 + RULES.maxNights())))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMoreThanTheMaximumNumberOfNights() {
        assertThatThrownBy(() -> validator.validate(TODAY.plusDays(1), TODAY.plusDays(2 + RULES.maxNights())))
                .isInstanceOf(InvalidStayException.class)
                .hasMessageContaining("30 nights");
    }

    @Test
    void acceptsCheckOutOnTheLastDayOfTheBookingWindow() {
        assertThatCode(() -> validator.validate(TODAY.plusDays(340), TODAY.plusDays(RULES.windowDays())))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsCheckOutBeyondTheBookingWindow() {
        assertThatThrownBy(() -> validator.validate(TODAY.plusDays(340), TODAY.plusDays(RULES.windowDays() + 1)))
                .isInstanceOf(InvalidStayException.class)
                .hasMessageContaining("365 days");
    }

    @Test
    void todayFollowsTheConfiguredTimezone() {
        // 20:00 UTC on 10 June is already 01:30 on 11 June in Asia/Kolkata
        Clock lateEveningUtc = Clock.fixed(Instant.parse("2025-06-10T20:00:00Z"), ZONE);
        StayValidator kolkataValidator = new StayValidator(RULES, lateEveningUtc);

        assertThatThrownBy(() -> kolkataValidator.validate(TODAY, TODAY.plusDays(1)))
                .isInstanceOf(InvalidStayException.class);
        assertThatCode(() -> kolkataValidator.validate(TODAY.plusDays(1), TODAY.plusDays(2)))
                .doesNotThrowAnyException();
    }

    @Test
    void windowCheckIsReusableWithoutExceptions() {
        assertThat(validator.isInsideBookingWindow(TODAY, TODAY.plusDays(2))).isTrue();
        assertThat(validator.isInsideBookingWindow(TODAY.minusDays(1), TODAY.plusDays(2))).isFalse();
        assertThat(validator.isInsideBookingWindow(TODAY.plusDays(364), TODAY.plusDays(366))).isFalse();
    }
}
