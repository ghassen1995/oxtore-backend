package com.oxtore.backend.stock.DTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * totalSold covers the requested period; totalSales, totalPaid and owedUnsettled (the balance still due,
 * totalSales - totalPaid) are all-time, since a payment settles the oldest sales whatever the period shown.
 */
public record ResellerSummary(
        Long resellerStoreId,
        String resellerStoreName,
        List<ResellerProductLine> products,
        int totalSold,
        BigDecimal totalSales,
        BigDecimal totalPaid,
        BigDecimal owedUnsettled
) {}
