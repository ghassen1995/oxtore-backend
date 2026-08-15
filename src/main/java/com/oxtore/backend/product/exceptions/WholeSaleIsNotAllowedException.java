package com.oxtore.backend.product.exceptions;

public class WholeSaleIsNotAllowedException extends RuntimeException {

    public WholeSaleIsNotAllowedException() {
        super("Whole Sale Price Tire is not allowed when sale type is RETAIL");
    }

    public WholeSaleIsNotAllowedException(String message) {
        super(message);
    }
}
