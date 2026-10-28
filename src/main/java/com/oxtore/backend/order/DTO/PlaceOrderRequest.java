package com.oxtore.backend.order.DTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Checkout body. Items may come from several boutiques; one order is created per boutique, each with
 * {@code deliveryFee}. Unit prices are never taken from the client.
 */
public record PlaceOrderRequest(
        List<Item> items,
        String customerName,
        String customerPhone,
        String city,
        String address,
        String note,
        BigDecimal deliveryFee,
        String paymentMethod
) {
    public record Item(Long productId, Long boutiqueId, Integer quantity) {}
}
