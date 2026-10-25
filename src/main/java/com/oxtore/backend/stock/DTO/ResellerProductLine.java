package com.oxtore.backend.stock.DTO;

import java.math.BigDecimal;

/** One product held by one reseller. owedUnsettled is computed (quantity x unit price of UNSETTLED sales), never stored. */
public record ResellerProductLine(
        Long productId,
        String productName,
        String productImage,
        int received,
        int sold,
        int remaining,
        BigDecimal owedUnsettled
) {}
