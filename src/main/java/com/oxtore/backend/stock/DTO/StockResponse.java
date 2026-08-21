package com.oxtore.backend.stock.DTO;

import com.oxtore.backend.product.DTOs.CommissionRuleResponse;
import com.oxtore.backend.product.DTOs.WholesalePriceTierResponse;
import com.oxtore.backend.product.enums.SaleType;
import com.oxtore.backend.product.enums.TransactionMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record StockResponse(
        Long id,
        Long productId,
        String productName,
        Long storeId,
        String storeName,
        Integer quantity,
        SaleType saleType,
        TransactionMode transactionMode,
        BigDecimal sellingPrice,
        BigDecimal marketPrice,
        CommissionRuleResponse commissionRule,
        List<WholesalePriceTierResponse> wholesalePriceTiers,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}