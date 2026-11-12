package com.hotelbooking.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** Creates references like HB-K7M2XQ9P: random (not sequential), easy to read aloud. */
@Component
public class BookingReferenceGenerator {

    private static final String PREFIX = "HB-";
    /** No 0/O and 1/I, which are easy to confuse. 32 symbols, 32^8 is about 1.1 trillion combinations. */
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder reference = new StringBuilder(PREFIX);
        for (int i = 0; i < CODE_LENGTH; i++) {
            reference.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return reference.toString();
    }
}
