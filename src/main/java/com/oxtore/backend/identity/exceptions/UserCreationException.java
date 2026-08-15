package com.oxtore.backend.identity.exceptions;

public class UserCreationException extends RuntimeException{
    private Exception cause;

    public UserCreationException(String message){
        super(message);
    }

    public UserCreationException(String message, Exception cause){
        super(message);
        this.cause = cause;
    }

}
