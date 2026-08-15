package com.oxtore.backend.product.DTOs;


import com.oxtore.backend.product.enums.*;

import java.math.BigDecimal;
import java.util.List;

public record CreateProductRequest(
        ProductCondition condition,
        SaleType saleType,
        TransactionMode transactionMode,
        String productName,
        BigDecimal marketPrice,
        BigDecimal retailPrice,
        Integer availableStock,
        UnitOfSale unitOfSale,
        CommissionRuleRequest commissionRule,
        List<ProductImageRequest> images,
        List<WholesalePriceTierRequest> wholesalePriceTiers,
        ProductStatus status,
        Long storeId
) {
}
