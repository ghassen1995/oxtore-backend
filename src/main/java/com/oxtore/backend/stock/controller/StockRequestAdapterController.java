package com.oxtore.backend.stock.controller;

import com.oxtore.backend.common.security.StoreAccess;
import com.oxtore.backend.product.entity.Product;
import com.oxtore.backend.staff.repository.StoreStaffRepository;
import com.oxtore.backend.stock.entity.Stock;
import com.oxtore.backend.stock.entity.SupplyRequest;
import com.oxtore.backend.stock.enums.SupplyRequestStatus;
import com.oxtore.backend.stock.mapper.SupplyRequestMapper;
import com.oxtore.backend.stock.repository.SupplyRequestRepository;
import com.oxtore.backend.stock.service.StockService;
import com.oxtore.backend.stock.service.SupplyRequestService;
import com.oxtore.backend.upload.ImageUrlResolver;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.oxtore.backend.stock.enums.SupplyRequestStatus.*;

@RestController
@RequestMapping("/api/stock-requests")
@RequiredArgsConstructor
public class StockRequestAdapterController {

    private final SupplyRequestService service;
    private final SupplyRequestRepository repository;
    private final SupplyRequestMapper mapper;
    private final StoreStaffRepository storeStaffRepository;
    private final StockService stockService;
    private final ImageUrlResolver imageUrlResolver;
    private final StoreAccess storeAccess;

    /** Sent by the requesting (reseller) boutique: its supervisor or a manager. */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String,Object> body, HttpServletRequest http) {
        Long userId = StoreAccess.currentUserId(http);
        Long productId = Long.valueOf(body.get("productId").toString());
        Long requesterId = Long.valueOf(body.get("requesterId").toString());
        Long receiverId = Long.valueOf(body.get("receiverId").toString());
        storeAccess.require(userId, requesterId, StoreAccess.MANAGERS);
        Integer qty = (Integer) body.get("quantity");
        SupplyRequest r = service.createRequest(productId, receiverId, requesterId, qty, userId);
        return ResponseEntity.status(201).body(toFrontendShape(r));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable("id") Long id, HttpServletRequest http) {
        Long userId = Long.valueOf(http.getAttribute("userId").toString());
        SupplyRequest existing = repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Supply request not found: " + id));
        // Answered by the supplying (source) boutique: its supervisor or a manager.
        storeAccess.require(userId, existing.getSourceStore().getId(), StoreAccess.MANAGERS);
        return ResponseEntity.ok(toFrontendShape(
                service.decide(id, existing.getSourceStore().getId(), true, userId)));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable("id") Long id, HttpServletRequest http) {
        Long userId = Long.valueOf(http.getAttribute("userId").toString());
        SupplyRequest existing = repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Supply request not found: " + id));
        storeAccess.require(userId, existing.getSourceStore().getId(), StoreAccess.MANAGERS);
        return ResponseEntity.ok(toFrontendShape(
                service.decide(id, existing.getSourceStore().getId(), false, userId)));
    }

    @PatchMapping("/{id}/fulfill")
    public ResponseEntity<?> fulfill(@PathVariable("id") Long id, HttpServletRequest http) {
        SupplyRequest existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Supply request not found: " + id));
        Long userId = StoreAccess.currentUserId(http);
        // Confirming arrival adds stock to the reseller boutique: its supervisor or a manager.
        storeAccess.require(userId, existing.getResellerStore().getId(), StoreAccess.MANAGERS);

        service.confirmArrival(id, existing.getResellerStore().getId());
        SupplyRequest updated = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Supply request not found: " + id));
        return ResponseEntity.ok(toFrontendShape(updated));
    }

    /** Requests of the given boutiques (the caller must work at each), or of all the caller's boutiques. */
    @GetMapping
    public ResponseEntity<Map<String, Object>> findAll(
            @RequestParam(required = false) String boutiqueIds, HttpServletRequest http) {

        Long userId = StoreAccess.currentUserId(http);
        List<Long> ids;
        if (boutiqueIds != null && !boutiqueIds.isBlank()) {
            ids = Arrays.stream(boutiqueIds.split(",")).map(Long::valueOf).toList();
            storeAccess.requireMemberOfAll(userId, ids);
        } else {
            ids = storeAccess.memberStoreIds(userId);
        }
        List<SupplyRequest> requests = ids.isEmpty()
                ? List.of()
                : repository.findBySourceStore_IdInOrResellerStore_IdIn(ids, ids);

        List<?> data = requests.stream().map(this::toFrontendShape).toList();
        return ResponseEntity.ok(Map.of("data", data, "meta", Map.of()));
    }

    private Map<String, Object> toFrontendShape(SupplyRequest r) {
        Product product = r.getProduct();
        Stock stock = null;
        try {
            stock = stockService.getStock(product.getId(), r.getSourceStore().getId());
        } catch (EntityNotFoundException ignored) {
        }

        BigDecimal unitPrice = stock != null ? stock.getRetailPrice() : BigDecimal.ZERO;
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(r.getRequestedQuantity()));
        List<String> images = imageUrlResolver.urls(product);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId());
        map.put("productId", product.getId());
        map.put("requesterId", r.getResellerStore().getId());
        map.put("receiverId", r.getSourceStore().getId());
        map.put("quantity", r.getRequestedQuantity());
        map.put("status", mapStatus(r.getStatus()));
        map.put("note", null);
        map.put("rejectionReason", null);
        map.put("respondedAt", r.getDecidedAt());
        map.put("respondedBy", r.getDecidedBy());
        map.put("fulfilledAt", r.getConfirmedAt());
        map.put("createdAt", r.getRequestedAt());
        map.put("product", Map.of("name", product.getProductName(), "images", images, "price", unitPrice));
        map.put("requester", Map.of("name", r.getResellerStore().getStoreName()));
        map.put("receiver", Map.of("name", r.getSourceStore().getStoreName()));
        return map;
    }

    private String mapStatus(SupplyRequestStatus status) {
        return switch (status) {
            case PENDING -> "PENDING";
            case APPROVED -> "APPROVED";
            case DECLINED -> "REJECTED";
            case CONFIRMED -> "FULFILLED";
        };
    }
}