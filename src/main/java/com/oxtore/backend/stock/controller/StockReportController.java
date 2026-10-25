package com.oxtore.backend.stock.controller;

import com.oxtore.backend.common.security.RequireStoreRole;
import com.oxtore.backend.product.entity.Product;
import com.oxtore.backend.staff.enums.StaffRole;
import com.oxtore.backend.stock.entity.Stock;
import com.oxtore.backend.stock.repository.StockRepository;
import com.oxtore.backend.store.entity.Store;
import com.oxtore.backend.store.repository.StoreRepository;
import com.oxtore.backend.upload.ImageUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * "My stock" report: every Stock row of a store, tagged "own" (the store created the product)
 * or "sourced" (it came from another store through a supply request). Shape matches stock.api.ts.
 */
@RestController
@RequiredArgsConstructor
public class StockReportController {

    private final StockRepository stockRepository;
    private final StoreRepository storeRepository;
    private final ImageUrlResolver imageUrlResolver;

    @Transactional(readOnly = true)
    @GetMapping("/api/stock/items")
    @RequireStoreRole(value = {StaffRole.SUPERVISOR, StaffRole.MANAGER}, pathParam = "boutiqueId")
    public ResponseEntity<Map<String, Object>> myStock(@RequestParam("boutiqueId") Long boutiqueId) {
        List<Stock> stocks = stockRepository.findWithProductByStoreId(boutiqueId);

        List<Long> sourceIds = stocks.stream()
                .map(s -> s.getProduct().getStoreId())
                .filter(id -> !id.equals(boutiqueId))
                .distinct()
                .toList();
        Map<Long, String> sourceNames = storeRepository.findAllById(sourceIds).stream()
                .collect(Collectors.toMap(Store::getId, Store::getStoreName));

        List<Map<String, Object>> data = stocks.stream()
                .map(s -> toStockItem(s, boutiqueId, sourceNames))
                .toList();
        return ResponseEntity.ok(Map.of("data", data, "meta", Map.of("total", data.size())));
    }

    private Map<String, Object> toStockItem(Stock stock, Long boutiqueId, Map<Long, String> sourceNames) {
        Product p = stock.getProduct();
        boolean sourced = !p.getStoreId().equals(boutiqueId);
        int quantity = stock.getQuantity() != null ? stock.getQuantity() : 0;

        Map<String, Object> product = new LinkedHashMap<>();
        product.put("name", p.getProductName());
        product.put("sku", "");
        product.put("image", imageUrlResolver.firstUrl(p));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", stock.getId());
        map.put("productId", p.getId());
        map.put("boutiqueId", boutiqueId);
        map.put("quantity", quantity);
        map.put("available", quantity);
        map.put("reserved", 0);
        map.put("minQuantity", 0);
        map.put("safetyStock", 0);
        map.put("reorderLevel", 0);
        map.put("status", quantity == 0 ? "out_of_stock" : "in_stock");
        map.put("createdAt", stock.getCreatedAt());
        map.put("updatedAt", stock.getUpdatedAt());
        map.put("product", product);
        map.put("saleType", stock.getSaleType() != null ? stock.getSaleType().name() : null);
        map.put("transactionMode", stock.getTransactionMode() != null ? stock.getTransactionMode().name() : null);
        map.put("retailPrice", stock.getRetailPrice() != null ? stock.getRetailPrice() : BigDecimal.ZERO);
        map.put("origin", sourced ? "sourced" : "own");
        map.put("sourceBoutiqueId", sourced ? p.getStoreId() : null);
        map.put("sourceBoutiqueName", sourced ? sourceNames.get(p.getStoreId()) : null);
        return map;
    }
}
