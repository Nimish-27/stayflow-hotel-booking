package com.hotelbooking.exception;

/** Business-rule violation when no rooms remain for the requested stay. Maps to 409. */
public class SoldOutException extends RuntimeException {

    public SoldOutException(String message) {
        super(message);
    }
}