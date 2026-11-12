package com.hotelbooking.service;

import com.hotelbooking.dto.AvailabilityDay;
import com.hotelbooking.entity.Hotel;
import com.hotelbooking.entity.RoomInventory;
import com.hotelbooking.exception.HotelNotFoundException;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.RoomInventoryRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HotelAvailabilityService {

    private final HotelRepository hotelRepository;
    private final RoomInventoryRepository inventoryRepository;

    public HotelAvailabilityService(HotelRepository hotelRepository,
                                    RoomInventoryRepository inventoryRepository) {
        this.hotelRepository = hotelRepository;
        this.inventoryRepository = inventoryRepository;
    }

    public List<AvailabilityDay> getAvailability(Long hotelId, LocalDate from, LocalDate to) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));
        return inventoryRepository.findStay(hotelId, from, to).stream()
                .map(row -> toDay(hotel, row))
                .toList();
    }

    private AvailabilityDay toDay(Hotel hotel, RoomInventory row) {
        int roomsAvailable = hotel.getTotalRooms() - row.getBookedRooms();
        int limitedThreshold = Math.max(1, (int) Math.ceil(hotel.getTotalRooms() * 0.25));
        String level = roomsAvailable == 0 ? "sold-out"
                : roomsAvailable <= limitedThreshold ? "limited" : "available";
        return new AvailabilityDay(row.getStayDate(), roomsAvailable, level);
    }
}
