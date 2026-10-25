package com.oxtore.backend.common.security;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Fallback for packages whose own advice doesn't handle these exceptions (store, staff, feed), so they answer JSON
 * the app can show instead of an empty error page or a 500. Lowest precedence: package advices that do handle them
 * (product, stock, notification, identity) keep their own format.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class AccessDeniedAdvice {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> forbidden(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("code", "FORBIDDEN", "message", ex.getMessage(), "status", 403));
    }

    /** Invalid input detected in a controller or service, e.g. an unknown boutique category. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code", "INVALID_INPUT", "message", String.valueOf(ex.getMessage()), "status", 400));
    }
}
