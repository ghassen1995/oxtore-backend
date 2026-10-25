package com.oxtore.backend.identity.exceptions;

import com.oxtore.backend.identity.DTO.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.oxtore.backend.identity")
@Order(2)
public class IdentityExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleUserAlreadyExist(UserAlreadyExistsException exception) {
        ApiError error = new ApiError("USER_ALREADY_EXIST", exception.getMessage(), 409);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(UserCreationException.class)
    public ResponseEntity<ApiError> handleUserCreation(UserCreationException exception) {
        ApiError error = new ApiError("USER_CREATION", exception.getMessage(), 500);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(OtpException.class)
    public ResponseEntity<ApiError> handleOtp(OtpException exception) {
        ApiError error = new ApiError(exception.getCode(), exception.getMessage(), exception.getStatus().value());
        ResponseEntity.BodyBuilder response = ResponseEntity.status(exception.getStatus());
        if (exception.getRetryAfterSeconds() != null) {
            response.header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.getRetryAfterSeconds()));
        }
        return response.body(error);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiError> handleAuth(AuthException exception) {
        ApiError error = new ApiError(exception.getCode(), exception.getMessage(), exception.getStatus().value());
        return ResponseEntity.status(exception.getStatus()).body(error);
    }

    /** Bean validation on request bodies (@Valid): reports the first failing field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        FieldError first = exception.getBindingResult().getFieldError();
        String message = first != null && first.getDefaultMessage() != null
                ? first.getDefaultMessage()
                : "Invalid request";
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", message, 400));
    }

    /** Malformed JSON or an unknown enum value (e.g. method other than "email"/"whatsapp"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_INPUT", "Malformed request body", 400));
    }

    /** e.g. choosing an active boutique the user doesn't work at; without this the catch-all below made it a 500. */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(org.springframework.security.access.AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError("FORBIDDEN", exception.getMessage(), 403));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception exception) {
        log.error("Unhandled exception in identity endpoint", exception);
        ApiError error = new ApiError("INTERNAL_SERVER_ERROR", "An unexpected error occurred", 500);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception) {
        ApiError error = new ApiError("INVALID_INPUT", exception.getMessage(), 400);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public  ResponseEntity<ApiError> handleAuthenticationFailed(AuthenticationFailedException exception) {
        ApiError error = new ApiError("AUTHENTICATION_FAILED", exception.getMessage(), 401);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);

    }
}
