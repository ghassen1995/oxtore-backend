package com.oxtore.backend.stock.DTO;

import java.math.BigDecimal;

/** One product held by one reseller. owedUnsettled is what is still unpaid on its sales (all-time), never stored. */
public record ResellerProductLine(
        Long productId,
        String productName,
        String productImage,
        int received,
        int sold,
        int remaining,
        BigDecimal owedUnsettled
) {}
