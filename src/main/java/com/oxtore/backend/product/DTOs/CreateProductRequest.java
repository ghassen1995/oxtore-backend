package com.oxtore.backend.product.DTOs;


import com.oxtore.backend.common.security.HasStoreId;
import com.oxtore.backend.product.enums.*;

import java.math.BigDecimal;
import java.util.List;

public record CreateProductRequest(
        ProductCondition condition,
        String productName,
        UnitOfSale unitOfSale,
        ProductStatus status,
        Long storeId,
        List<ProductImageRequest> images,
        SaleType saleType,
        TransactionMode transactionMode,
        BigDecimal sellingPrice,
        BigDecimal marketPrice,
        Integer initialStock,
        CommissionRuleRequest commissionRule,
        List<WholesalePriceTierRequest> wholesalePriceTiers
) implements HasStoreId {
    @Override
    public Long resolveStoreId() {
        return storeId;
    }
}
