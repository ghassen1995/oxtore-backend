package com.oxtore.backend.product.exceptions;

public class WholeSaleIsEmptyException extends RuntimeException {

    public WholeSaleIsEmptyException() {
        super("Whole Sale Price Tire is required when sale type is WHOLESALE");
    }

    public WholeSaleIsEmptyException(String message) {
        super(message);
    }
}
