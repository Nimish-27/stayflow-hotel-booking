package com.hotelbooking.exception;

/**
 * The message is fixed on purpose. "No such reference" and "wrong email" must look identical,
 * so an attacker cannot learn which references exist.
 */
public class BookingNotFoundException extends NotFoundException {

    public BookingNotFoundException() {
        super("Booking not found for the given reference and email");
    }
}