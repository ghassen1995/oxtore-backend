package com.oxtore.backend.stock.exceptions;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(Long sourceStockId, Integer sourceStockQuantity, Integer requestedQuantity) {
        super("Insufficient stock for " + sourceStockId + ", " + sourceStockQuantity + ", " + requestedQuantity);
    }
}
