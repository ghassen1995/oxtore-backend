package com.oxtore.backend.product.mapper;

import com.oxtore.backend.common.config.CurrencyCatalog;
import com.oxtore.backend.product.entity.Product;
import com.oxtore.backend.product.entity.WholesalePriceTier;
import com.oxtore.backend.product.enums.ProductStatus;
import com.oxtore.backend.product.enums.TransactionMode;
import com.oxtore.backend.stock.entity.Stock;
import com.oxtore.backend.stock.service.StockService;
import com.oxtore.backend.store.repository.StoreRepository;
import com.oxtore.backend.upload.ImageUrlResolver;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The product shape products.api.ts (BackendProduct) parses; shared by the boutique and product adapters.
 * saleType and transactionMode are returned as stored (never derived from the prices); amounts are major units of
 * {@code currency}.
 */
@Component
@RequiredArgsConstructor
public class AngularProductShapeMapper {

    private final StockService stockService;
    private final ImageUrlResolver imageUrlResolver;
    private final StoreRepository storeRepository;

    public Map<String, Object> toShape(Product p) {
        Stock stock = null;
        try {
            stock = stockService.getStock(p.getId(), p.getStoreId());
        } catch (EntityNotFoundException ignored) {
            // no stock row yet for this product/store pair
        }
        String currency = stock != null
                ? stock.getStore().getCurrency()
                : storeRepository.findById(p.getStoreId()).map(s -> s.getCurrency()).orElse(null);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("name", p.getProductName());
        map.put("description", "");
        map.put("category", "all");
        map.put("images", imageUrlResolver.urls(p));
        map.put("ownerBoutiqueId", p.getStoreId());
        map.put("sku", "");
        map.put("cost", stock != null ? stock.getRetailPrice() : BigDecimal.ZERO);
        map.put("price", stock != null ? stock.getRetailPrice() : BigDecimal.ZERO);
        map.put("currency", CurrencyCatalog.orDefault(currency));
        map.put("wholesalePrice", stock != null && stock.getMarketPrice() != null ? stock.getMarketPrice() : BigDecimal.ZERO);
        map.put("isActive", p.getStatus() == ProductStatus.PUBLISHED);
        map.put("visibility", "public");
        map.put("saleType", stock != null && stock.getSaleType() != null ? stock.getSaleType().apiValue() : "retail");
        map.put("transactionMode", stock != null && stock.getTransactionMode() == TransactionMode.CONSIGNMENT ? "consignment" : "direct");
        map.put("condition", p.getCondition().name().toLowerCase());
        map.put("createdAt", p.getCreatedAt());
        map.put("updatedAt", p.getUpdatedAt());
        map.put("wholesaleTiers", stock != null ? tiers(stock) : List.of());
        map.put("commissions", List.of());
        map.put("stock", stock != null
                ? List.of(Map.of(
                "quantity", stock.getQuantity(),
                "available", stock.getQuantity(),
                "safetyStock", 0,
                "reorderLevel", 0,
                "status", stock.getQuantity() > 0 ? "in_stock" : "out_of_stock"))
                : List.of());
        return map;
    }

    /** [{minQty, unitPrice}], lowest quantity first. */
    public static List<Map<String, Object>> tiers(Stock stock) {
        if (stock.getWholesalePriceTiers() == null) return List.of();
        return stock.getWholesalePriceTiers().stream()
                .map((WholesalePriceTier t) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("minQty", t.getMinQuantity());
                    m.put("unitPrice", t.getUnitPrice());
                    return m;
                })
                .toList();
    }
}
