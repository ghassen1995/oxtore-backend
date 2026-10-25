package com.oxtore.backend.product.controller;

import com.oxtore.backend.product.entity.Product;
import com.oxtore.backend.product.mapper.AngularProductShapeMapper;
import com.oxtore.backend.product.service.ProductAdapterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** /api/products/{id} as called by products.api.ts (update + delete). */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductAdapterController {

    private final ProductAdapterService productAdapterService;
    private final AngularProductShapeMapper productShapeMapper;

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable("id") Long id,
                                                      @RequestBody Map<String, Object> body,
                                                      HttpServletRequest http) {
        Product updated = productAdapterService.update(id, body, currentUserId(http));
        return ResponseEntity.ok(productShapeMapper.toShape(updated));
    }

    /** Soft delete (archive). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable("id") Long id, HttpServletRequest http) {
        productAdapterService.archive(id, currentUserId(http));
        return ResponseEntity.ok(Map.of("message", "Product archived"));
    }

    private Long currentUserId(HttpServletRequest http) {
        Object userId = http.getAttribute("userId");
        if (userId == null) {
            throw new AccessDeniedException("No authenticated user found on request");
        }
        return Long.valueOf(userId.toString());
    }
}
