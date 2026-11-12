package com.hotelbooking.service;

import com.hotelbooking.dto.HotelResponse;
import com.hotelbooking.dto.StayQuote;
import com.hotelbooking.entity.Hotel;
import com.hotelbooking.entity.RoomInventory;
import com.hotelbooking.exception.HotelNotFoundException;
import com.hotelbooking.exception.InvalidStayException;
import com.hotelbooking.mapper.HotelMapper;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.RoomInventoryRepository;
import com.hotelbooking.util.InputNormalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only use cases: browse, search and hotel details. Nothing here changes data. */
@Service
public class HotelService {

    private final HotelRepository hotelRepository;
    private final RoomInventoryRepository inventoryRepository;
    private final StayValidator stayValidator;
    private final AvailabilityCalculator availabilityCalculator;
    private final PriceCalculator priceCalculator;
    private final HotelMapper hotelMapper;

    public HotelService(HotelRepository hotelRepository, RoomInventoryRepository inventoryRepository,
                        StayValidator stayValidator, AvailabilityCalculator availabilityCalculator,
                        PriceCalculator priceCalculator, HotelMapper hotelMapper) {
        this.hotelRepository = hotelRepository;
        this.inventoryRepository = inventoryRepository;
        this.stayValidator = stayValidator;
        this.availabilityCalculator = availabilityCalculator;
        this.priceCalculator = priceCalculator;
        this.hotelMapper = hotelMapper;
    }

    /** Dates are optional, but both or neither. Sold-out hotels stay in the list with 0 rooms. */
    @Transactional(readOnly = true)
    public List<HotelResponse> searchHotels(String location, LocalDate checkIn, LocalDate checkOut) {
        boolean withDates = hasDates(checkIn, checkOut);
        if (withDates) {
            stayValidator.validate(checkIn, checkOut);
        }
        List<Hotel> hotels = findHotels(location);
        if (!withDates) {
            return hotels.stream().map(hotel -> hotelMapper.toResponse(hotel, null)).toList();
        }
        Map<Long, Map<LocalDate, Integer>> bookedByHotel = bookedNightsByHotel(checkIn, checkOut);
        return hotels.stream()
                .map(hotel -> hotelMapper.toResponse(hotel,
                        quote(hotel, bookedByHotel.getOrDefault(hotel.getId(), Map.of()), checkIn, checkOut)))
                .toList();
    }

    @Transactional(readOnly = true)
    public HotelResponse getHotel(Long hotelId, LocalDate checkIn, LocalDate checkOut) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));
        if (!hasDates(checkIn, checkOut)) {
            return hotelMapper.toResponse(hotel, null);
        }
        stayValidator.validate(checkIn, checkOut);
        Map<LocalDate, Integer> booked =
                availabilityCalculator.bookedByNight(inventoryRepository.findStay(hotelId, checkIn, checkOut));
        return hotelMapper.toResponse(hotel, quote(hotel, booked, checkIn, checkOut));
    }

    private boolean hasDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null && checkOut == null) {
            return false;
        }
        if (checkIn == null || checkOut == null) {
            throw new InvalidStayException("Provide both checkIn and checkOut, or neither");
        }
        return true;
    }

    private List<Hotel> findHotels(String location) {
        String term = InputNormalizer.trim(location);
        if (term == null || term.isEmpty()) {
            return hotelRepository.findAllByOrderByNameAsc();
        }
        return hotelRepository.findByLocationContainingIgnoreCaseOrderByNameAsc(term);
    }

    private Map<Long, Map<LocalDate, Integer>> bookedNightsByHotel(LocalDate checkIn, LocalDate checkOut) {
        Map<Long, List<RoomInventory>> rowsByHotel =
                inventoryRepository.findAllHotelsStay(checkIn, checkOut).stream()
                        .collect(Collectors.groupingBy(RoomInventory::getHotelId));
        return rowsByHotel.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> availabilityCalculator.bookedByNight(entry.getValue())));
    }

    private StayQuote quote(Hotel hotel, Map<LocalDate, Integer> booked, LocalDate checkIn, LocalDate checkOut) {
        int roomsAvailable = availabilityCalculator.roomsAvailable(hotel.getTotalRooms(), booked, checkIn, checkOut);
        return priceCalculator.quote(hotel.getPricePerNight(), checkIn, checkOut, roomsAvailable);
    }
}
