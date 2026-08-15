package com.oxtore.backend.identity.exceptions;

public class AuthenticationFailedException extends RuntimeException{
    public AuthenticationFailedException(String message){
        super(message);
    }
}
