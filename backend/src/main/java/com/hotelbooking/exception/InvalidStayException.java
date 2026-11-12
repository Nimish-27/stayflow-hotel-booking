package com.hotelbooking.exception;

/** Business-rule violation in the requested dates (past, too long, outside the window...). Maps to 400. */
public class InvalidStayException extends RuntimeException {

    public InvalidStayException(String message) {
        super(message);
    }
}