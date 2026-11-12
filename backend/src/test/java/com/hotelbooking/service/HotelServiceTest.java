package com.hotelbooking.service;

import static com.hotelbooking.service.TestFixtures.CLOCK;
import static com.hotelbooking.service.TestFixtures.HOTEL_ID;
import static com.hotelbooking.service.TestFixtures.RULES;
import static com.hotelbooking.service.TestFixtures.TODAY;
import static com.hotelbooking.service.TestFixtures.hotel;
import static com.hotelbooking.service.TestFixtures.inventory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hotelbooking.dto.HotelResponse;
import com.hotelbooking.exception.HotelNotFoundException;
import com.hotelbooking.exception.InvalidStayException;
import com.hotelbooking.mapper.HotelMapper;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.RoomInventoryRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HotelServiceTest {

    private static final LocalDate CHECK_IN = TODAY.plusDays(10);
    private static final LocalDate CHECK_OUT = CHECK_IN.plusDays(3);

    @Mock
    private HotelRepository hotelRepository;
    @Mock
    private RoomInventoryRepository inventoryRepository;

    private HotelService service;

    @BeforeEach
    void setUp() {
        service = new HotelService(hotelRepository, inventoryRepository, new StayValidator(RULES, CLOCK),
                new AvailabilityCalculator(), new PriceCalculator(), new HotelMapper());
    }

    @Test
    void listWithoutDatesHasNoQuoteAndReadsNoInventory() {
        when(hotelRepository.findAllByOrderByNameAsc()).thenReturn(List.of(hotel(3)));

        List<HotelResponse> hotels = service.searchHotels(null, null, null);

        assertThat(hotels).hasSize(1);
        assertThat(hotels.get(0).quote()).isNull();
        assertThat(hotels.get(0).amenities()).containsExactly("WiFi", "Pool");
        verifyNoInteractions(inventoryRepository);
    }

    @Test
    void blankLocationMeansAllHotels() {
        when(hotelRepository.findAllByOrderByNameAsc()).thenReturn(List.of(hotel(3)));

        assertThat(service.searchHotels("   ", null, null)).hasSize(1);
    }

    @Test
    void locationIsTrimmedBeforeSearching() {
        when(hotelRepository.findByLocationContainingIgnoreCaseOrderByNameAsc("Goa"))
                .thenReturn(List.of(hotel(3)));

        assertThat(service.searchHotels("  Goa ", null, null)).hasSize(1);

        verify(hotelRepository).findByLocationContainingIgnoreCaseOrderByNameAsc("Goa");
    }

    @Test
    void listWithDatesShowsRoomsAvailableAndTotal() {
        when(hotelRepository.findAllByOrderByNameAsc()).thenReturn(List.of(hotel(3)));
        when(inventoryRepository.findAllHotelsStay(CHECK_IN, CHECK_OUT))
                .thenReturn(inventory(CHECK_IN, CHECK_OUT, 1));

        HotelResponse response = service.searchHotels(null, CHECK_IN, CHECK_OUT).get(0);

        assertThat(response.quote().roomsAvailable()).isEqualTo(2);
        assertThat(response.quote().nights()).isEqualTo(3);
        assertThat(response.quote().totalPrice()).isEqualByComparingTo("9000.00");
    }

    @Test
    void soldOutHotelIsStillListedWithZeroRooms() {
        when(hotelRepository.findAllByOrderByNameAsc()).thenReturn(List.of(hotel(3)));
        when(inventoryRepository.findAllHotelsStay(CHECK_IN, CHECK_OUT))
                .thenReturn(inventory(CHECK_IN, CHECK_OUT, 3));

        assertThat(service.searchHotels(null, CHECK_IN, CHECK_OUT).get(0).quote().roomsAvailable()).isZero();
    }

    @Test
    void givingOnlyOneDateIsRejected() {
        assertThatThrownBy(() -> service.searchHotels(null, CHECK_IN, null))
                .isInstanceOf(InvalidStayException.class)
                .hasMessageContaining("both");
    }

    @Test
    void detailsWithDatesIncludeTheQuote() {
        when(hotelRepository.findById(HOTEL_ID)).thenReturn(Optional.of(hotel(3)));
        when(inventoryRepository.findStay(HOTEL_ID, CHECK_IN, CHECK_OUT))
                .thenReturn(inventory(CHECK_IN, CHECK_OUT, 2));

        HotelResponse response = service.getHotel(HOTEL_ID, CHECK_IN, CHECK_OUT);

        assertThat(response.quote().roomsAvailable()).isEqualTo(1);
    }

    @Test
    void unknownHotelIsNotFound() {
        when(hotelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getHotel(99L, null, null)).isInstanceOf(HotelNotFoundException.class);
    }
}
