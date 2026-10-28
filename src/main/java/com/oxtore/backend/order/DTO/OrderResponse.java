package com.oxtore.backend.order.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Same shape for the buyer ("My orders") and the store ("Orders" in boutique management). */
public record OrderResponse(
        String id,
        String reference,
        String status,
        String paymentMethod,
        String boutiqueId,
        String boutiqueName,
        String boutiqueLogo,
        String customerName,
        String customerPhone,
        String city,
        String address,
        String note,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal total,
        int itemCount,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<Item> items
) {
    public record Item(String productId, String name, String image, BigDecimal unitPrice, int quantity,
                       BigDecimal total) {}
}
