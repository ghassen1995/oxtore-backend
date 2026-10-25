package com.oxtore.backend.stock.controller;

import com.oxtore.backend.identity.entity.User;
import com.oxtore.backend.identity.repository.IdentityRepository;
import com.oxtore.backend.product.entity.Product;
import com.oxtore.backend.upload.ImageUrlResolver;
import com.oxtore.backend.product.enums.TransactionMode;
import com.oxtore.backend.staff.repository.StoreStaffRepository;
import com.oxtore.backend.stock.entity.Stock;
import com.oxtore.backend.stock.entity.StockActivity;
import com.oxtore.backend.stock.repository.StockActivityRepository;
import com.oxtore.backend.stock.repository.StockRepository;
import com.oxtore.backend.stock.service.StockService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** In-store cash sales. Each sale is one StockActivity row against the selling store's own Stock. */
@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SalesAdapterController {
    private final StockService stockService;
    private final StockRepository stockRepository;
    private final StockActivityRepository stockActivityRepository;
    private final StoreStaffRepository storeStaffRepository;
    private final IdentityRepository identityRepository;
    private final ImageUrlResolver imageUrlResolver;

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body, HttpServletRequest http) {
        Long userId = currentUserId(http);
        Long boutiqueId = parseId(body.get("boutiqueId"), "boutiqueId");
        requireStaff(boutiqueId, userId);

        if (!(body.get("items") instanceof List<?> items) || items.isEmpty()
                || !(items.get(0) instanceof Map<?, ?> item)) {
            throw new IllegalArgumentException("items must contain at least one item");
        }
        // Single-item sale, matches make-sale.page.ts.
        Long productId = parseId(item.get("productId"), "productId");
        int quantity = parsePositiveInt(item.get("quantity"), "quantity");

        Stock stock = stockService.getStock(productId, boutiqueId);
        StockActivity activity = stockService.recordSale(stock.getId(), quantity, userId);

        User seller = identityRepository.findById(userId).orElse(null);
        return ResponseEntity.status(201).body(toBackendSale(activity, boutiqueId, seller));
    }

    @Transactional(readOnly = true)
    @GetMapping
    public ResponseEntity<Map<String, Object>> findAll(@RequestParam("boutiqueId") Long boutiqueId,
                                                       HttpServletRequest http) {
        requireStaff(boutiqueId, currentUserId(http));

        List<StockActivity> activities = stockActivityRepository.findByStock_Store_IdOrderBySoldAtDesc(boutiqueId);
        Map<Long, User> sellers = identityRepository
                .findAllById(activities.stream().map(StockActivity::getSoldBy).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));

        List<Map<String, Object>> data = activities.stream()
                .map(a -> toBackendSale(a, boutiqueId, sellers.get(a.getSoldBy())))
                .toList();
        return ResponseEntity.ok(Map.of("data", data, "meta", Map.of()));
    }

    /** Everything this store can sell right now: its own Stock rows with quantity > 0, including consigned stock. */
    @Transactional(readOnly = true)
    @GetMapping("/sellable-products")
    public ResponseEntity<Map<String, Object>> sellableProducts(@RequestParam("boutiqueId") Long boutiqueId,
                                                                HttpServletRequest http) {
        requireStaff(boutiqueId, currentUserId(http));

        List<Map<String, Object>> data = stockRepository.findByStore_IdAndQuantityGreaterThan(boutiqueId, 0)
                .stream().map(this::toAngularProductShape).toList();
        return ResponseEntity.ok(Map.of("data", data, "meta", Map.of()));
    }

    private void requireStaff(Long boutiqueId, Long userId) {
        if (storeStaffRepository.findByStore_IdAndUserId(boutiqueId, userId).isEmpty()) {
            throw new AccessDeniedException("User %d is not staff of store %d".formatted(userId, boutiqueId));
        }
    }

    private Long currentUserId(HttpServletRequest http) {
        Object userId = http.getAttribute("userId");
        if (userId == null) {
            throw new AccessDeniedException("No authenticated user found on request");
        }
        return Long.valueOf(userId.toString());
    }

    private Long parseId(Object value, String field) {
        try {
            if (value instanceof Number n && n.doubleValue() == n.longValue()) return n.longValue();
            if (value instanceof String s && !s.isBlank()) return Long.valueOf(s.trim());
        } catch (NumberFormatException ignored) {
            // fall through to the error below
        }
        throw new IllegalArgumentException(field + " must be a valid id");
    }

    private int parsePositiveInt(Object value, String field) {
        Integer parsed = null;
        if (value instanceof Number n && n.doubleValue() == n.intValue()) {
            parsed = n.intValue();
        } else if (value instanceof String s) {
            try {
                parsed = Integer.valueOf(s.trim());
            } catch (NumberFormatException ignored) {
                // fall through to the error below
            }
        }
        if (parsed == null || parsed <= 0) {
            throw new IllegalArgumentException(field + " must be a positive whole number");
        }
        return parsed;
    }

    private Map<String, Object> toBackendSale(StockActivity a, Long boutiqueId, User seller) {
        BigDecimal unitPrice = a.getUnitPrice() != null ? a.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(a.getQuantitySold()));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", a.getId());
        map.put("boutiqueId", boutiqueId);
        map.put("soldBy", a.getSoldBy());
        map.put("employeeId", a.getSoldBy());
        if (seller != null) {
            map.put("employee", Map.of(
                    "id", seller.getId(),
                    "fullName", seller.getFirstName() + " " + seller.getLastName()));
        }
        map.put("items", List.of(Map.of(
                "productId", a.getStock().getProduct().getId(),
                "quantity", a.getQuantitySold(),
                "unitPrice", unitPrice)));
        map.put("subtotal", total);
        map.put("discount", BigDecimal.ZERO);
        map.put("tax", BigDecimal.ZERO);
        map.put("total", total);
        map.put("paymentMethod", "cash");
        map.put("status", "completed");
        map.put("settlementStatus", a.getSettlementStatus());
        map.put("commissions", List.of());
        map.put("createdAt", a.getSoldAt());
        return map;
    }

    /** Same shape as BoutiqueAdapterController's product list, but priced and counted from this store's Stock row. */
    private Map<String, Object> toAngularProductShape(Stock stock) {
        Product p = stock.getProduct();
        List<String> images = imageUrlResolver.urls(p);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("name", p.getProductName());
        map.put("description", "");
        map.put("category", "all");
        map.put("images", images);
        map.put("ownerBoutiqueId", p.getStoreId());
        map.put("sku", "");
        map.put("cost", stock.getRetailPrice());
        map.put("price", stock.getRetailPrice());
        map.put("wholesalePrice", stock.getMarketPrice() != null ? stock.getMarketPrice() : BigDecimal.ZERO);
        map.put("isActive", true);
        map.put("visibility", "public");
        map.put("saleType", stock.getSaleType() != null ? stock.getSaleType().name().toLowerCase() : "retail");
        map.put("transactionMode", stock.getTransactionMode() == TransactionMode.CONSIGNMENT ? "consignment" : "direct");
        map.put("condition", p.getCondition() != null ? p.getCondition().name().toLowerCase() : "new");
        map.put("createdAt", p.getCreatedAt());
        map.put("wholesaleTiers", List.of());
        map.put("commissions", List.of());
        map.put("stock", List.of(Map.of(
                "quantity", stock.getQuantity(),
                "available", stock.getQuantity(),
                "safetyStock", 0,
                "reorderLevel", 0,
                "status", "in_stock")));
        return map;
    }
}
