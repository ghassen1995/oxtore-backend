package com.oxtore.backend.upload;

/** A product payload referenced an image that was not uploaded through /api/uploads/images (or too many). */
public class InvalidImageReferenceException extends RuntimeException {
    public InvalidImageReferenceException(String message) {
        super(message);
    }
}
