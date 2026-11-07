package com.oxtore.backend.product.controller;

import com.oxtore.backend.common.config.CurrencyCatalog;
import com.oxtore.backend.common.error.ApiException;
import com.oxtore.backend.common.security.StoreAccess;
import com.oxtore.backend.product.DTOs.CategoryResponse;
import com.oxtore.backend.product.entity.Product;
import com.oxtore.backend.product.enums.SaleType;
import com.oxtore.backend.product.enums.TransactionMode;
import com.oxtore.backend.product.repository.CategoryRepository;
import com.oxtore.backend.stock.entity.Stock;
import com.oxtore.backend.stock.repository.StockRepository;
import com.oxtore.backend.store.entity.Store;
import com.oxtore.backend.product.enums.ProductStatus;
import com.oxtore.backend.store.service.StoreRelations;
import com.oxtore.backend.upload.ImageUrlResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
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
    private final StoreRelations storeRelations;

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        List<CategoryResponse> categories = categoryRepository.findAll().stream()
                .map(c -> new CategoryResponse(c.getSlug(), c.getName(), c.getIcon(), c.getSlug()))
                .toList();
        return ResponseEntity.ok(categories);
    }

    /**
     * Public offers (retail or "both"). Each item carries {@code relation} (its boutique's relation to the caller's
     * active boutique) and {@code canOrder} (false for consignment offers when the caller isn't an approved reseller).
     */
    @Transactional(readOnly = true)
    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> getMarketplaceProducts(HttpServletRequest http) {
        StoreRelations.View view = storeRelations.forUser(StoreAccess.currentUserId(http));
        List<Map<String, Object>> data = stockRepository.findBySaleTypeIn(SaleType.SOLD_RETAIL).stream()
                .filter(s -> s.getProduct().getStatus() != ProductStatus.ARCHIVED)
                .map(s -> toMarketplaceProductShape(s, view))
                .toList();
        return ResponseEntity.ok(Map.of("data", data, "meta", Map.of("total", data.size())));
    }

    /**
     * One offer, same shape as a list item. {@code boutiqueId} picks the seller when several boutiques offer the
     * product; otherwise the product's own boutique, else the first offer the caller may see. Besides public offers,
     * trade offers of the caller's own boutique and of its sources are visible here (as in the feed).
     */
    @Transactional(readOnly = true)
    @GetMapping("/products/{id}")
    public ResponseEntity<Map<String, Object>> getMarketplaceProduct(@PathVariable("id") Long productId,
                                                                     @RequestParam(required = false) Long boutiqueId,
                                                                     HttpServletRequest http) {
        StoreRelations.View view = storeRelations.forUser(StoreAccess.currentUserId(http));
        Stock stock = stockRepository.findByProduct_Id(productId).stream()
                .filter(s -> s.getProduct().getStatus() != ProductStatus.ARCHIVED)
                .filter(s -> boutiqueId == null || s.getStore().getId().equals(boutiqueId))
                .filter(s -> isVisible(s, view))
                .min(Comparator.comparing((Stock s) -> !s.getStore().getId().equals(s.getProduct().getStoreId()))
                        .thenComparing(Stock::getId))
                .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND", "Product not found"));
        return ResponseEntity.ok(toMarketplaceProductShape(stock, view));
    }

    private static boolean isVisible(Stock s, StoreRelations.View view) {
        if (s.getSaleType() != null && s.getSaleType().isRetail()) return true;
        String relation = view.relationTo(s.getStore().getId());
        return StoreRelations.OWN.equals(relation) || StoreRelations.SOURCE.equals(relation);
    }

    private Map<String, Object> toMarketplaceProductShape(Stock stock, StoreRelations.View view) {
        Product p = stock.getProduct();
        Store store = stock.getStore();

        List<String> images = imageUrlResolver.urls(p);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("stockId", stock.getId());
        map.put("name", p.getProductName());
        map.put("category", "all");
        map.put("condition", p.getCondition().name());
        // The marketplace sells at the retail price; BOTH offers are shown as retail here.
        map.put("saleType", stock.getSaleType() == SaleType.WHOLESALE ? "WHOLESALE" : "RETAIL");
        map.put("saleMode", stock.getSaleType() == null ? "retail" : stock.getSaleType().apiValue());
        map.put("description", "");
        map.put("image", images.isEmpty() ? "" : images.get(0));
        map.put("images", images);
        map.put("price", stock.getRetailPrice());
        map.put("currency", CurrencyCatalog.orDefault(store.getCurrency()));
        map.put("stock", stock.getQuantity());
        map.put("boutique", store.getStoreName());
        map.put("boutiqueId", store.getId());
        map.put("transactionMode", stock.getTransactionMode() == TransactionMode.CONSIGNMENT ? "consignment" : "direct");
        map.put("relation", view.relationTo(store.getId()));
        map.put("canOrder", view.canOrder(stock));
        return map;
    }
}
