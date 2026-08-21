package com.oxtore.backend.stock.exceptions;

public class StockAlreadyExistsException extends RuntimeException {
    public StockAlreadyExistsException(Long productId, Long storeId) {
        super("Product with id " + productId + " already exists with id " + storeId);
    }
}
