package com.oxtore.backend.order.controller;

import com.oxtore.backend.common.security.StoreAccess;
import com.oxtore.backend.order.DTO.OrderResponse;
import com.oxtore.backend.order.DTO.PlaceOrderRequest;
import com.oxtore.backend.order.DTO.UpdateOrderStatusRequest;
import com.oxtore.backend.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Marketplace orders. /api/orders is the signed-in buyer's side (any user, with or without a boutique);
 * /api/boutiques/{boutiqueId}/orders is the boutique's side, for its supervisors and managers.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** Checkout: answers {data: [one order per boutique]}. */
    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> place(@RequestBody PlaceOrderRequest body, HttpServletRequest http) {
        List<OrderResponse> created = orderService.place(StoreAccess.currentUserId(http), body);
        return ResponseEntity.status(201).body(Map.of("data", created));
    }

    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> mine(HttpServletRequest http) {
        return ResponseEntity.ok(Map.of("data", orderService.listForBuyer(StoreAccess.currentUserId(http)),
                "meta", Map.of()));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderResponse> mineById(@PathVariable Long orderId, HttpServletRequest http) {
        return ResponseEntity.ok(orderService.getForBuyer(StoreAccess.currentUserId(http), orderId));
    }

    @PatchMapping("/orders/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable Long orderId, HttpServletRequest http) {
        return ResponseEntity.ok(orderService.cancelByBuyer(StoreAccess.currentUserId(http), orderId));
    }

    /** Optional ?status=pending|confirmed|shipped|delivered|rejected|cancelled. */
    @GetMapping("/boutiques/{boutiqueId}/orders")
    public ResponseEntity<Map<String, Object>> boutiqueOrders(@PathVariable Long boutiqueId,
                                                              @RequestParam(required = false) String status,
                                                              HttpServletRequest http) {
        return ResponseEntity.ok(Map.of(
                "data", orderService.listForStore(StoreAccess.currentUserId(http), boutiqueId, status),
                "meta", Map.of()));
    }

    @GetMapping("/boutiques/{boutiqueId}/orders/{orderId}")
    public ResponseEntity<OrderResponse> boutiqueOrder(@PathVariable Long boutiqueId, @PathVariable Long orderId,
                                                       HttpServletRequest http) {
        return ResponseEntity.ok(orderService.getForStore(StoreAccess.currentUserId(http), boutiqueId, orderId));
    }

    @PatchMapping("/boutiques/{boutiqueId}/orders/{orderId}/status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long boutiqueId, @PathVariable Long orderId,
                                                      @RequestBody UpdateOrderStatusRequest body,
                                                      HttpServletRequest http) {
        if (body == null) throw new IllegalArgumentException("status is required");
        return ResponseEntity.ok(orderService.updateStatusByStore(
                StoreAccess.currentUserId(http), boutiqueId, orderId, body.status(), body.reason()));
    }
}
