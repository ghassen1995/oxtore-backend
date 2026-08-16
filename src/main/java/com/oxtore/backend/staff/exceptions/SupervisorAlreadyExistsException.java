package com.oxtore.backend.staff.exceptions;

public class SupervisorAlreadyExistsException extends RuntimeException {
    public SupervisorAlreadyExistsException(String message) { super(message); }
}