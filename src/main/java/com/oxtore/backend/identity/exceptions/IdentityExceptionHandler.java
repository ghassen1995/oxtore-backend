package com.oxtore.backend.identity.exceptions;

import com.oxtore.backend.identity.DTO.ApiError;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception exception) {
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