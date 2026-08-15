package com.oxtore.backend.product.DTOs;

import java.math.BigDecimal;

public record WholesalePriceTierRequest(
        Integer minQuantity,
        BigDecimal unitPrice
) {
}
