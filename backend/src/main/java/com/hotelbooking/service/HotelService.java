package com.hotelbooking.service;

import com.hotelbooking.dto.HotelResponse;
import com.hotelbooking.dto.StayQuote;
import com.hotelbooking.entity.Hotel;
import com.hotelbooking.entity.RoomTypeInventory;
import com.hotelbooking.exception.HotelNotFoundException;
import com.hotelbooking.exception.InvalidStayException;
import com.hotelbooking.mapper.HotelMapper;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.RoomTypeInventoryRepository;
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
    private final RoomTypeInventoryRepository roomTypeInventoryRepository;
    private final StayValidator stayValidator;
    private final AvailabilityCalculator availabilityCalculator;
    private final PriceCalculator priceCalculator;
    private final HotelMapper hotelMapper;

    public HotelService(HotelRepository hotelRepository,
                        RoomTypeInventoryRepository roomTypeInventoryRepository, StayValidator stayValidator,
                        AvailabilityCalculator availabilityCalculator, PriceCalculator priceCalculator,
                        HotelMapper hotelMapper) {
        this.hotelRepository = hotelRepository;
        this.roomTypeInventoryRepository = roomTypeInventoryRepository;
        this.stayValidator = stayValidator;
        this.availabilityCalculator = availabilityCalculator;
        this.priceCalculator = priceCalculator;
        this.hotelMapper = hotelMapper;
    }

    /** Dates are optional, but both or neither. Sold-out hotels stay in the list with 0 rooms. */
    public List<HotelResponse> searchHotels(String location, LocalDate checkIn, LocalDate checkOut) {
        return searchHotels(location, checkIn, checkOut, null);
    }

    @Transactional(readOnly = true)
    public List<HotelResponse> searchHotels(String location, LocalDate checkIn, LocalDate checkOut, String roomType) {
        boolean withDates = hasDates(checkIn, checkOut);
        if (withDates) {
            stayValidator.validate(checkIn, checkOut);
        }
        List<Hotel> hotels = findHotels(location);
        if (!withDates) {
            return hotels.stream().map(hotel -> hotelMapper.toResponse(hotel, null)).toList();
        }
        Map<Long, Map<String, Map<LocalDate, Integer>>> bookedByHotel = bookedNightsByHotel(checkIn, checkOut);
        return hotels.stream()
                .map(hotel -> {
                    String resolved = hotel.resolveRoomType(roomType);
                    return hotelMapper.toResponse(hotel,
                            quote(hotel, bookedByHotel.getOrDefault(hotel.getId(), Map.of())
                                    .getOrDefault(resolved, Map.of()),
                                    checkIn, checkOut, resolved));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public HotelResponse getHotel(Long hotelId, LocalDate checkIn, LocalDate checkOut) {
        return getHotel(hotelId, checkIn, checkOut, null);
    }

    @Transactional(readOnly = true)
    public HotelResponse getHotel(Long hotelId, LocalDate checkIn, LocalDate checkOut, String roomType) {
        return getHotel(hotelId, checkIn, checkOut, roomType, 1);
    }

    @Transactional(readOnly = true)
    public HotelResponse getHotel(Long hotelId, LocalDate checkIn, LocalDate checkOut,
                                  String roomType, int rooms) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));
        String resolvedRoomType = hotel.resolveRoomType(roomType);
        if (!hasDates(checkIn, checkOut)) {
            return hotelMapper.toResponse(hotel, null, hotel.priceForRoomType(resolvedRoomType));
        }
        stayValidator.validate(checkIn, checkOut);
        Map<LocalDate, Integer> booked = roomTypeInventoryRepository.findStay(hotelId, resolvedRoomType, checkIn, checkOut).stream()
                .collect(Collectors.toMap(RoomTypeInventory::getStayDate, RoomTypeInventory::getBookedRooms));
        if (rooms < 1) {
            throw new InvalidStayException("rooms must be at least 1");
        }
        return hotelMapper.toResponse(hotel, quote(hotel, booked, checkIn, checkOut, resolvedRoomType, rooms),
                hotel.priceForRoomType(resolvedRoomType));
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

    private Map<Long, Map<String, Map<LocalDate, Integer>>> bookedNightsByHotel(
            LocalDate checkIn, LocalDate checkOut) {
        Map<Long, List<RoomTypeInventory>> rowsByHotel =
                roomTypeInventoryRepository.findAllStay(checkIn, checkOut).stream()
                        .collect(Collectors.groupingBy(RoomTypeInventory::getHotelId));
        return rowsByHotel.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .collect(Collectors.groupingBy(RoomTypeInventory::getRoomType,
                                        Collectors.toMap(RoomTypeInventory::getStayDate,
                                                RoomTypeInventory::getBookedRooms)))));
    }

    private StayQuote quote(Hotel hotel, Map<LocalDate, Integer> booked, LocalDate checkIn,
                            LocalDate checkOut, String roomType) {
        return quote(hotel, booked, checkIn, checkOut, roomType, 1);
    }

    private StayQuote quote(Hotel hotel, Map<LocalDate, Integer> booked, LocalDate checkIn,
                            LocalDate checkOut, String roomType, int rooms) {
        int roomsAvailable = availabilityCalculator.roomsAvailable(hotel.roomsForRoomType(roomType),
                booked, checkIn, checkOut);
        return priceCalculator.quote(hotel.priceForRoomType(roomType), checkIn, checkOut,
                roomsAvailable, rooms);
    }
}
