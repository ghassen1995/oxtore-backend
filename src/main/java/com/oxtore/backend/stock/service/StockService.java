package com.oxtore.backend.stock.service;

import com.oxtore.backend.product.DTOs.CommissionRuleRequest;
import com.oxtore.backend.product.DTOs.WholesalePriceTierRequest;
import com.oxtore.backend.product.entity.CommissionRule;
import com.oxtore.backend.product.entity.WholesalePriceTier;
import com.oxtore.backend.product.enums.SaleType;
import com.oxtore.backend.product.enums.TransactionMode;
import com.oxtore.backend.stock.entity.Stock;
import com.oxtore.backend.stock.entity.StockActivity;

import java.math.BigDecimal;
import java.util.List;

public interface StockService {
    public Stock createStock(Long storeId, Long productId, Integer quantity,
                             SaleType saleType, TransactionMode transactionMode,
                             BigDecimal retailPrice, BigDecimal marketPrice,
                             CommissionRuleRequest commissionRuleRequest,
                             List<WholesalePriceTierRequest> wholesalePriceTierRequests,
                             Long sourceSupplyRequestId);
    public StockActivity recordSale(Long stockId, Integer quantitySold, Long soldBy);
    public Stock reclaimStock(Long stockId, Integer reclaimQuantity);
    public List<Stock> getStoreStock(Long storeId);
    Stock getStock(Long productId, Long storeId);
    }
