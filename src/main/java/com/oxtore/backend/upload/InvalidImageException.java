package com.oxtore.backend.upload;

/** The uploaded bytes are not a JPEG, PNG or WebP image. */
public class InvalidImageException extends RuntimeException {
    public InvalidImageException(String message) {
        super(message);
    }
}
