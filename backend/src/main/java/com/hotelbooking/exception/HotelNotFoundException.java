package com.hotelbooking.exception;

public class HotelNotFoundException extends NotFoundException {

    public HotelNotFoundException(Long hotelId) {
        super("Hotel not found: " + hotelId);
    }
}