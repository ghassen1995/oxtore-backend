package com.oxtore.backend.product.controller;

import com.oxtore.backend.product.DTOs.CategoryResponse;
import com.oxtore.backend.product.entity.Product;
import com.oxtore.backend.product.enums.SaleType;
import com.oxtore.backend.product.enums.TransactionMode;
import com.oxtore.backend.product.repository.CategoryRepository;
import com.oxtore.backend.stock.entity.Stock;
import com.oxtore.backend.stock.repository.StockRepository;
import com.oxtore.backend.store.entity.Store;
import com.oxtore.backend.product.enums.ProductStatus;
import com.oxtore.backend.upload.ImageUrlResolver;
import com.oxtore.backend.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/marketplace")
@RequiredArgsConstructor
public class MarketplaceAdapterController {

    private final CategoryRepository categoryRepository;
    private final StockRepository stockRepository;
    private final ImageUrlResolver imageUrlResolver;

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        List<CategoryResponse> categories = categoryRepository.findAll().stream()
                .map(c -> new CategoryResponse(c.getSlug(), c.getName(), c.getIcon(), c.getSlug()))
                .toList();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> getMarketplaceProducts() {
        List<Stock> retailStocks = stockRepository.findBySaleType(SaleType.RETAIL);
        List<Map<String, Object>> data = retailStocks.stream()
                .filter(s -> s.getProduct().getStatus() != ProductStatus.ARCHIVED)
                .map(this::toMarketplaceProductShape)
                .toList();
        return ResponseEntity.ok(Map.of("data", data, "meta", Map.of()));
    }

    private Map<String, Object> toMarketplaceProductShape(Stock stock) {
        Product p = stock.getProduct();
        Store store = stock.getStore();

        List<String> images = imageUrlResolver.urls(p);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("name", p.getProductName());
        map.put("category", "all");
        map.put("condition", p.getCondition().name());
        map.put("saleType", stock.getSaleType().name());
        map.put("description", "");
        map.put("image", images.isEmpty() ? "" : images.get(0));
        map.put("images", images);
        map.put("price", stock.getRetailPrice());
        map.put("stock", stock.getQuantity());
        map.put("boutique", store.getStoreName());
        map.put("boutiqueId", store.getId());
        map.put("transactionMode", stock.getTransactionMode() == TransactionMode.CONSIGNMENT ? "consignment" : "direct");
        return map;
    }
}

