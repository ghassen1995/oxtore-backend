package com.oxtore.backend.stock.controller;

import com.oxtore.backend.common.security.RequireStoreRole;
import com.oxtore.backend.staff.enums.StaffRole;
import com.oxtore.backend.stock.DTO.ResellerActivityResponse;
import com.oxtore.backend.stock.service.ResellerActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Source-store view of what its resellers received, sold, still hold and owe.
 * Lives in the stock package so StockExceptionHandler turns AccessDeniedException into a 403.
 */
@RestController
@RequiredArgsConstructor
public class ResellerActivityController {

    private final ResellerActivityService resellerActivityService;

    @GetMapping("/api/boutiques/{sourceStoreId}/reseller-activity")
    @RequireStoreRole(value = {StaffRole.SUPERVISOR, StaffRole.MANAGER}, pathParam = "sourceStoreId")
    public ResponseEntity<ResellerActivityResponse> getResellerActivity(
            @PathVariable("sourceStoreId") Long sourceStoreId,
            @RequestParam(value = "resellerStoreId", required = false) Long resellerStoreId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        return ResponseEntity.ok(
                resellerActivityService.getResellerActivity(sourceStoreId, resellerStoreId, from, to));
    }
}
