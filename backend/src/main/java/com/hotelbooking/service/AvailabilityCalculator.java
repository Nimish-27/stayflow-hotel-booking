package com.hotelbooking.service;

import com.hotelbooking.entity.RoomInventory;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * THE availability rule, used by search, booking and alternative dates:
 * rooms available for a stay = total rooms - booked rooms on the busiest night of the stay.
 * The stay covers [checkIn, checkOut): the check-out day is not looked at.
 */
@Component
public class AvailabilityCalculator {

    public Map<LocalDate, Integer> bookedByNight(List<RoomInventory> rows) {
        return rows.stream()
                .collect(Collectors.toMap(RoomInventory::getStayDate, RoomInventory::getBookedRooms));
    }

    public int roomsAvailable(int totalRooms, Map<LocalDate, Integer> bookedByNight,
                              LocalDate checkIn, LocalDate checkOut) {
        int busiestNight = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            Integer booked = bookedByNight.get(night);
            if (booked == null) {
                return 0; // no inventory row for this night, so nothing can be sold
            }
            busiestNight = Math.max(busiestNight, booked);
        }
        return Math.max(0, totalRooms - busiestNight);
    }

    public boolean isAvailable(int totalRooms, Map<LocalDate, Integer> bookedByNight,
                               LocalDate checkIn, LocalDate checkOut) {
        return roomsAvailable(totalRooms, bookedByNight, checkIn, checkOut) > 0;
    }
}
