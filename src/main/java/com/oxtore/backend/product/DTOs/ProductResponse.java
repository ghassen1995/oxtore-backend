package com.oxtore.backend.product.DTOs;

import com.oxtore.backend.product.enums.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
        Long id,
        ProductCondition condition,
        SaleType saleType,
        TransactionMode transactionMode,
        String productName,
        List<ProductImageResponse> images,
        BigDecimal marketPrice,
        BigDecimal retailPrice,
        Integer availableStock,
        UnitOfSale unitOfSale,
        List<WholesalePriceTierResponse> wholesalePriceTiers,
        ProductStatus status,
        CommissionRuleResponse commissionRule,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long storeId) {
}
