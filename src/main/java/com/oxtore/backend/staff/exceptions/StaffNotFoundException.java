package com.oxtore.backend.staff.exceptions;

public class StaffNotFoundException extends RuntimeException {
    public StaffNotFoundException(String message) { super(message); }
}