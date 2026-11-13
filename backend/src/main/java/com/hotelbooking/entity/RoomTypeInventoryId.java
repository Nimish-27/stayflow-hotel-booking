package com.hotelbooking.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class RoomTypeInventoryId implements Serializable {
    private Long hotelId;
    private String roomType;
    private LocalDate stayDate;

    protected RoomTypeInventoryId() { }

    public RoomTypeInventoryId(Long hotelId, String roomType, LocalDate stayDate) {
        this.hotelId = hotelId;
        this.roomType = roomType;
        this.stayDate = stayDate;
    }

    public Long getHotelId() { return hotelId; }
    public String getRoomType() { return roomType; }
    public LocalDate getStayDate() { return stayDate; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RoomTypeInventoryId that)) return false;
        return Objects.equals(hotelId, that.hotelId)
                && Objects.equals(roomType, that.roomType)
                && Objects.equals(stayDate, that.stayDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hotelId, roomType, stayDate);
    }
}
