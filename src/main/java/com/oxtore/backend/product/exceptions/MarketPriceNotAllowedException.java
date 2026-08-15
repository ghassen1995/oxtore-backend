package com.oxtore.backend.product.exceptions;

public class MarketPriceNotAllowedException extends RuntimeException {

    public MarketPriceNotAllowedException() {
        super("Market price is not allowed, sale type must be WHOLESALE");
    }

    public MarketPriceNotAllowedException(String message) {
        super(message);
    }
}