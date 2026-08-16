package com.oxtore.backend.staff.exceptions;

public class StaffAlreadyExistsException extends RuntimeException {
    public StaffAlreadyExistsException(String message) { super(message); }
}