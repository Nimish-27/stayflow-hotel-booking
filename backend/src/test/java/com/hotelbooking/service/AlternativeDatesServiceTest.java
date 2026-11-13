package com.hotelbooking.service;

import static com.hotelbooking.service.TestFixtures.CLOCK;
import static com.hotelbooking.service.TestFixtures.HOTEL_ID;
import static com.hotelbooking.service.TestFixtures.RULES;
import static com.hotelbooking.service.TestFixtures.TODAY;
import static com.hotelbooking.service.TestFixtures.hotel;
import static com.hotelbooking.service.TestFixtures.typeInventory;
import static com.hotelbooking.service.TestFixtures.isNightOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hotelbooking.dto.StayQuote;
import com.hotelbooking.exception.HotelNotFoundException;
import com.hotelbooking.exception.InvalidStayException;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.RoomTypeInventoryRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlternativeDatesServiceTest {

    private static final int NIGHTS = 3;
    private static final int TOTAL_ROOMS = 2;
    private static final int SHIFT = RULES.alternativeShiftDays();

    @Mock
    private HotelRepository hotelRepository;
    @Mock
    private RoomTypeInventoryRepository inventoryRepository;

    private AlternativeDatesService service;

    @BeforeEach
    void setUp() {
        service = new AlternativeDatesService(hotelRepository, inventoryRepository,
                new StayValidator(RULES, CLOCK), new AvailabilityCalculator(), new PriceCalculator(RULES),
                RULES, CLOCK);
    }

    @Test
    void suggestsTheNearestFreeWindowsAndPrefersTheEarlierDateOnATie() {
        LocalDate checkIn = TODAY.plusDays(20);
        LocalDate checkOut = checkIn.plusDays(NIGHTS);
        // The requested nights are full. Shifts of +/-1 and +/-2 days still overlap them,
        // so the first free windows are 3 days earlier, 3 days later, then 4 days earlier.
        givenInventory(checkIn, NIGHTS, night -> isNightOf(night, checkIn, checkOut) ? TOTAL_ROOMS : 0);

        List<StayQuote> alternatives = service.findAlternatives(HOTEL_ID, checkIn, checkOut);

        assertThat(alternatives).extracting(StayQuote::checkIn)
                .containsExactly(checkIn.minusDays(3), checkIn.plusDays(3), checkIn.minusDays(4));
        assertThat(alternatives).allSatisfy(quote -> {
            assertThat(quote.nights()).isEqualTo(NIGHTS);
            assertThat(quote.totalPrice()).isEqualByComparingTo("10620.00");
            assertThat(quote.roomsAvailable()).isEqualTo(TOTAL_ROOMS);
        });
    }

    @Test
    void neverSuggestsAWindowContainingAFullNight() {
        LocalDate checkIn = TODAY.plusDays(23);
        LocalDate checkOut = checkIn.plusDays(NIGHTS);
        LocalDate fullNight = TODAY.plusDays(25); // the stay is only partially unavailable
        givenInventory(checkIn, NIGHTS, night -> night.equals(fullNight) ? TOTAL_ROOMS : 0);

        List<StayQuote> alternatives = service.findAlternatives(HOTEL_ID, checkIn, checkOut);

        assertThat(alternatives).extracting(StayQuote::checkIn)
                .containsExactly(checkIn.minusDays(1), checkIn.minusDays(2), checkIn.minusDays(3));
        assertThat(alternatives)
                .noneMatch(quote -> isNightOf(fullNight, quote.checkIn(), quote.checkOut()));
    }

    @Test
    void returnsAtMostTheConfiguredNumberOfAlternativesUsingOneInventoryQuery() {
        LocalDate checkIn = TODAY.plusDays(20);
        LocalDate checkOut = checkIn.plusDays(NIGHTS);
        givenInventory(checkIn, NIGHTS, night -> 0);

        List<StayQuote> alternatives = service.findAlternatives(HOTEL_ID, checkIn, checkOut);

        assertThat(alternatives).extracting(StayQuote::checkIn)
                .containsExactly(checkIn.minusDays(1), checkIn.plusDays(1), checkIn.minusDays(2));
        verify(inventoryRepository, times(1))
                .findStay(HOTEL_ID, "Standard Room", checkIn.minusDays(SHIFT), checkOut.plusDays(SHIFT));
    }

    @Test
    void doesNotSuggestStaysThatStartInThePast() {
        LocalDate checkIn = TODAY;
        LocalDate checkOut = checkIn.plusDays(2);
        givenInventory(checkIn, 2, night -> 0);

        List<StayQuote> alternatives = service.findAlternatives(HOTEL_ID, checkIn, checkOut);

        assertThat(alternatives).extracting(StayQuote::checkIn)
                .containsExactly(TODAY.plusDays(1), TODAY.plusDays(2), TODAY.plusDays(3));
        assertThat(alternatives).allSatisfy(quote -> assertThat(quote.nights()).isEqualTo(2));
    }

    @Test
    void doesNotSuggestStaysThatEndBeyondTheBookingWindow() {
        LocalDate checkIn = TODAY.plusDays(360);
        LocalDate checkOut = checkIn.plusDays(NIGHTS);
        // Earlier nights are full, so only later windows can qualify: +1 and +2 end on or before
        // today+365, but +3 would end on today+366.
        givenInventory(checkIn, NIGHTS, night -> night.isBefore(checkIn) ? TOTAL_ROOMS : 0);

        List<StayQuote> alternatives = service.findAlternatives(HOTEL_ID, checkIn, checkOut);

        assertThat(alternatives).extracting(StayQuote::checkIn)
                .containsExactly(checkIn.plusDays(1), checkIn.plusDays(2));
    }

    @Test
    void returnsNothingWhenEveryNearbyWindowIsFull() {
        LocalDate checkIn = TODAY.plusDays(20);
        LocalDate checkOut = checkIn.plusDays(NIGHTS);
        givenInventory(checkIn, NIGHTS, night -> TOTAL_ROOMS);

        assertThat(service.findAlternatives(HOTEL_ID, checkIn, checkOut)).isEmpty();
    }

    @Test
    void unknownHotelIsNotFound() {
        when(hotelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findAlternatives(99L, TODAY.plusDays(5), TODAY.plusDays(8)))
                .isInstanceOf(HotelNotFoundException.class);
    }

    @Test
    void invalidDatesAreRejectedBeforeAnyQuery() {
        assertThatThrownBy(() -> service.findAlternatives(HOTEL_ID, TODAY.plusDays(5), TODAY.plusDays(5)))
                .isInstanceOf(InvalidStayException.class);

        verifyNoInteractions(hotelRepository, inventoryRepository);
    }

    private void givenInventory(LocalDate checkIn, int nights, Function<LocalDate, Integer> bookedOn) {
        LocalDate from = checkIn.minusDays(SHIFT);
        LocalDate to = checkIn.plusDays(nights).plusDays(SHIFT);
        when(hotelRepository.findById(HOTEL_ID)).thenReturn(Optional.of(hotel(TOTAL_ROOMS)));
        when(inventoryRepository.findStay(HOTEL_ID, "Standard Room", from, to))
                .thenReturn(typeInventory(from, to, bookedOn));
    }
}
