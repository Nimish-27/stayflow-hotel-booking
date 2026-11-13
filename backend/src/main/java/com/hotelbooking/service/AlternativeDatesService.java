package com.hotelbooking.service;

import com.hotelbooking.config.BookingRules;
import com.hotelbooking.dto.StayQuote;
import com.hotelbooking.entity.Hotel;
import com.hotelbooking.entity.RoomTypeInventory;
import com.hotelbooking.exception.HotelNotFoundException;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.RoomTypeInventoryRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Smart alternative dates": same number of nights, shifted up to +/- N days, nearest first.
 * Uses the same StayValidator / AvailabilityCalculator / PriceCalculator as real bookings.
 */
@Service
public class AlternativeDatesService {

    private final HotelRepository hotelRepository;
    private final RoomTypeInventoryRepository inventoryRepository;
    private final StayValidator stayValidator;
    private final AvailabilityCalculator availabilityCalculator;
    private final PriceCalculator priceCalculator;
    private final BookingRules rules;

    public AlternativeDatesService(HotelRepository hotelRepository, RoomTypeInventoryRepository inventoryRepository,
                                   StayValidator stayValidator, AvailabilityCalculator availabilityCalculator,
                                   PriceCalculator priceCalculator, BookingRules rules, Clock clock) {
        this.hotelRepository = hotelRepository;
        this.inventoryRepository = inventoryRepository;
        this.stayValidator = stayValidator;
        this.availabilityCalculator = availabilityCalculator;
        this.priceCalculator = priceCalculator;
        this.rules = rules;
    }

    @Transactional(readOnly = true)
    public List<StayQuote> findAlternatives(Long hotelId, LocalDate checkIn, LocalDate checkOut) {
        stayValidator.validate(checkIn, checkOut);
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));

        int nights = priceCalculator.nights(checkIn, checkOut);
        int shift = rules.alternativeShiftDays();
        // ONE query covers every candidate stay; each candidate is then checked in memory.
        String roomType = hotel.resolveRoomType(null);
        Map<LocalDate, Integer> booked = inventoryRepository.findStay(
                hotelId, roomType, checkIn.minusDays(shift), checkOut.plusDays(shift)).stream()
                .collect(java.util.stream.Collectors.toMap(RoomTypeInventory::getStayDate,
                        RoomTypeInventory::getBookedRooms));

        return candidateOffsets().stream()
                .map(offset -> checkIn.plusDays(offset))
                .map(start -> quoteIfBookable(hotel, booked, start, nights))
                .flatMap(Optional::stream)
                .limit(rules.maxAlternatives())
                .toList();
    }

    /** -1, +1, -2, +2 ... nearest first; on a tie the earlier date comes first. */
    private List<Integer> candidateOffsets() {
        List<Integer> offsets = new ArrayList<>();
        for (int distance = 1; distance <= rules.alternativeShiftDays(); distance++) {
            offsets.add(-distance);
            offsets.add(distance);
        }
        return offsets;
    }

    private Optional<StayQuote> quoteIfBookable(Hotel hotel, Map<LocalDate, Integer> booked,
                                                LocalDate start, int nights) {
        LocalDate end = start.plusDays(nights);
        if (!stayValidator.isInsideBookingWindow(start, end)) {
            return Optional.empty();
        }
        int roomsAvailable = availabilityCalculator.roomsAvailable(
                hotel.roomsForRoomType(hotel.resolveRoomType(null)), booked, start, end);
        if (roomsAvailable == 0) {
            return Optional.empty();
        }
        String roomType = hotel.resolveRoomType(null);
        return Optional.of(priceCalculator.quote(hotel.priceForRoomType(roomType), start, end, roomsAvailable));
    }
}
