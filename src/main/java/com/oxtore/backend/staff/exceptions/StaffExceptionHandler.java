package com.oxtore.backend.staff.exceptions;

import com.oxtore.backend.product.exceptions.CommissionRuleNotAllowedException;
import com.oxtore.backend.product.exceptions.MarketPriceNotAllowedException;
import com.oxtore.backend.product.exceptions.WholeSaleIsEmptyException;
import com.oxtore.backend.product.exceptions.WholeSaleIsNotAllowedException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.oxtore.backend.staff")
@Order(3)
public class StaffExceptionHandler {
    @ExceptionHandler(InvalidStaffOperationException.class)
    public ResponseEntity<Object> invalidStaffOperationException(InvalidStaffOperationException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(StaffAlreadyExistsException.class)
    public ResponseEntity<Object> staffAlreadyExistsException(StaffAlreadyExistsException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(StaffNotFoundException.class)
    public ResponseEntity<Object> staffNotFoundException(StaffNotFoundException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SupervisorAlreadyExistsException.class)
    public ResponseEntity<Object> supervisorAlreadyExistsException(SupervisorAlreadyExistsException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<Object> buildErrorResponse(String message, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", message);

        return new ResponseEntity<>(errorResponse, status);
    }
}
