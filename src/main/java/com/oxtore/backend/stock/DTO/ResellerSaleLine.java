package com.oxtore.backend.stock.DTO;

import com.oxtore.backend.stock.enums.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ResellerSaleLine(
        Long id,
        LocalDateTime soldAt,
        Long productId,
        String productName,
        Long resellerStoreId,
        String resellerStoreName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String sellerName,
        SettlementStatus settlementStatus
) {}
