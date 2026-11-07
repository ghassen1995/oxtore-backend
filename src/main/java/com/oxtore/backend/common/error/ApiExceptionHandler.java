package com.oxtore.backend.common.error;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runs before the package advices, so every package answers {@link ApiException} and bean-validation failures the
 * same way: {@code {code, message, field?, status}}.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> api(ApiException ex) {
        return body(ex.getStatus(), ex.getCode(), ex.getMessage(), ex.getField());
    }

    /** @Valid request bodies: reports the first failing field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        FieldError first = ex.getBindingResult().getFieldError();
        String message = first != null && first.getDefaultMessage() != null ? first.getDefaultMessage() : "Invalid request";
        return body(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, first != null ? first.getField() : null);
    }

    public static ResponseEntity<Map<String, Object>> body(HttpStatus status, String code, String message, String field) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", String.valueOf(message));
        if (field != null) body.put("field", field);
        body.put("status", status.value());
        return ResponseEntity.status(status).body(body);
    }
}
