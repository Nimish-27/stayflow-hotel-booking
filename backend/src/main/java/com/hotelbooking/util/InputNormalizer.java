package com.hotelbooking.util;

import java.util.Locale;

/** Cleans user input in one place. Null stays null, so validation can report it as "required". */
public final class InputNormalizer {

    private InputNormalizer() {
    }

    public static String trim(String value) {
        return value == null ? null : value.trim();
    }

    /** Emails are trimmed and lowercased, so lookups are case-insensitive. */
    public static String email(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    /** References are generated in upper case, so a lookup upper-cases what the user typed. */
    public static String reference(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
