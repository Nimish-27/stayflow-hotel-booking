package com.hotelbooking.service;

import com.hotelbooking.dto.AvailabilityDay;
import com.hotelbooking.entity.Hotel;
import com.hotelbooking.exception.HotelNotFoundException;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.RoomTypeInventoryRepository;
import com.hotelbooking.exception.InvalidStayException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HotelAvailabilityService {

    private final HotelRepository hotelRepository;
    private final RoomTypeInventoryRepository roomTypeInventoryRepository;

    public HotelAvailabilityService(HotelRepository hotelRepository,
                                    RoomTypeInventoryRepository roomTypeInventoryRepository) {
        this.hotelRepository = hotelRepository;
        this.roomTypeInventoryRepository = roomTypeInventoryRepository;
    }

    public List<AvailabilityDay> getAvailability(Long hotelId, LocalDate from, LocalDate to) {
        return getAvailability(hotelId, from, to, null, 1);
    }

    public List<AvailabilityDay> getAvailability(Long hotelId, LocalDate from, LocalDate to,
                                                  String roomType, int rooms) {
        if (from == null || to == null || !to.isAfter(from) || rooms < 1) {
            throw new InvalidStayException("Availability requires a valid date range and at least one room");
        }
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));
        String resolved = hotel.resolveRoomType(roomType);
        int capacity = hotel.roomsForRoomType(resolved);
        return roomTypeInventoryRepository.findStay(hotelId, resolved, from, to).stream()
                .map(row -> toDay(capacity, row.getBookedRooms(), row.getStayDate(), rooms))
                .toList();
    }

    private AvailabilityDay toDay(int capacity, int bookedRooms, LocalDate date, int rooms) {
        int roomsAvailable = Math.max(0, capacity - bookedRooms);
        int limitedThreshold = Math.max(rooms, (int) Math.ceil(capacity * 0.25));
        String level = roomsAvailable == 0 ? "sold-out"
                : roomsAvailable <= limitedThreshold ? "limited" : "available";
        return new AvailabilityDay(date, roomsAvailable, level);
    }
}
