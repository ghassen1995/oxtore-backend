package com.oxtore.backend.order.enums;

import java.util.Locale;
import java.util.Set;

/**
 * Marketplace order lifecycle. The store moves an order PENDING → CONFIRMED → SHIPPED → DELIVERED, or rejects it while
 * PENDING; the buyer can cancel while PENDING. Serialized lowercase ("pending") to match the Angular Order status.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    REJECTED,
    CANCELLED;

    /** Statuses the store may set from this one. */
    public Set<OrderStatus> storeTransitions() {
        return switch (this) {
            case PENDING -> Set.of(CONFIRMED, REJECTED);
            case CONFIRMED -> Set.of(SHIPPED);
            case SHIPPED -> Set.of(DELIVERED);
            case DELIVERED, REJECTED, CANCELLED -> Set.of();
        };
    }

    public String apiValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** @throws IllegalArgumentException for an unknown value */
    public static OrderStatus fromApi(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("status is required");
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown order status: " + value);
        }
    }
}
