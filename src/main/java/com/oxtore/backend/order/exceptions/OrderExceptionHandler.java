package com.oxtore.backend.order.exceptions;

import com.oxtore.backend.stock.exceptions.InsufficientStockException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** {code, message, status} errors; the app shows {@code message}. */
@RestControllerAdvice(basePackages = "com.oxtore.backend.order")
@Order(4)
public class OrderExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException ex) {
        return error("INVALID_INPUT", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /** A step that isn't allowed from the order's current status, or stock that ran out before confirmation. */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> conflict(IllegalStateException ex) {
        return error("ORDER_CONFLICT", ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, Object>> insufficientStock(InsufficientStockException ex) {
        return error("ORDER_CONFLICT", "Not enough stock to confirm this order", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(EntityNotFoundException ex) {
        return error("NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> forbidden(AccessDeniedException ex) {
        return error("FORBIDDEN", ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    private static ResponseEntity<Map<String, Object>> error(String code, String message, HttpStatus status) {
        return ResponseEntity.status(status)
                .body(Map.of("code", code, "message", String.valueOf(message), "status", status.value()));
    }
}
