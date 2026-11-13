package com.hotelbooking.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "room_type_inventory")
public class RoomTypeInventory {
    @EmbeddedId
    private RoomTypeInventoryId id;
    private int bookedRooms;

    protected RoomTypeInventory() { }

    public RoomTypeInventory(Long hotelId, String roomType, java.time.LocalDate stayDate, int bookedRooms) {
        this.id = new RoomTypeInventoryId(hotelId, roomType, stayDate);
        this.bookedRooms = bookedRooms;
    }

    public Long getHotelId() { return id.getHotelId(); }
    public String getRoomType() { return id.getRoomType(); }
    public java.time.LocalDate getStayDate() { return id.getStayDate(); }
    public int getBookedRooms() { return bookedRooms; }
    public void increment(int rooms) { bookedRooms += rooms; }
    public void decrement(int rooms) {
        if (bookedRooms < rooms) throw new IllegalStateException("Not enough booked rooms to release");
        bookedRooms -= rooms;
    }
}
