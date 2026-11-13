package com.hotelbooking.exception;

/** Base for "404 Not Found" cases. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}