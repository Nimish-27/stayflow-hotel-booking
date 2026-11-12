package com.hotelbooking.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/** Composite primary key (hotel_id, stay_date) of room_inventory. */
@Embeddable
public class RoomInventoryId implements Serializable {

    private Long hotelId;
    private LocalDate stayDate;

    protected RoomInventoryId() {
        // required by JPA
    }

    public RoomInventoryId(Long hotelId, LocalDate stayDate) {
        this.hotelId = hotelId;
        this.stayDate = stayDate;
    }

    public Long getHotelId() { return hotelId; }
    public LocalDate getStayDate() { return stayDate; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RoomInventoryId that)) return false;
        return Objects.equals(hotelId, that.hotelId) && Objects.equals(stayDate, that.stayDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hotelId, stayDate);
    }
}