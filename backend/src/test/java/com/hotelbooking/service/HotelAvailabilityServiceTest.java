package com.hotelbooking.service;

import static com.hotelbooking.service.TestFixtures.CHECK_IN;
import static com.hotelbooking.service.TestFixtures.HOTEL_ID;
import static com.hotelbooking.service.TestFixtures.RULES;
import static com.hotelbooking.service.TestFixtures.CLOCK;
import static com.hotelbooking.service.TestFixtures.hotel;
import static com.hotelbooking.service.TestFixtures.typeInventory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.hotelbooking.dto.AvailabilityDay;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.RoomTypeInventoryRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HotelAvailabilityServiceTest {

    @Mock
    private HotelRepository hotelRepository;
    @Mock
    private RoomTypeInventoryRepository inventoryRepository;

    private HotelAvailabilityService service;

    @BeforeEach
    void setUp() {
        service = new HotelAvailabilityService(hotelRepository, inventoryRepository);
    }

    @Test
    void reportsSoldOutAndLimitedDaysForTheSelectedRoomType() {
        when(hotelRepository.findById(HOTEL_ID)).thenReturn(Optional.of(hotel(2)));
        when(inventoryRepository.findStay(HOTEL_ID, "Standard Room", CHECK_IN, CHECK_IN.plusDays(2)))
                .thenReturn(typeInventory(CHECK_IN, CHECK_IN.plusDays(2), night -> night.equals(CHECK_IN) ? 2 : 1));

        List<AvailabilityDay> days = service.getAvailability(
                HOTEL_ID, CHECK_IN, CHECK_IN.plusDays(2), "Standard Room", 1);

        assertThat(days).extracting(AvailabilityDay::level).containsExactly("sold-out", "limited");
        assertThat(days).extracting(AvailabilityDay::roomsAvailable).containsExactly(0, 1);
    }
}
