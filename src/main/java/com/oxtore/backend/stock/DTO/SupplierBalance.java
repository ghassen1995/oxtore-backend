package com.oxtore.backend.stock.DTO;

import java.math.BigDecimal;
import java.util.List;

/** Reseller-side view of one source store: what its products sold for, what was paid back, what is still due. */
public record SupplierBalance(
        Long sourceStoreId,
        String sourceStoreName,
        int totalSold,
        BigDecimal totalSales,
        BigDecimal totalPaid,
        BigDecimal balance,
        List<ResellerPaymentResponse> payments
) {}
