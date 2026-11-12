package com.hotelbooking.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** How many rooms of one hotel are booked on one night. */
@Entity
@Table(name = "room_inventory")
public class RoomInventory {

    @EmbeddedId
    private RoomInventoryId id;

    private int bookedRooms;

    protected RoomInventory() {
        // required by JPA
    }

    public RoomInventory(Long hotelId, LocalDate stayDate, int bookedRooms) {
        this.id = new RoomInventoryId(hotelId, stayDate);
        this.bookedRooms = bookedRooms;
    }

    public Long getHotelId() { return id.getHotelId(); }
    public LocalDate getStayDate() { return id.getStayDate(); }
    public int getBookedRooms() { return bookedRooms; }

    /** The caller must already have checked bookedRooms < totalRooms while holding the row lock. */
    public void increment() {
        bookedRooms++;
    }

    public void decrement() {
        if (bookedRooms == 0) {
            throw new IllegalStateException("No booked room to release on " + id.getStayDate());
        }
        bookedRooms--;
    }
}