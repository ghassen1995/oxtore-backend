package com.oxtore.backend.common.error;

import org.springframework.http.HttpStatus;

/**
 * An error the app can act on: answered as {@code {code, message, field?, status}} by {@link ApiExceptionHandler}.
 * {@code code} is stable (the app translates known codes), {@code message} is for display, {@code field} names the
 * offending request field ("customerName", "items[1]") when there is one.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String field;

    public ApiException(HttpStatus status, String code, String message, String field) {
        super(message);
        this.status = status;
        this.code = code;
        this.field = field;
    }

    /** 400 VALIDATION_ERROR on one field. */
    public static ApiException validation(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, field);
    }

    public static ApiException forbidden(String code, String message) {
        return new ApiException(HttpStatus.FORBIDDEN, code, message, null);
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message, null);
    }

    public static ApiException unprocessable(String code, String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, code, message, null);
    }

    public static ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message, null);
    }

    /** Same error, attached to a request field. */
    public ApiException at(String field) {
        return new ApiException(status, code, getMessage(), field);
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public String getField() { return field; }
}
