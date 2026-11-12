package com.hotelbooking.dto;

import java.time.Instant;
import java.util.List;

/** The one error shape returned by every failing endpoint. fieldErrors is empty unless validation failed. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorDetail> fieldErrors) {
}