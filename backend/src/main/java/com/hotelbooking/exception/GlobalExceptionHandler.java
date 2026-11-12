package com.hotelbooking.exception;

import com.hotelbooking.dto.ApiError;
import com.hotelbooking.dto.FieldErrorDetail;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every exception into the same ApiError JSON. ResponseEntityExceptionHandler already
 * catches Spring's own exceptions (bad JSON, missing parameter, wrong method, unknown URL);
 * we only change the body shape and add our business exceptions.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    // ---- our business exceptions ----

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFound(NotFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidStayException.class)
    public ResponseEntity<Object> handleInvalidStay(InvalidStayException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(SoldOutException.class)
    public ResponseEntity<Object> handleSoldOut(SoldOutException ex, WebRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /** Last resort: log the details on the server, tell the client nothing internal. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error on {}", pathOf(request), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.", request);
    }

    // ---- Spring's exceptions: overrides that give friendlier messages ----

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList();
        return respond(status, headers, "Validation failed", request, fieldErrors);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return respond(status, headers, "Request body is missing or malformed", request, List.of());
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        return respond(status, headers, "Missing required parameter: " + ex.getParameterName(),
                request, List.of());
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String name = ex instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName()
                : ex.getPropertyName();
        return respond(status, headers, "Invalid value for parameter '" + name + "'", request, List.of());
    }

    /** Every other Spring exception (404 unknown URL, 405, 415...) ends up here. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        return respond(statusCode, headers, ex.getMessage(), request, List.of());
    }

    // ---- helpers ----

    private ResponseEntity<Object> build(HttpStatus status, String message, WebRequest request) {
        return respond(status, HttpHeaders.EMPTY, message, request, List.of());
    }

    private ResponseEntity<Object> respond(HttpStatusCode statusCode, HttpHeaders headers, String message,
                                           WebRequest request, List<FieldErrorDetail> fieldErrors) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        ApiError body = new ApiError(Instant.now(clock), status.value(), status.getReasonPhrase(),
                message, pathOf(request), fieldErrors);
        return new ResponseEntity<>(body, headers, status);
    }

    private String pathOf(WebRequest request) {
        return ((ServletWebRequest) request).getRequest().getRequestURI();
    }
}