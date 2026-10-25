package com.oxtore.backend.stock.DTO;

import java.math.BigDecimal;
import java.util.List;

public record ResellerSummary(
        Long resellerStoreId,
        String resellerStoreName,
        List<ResellerProductLine> products,
        int totalSold,
        BigDecimal owedUnsettled
) {}
