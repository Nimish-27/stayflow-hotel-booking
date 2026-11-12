package com.hotelbooking.exception;

/** Raised when a confirmed booking is cancelled after its free-cancellation deadline. */
public class CancellationNotAllowedException extends RuntimeException {

    public CancellationNotAllowedException(String message) {
        super(message);
    }
}
