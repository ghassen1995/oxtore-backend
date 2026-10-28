package com.oxtore.backend.stock.controller;

import com.oxtore.backend.common.security.RequireStoreRole;
import com.oxtore.backend.common.security.StoreAccess;
import com.oxtore.backend.staff.enums.StaffRole;
import com.oxtore.backend.stock.DTO.RecordResellerPaymentRequest;
import com.oxtore.backend.stock.DTO.ResellerActivityResponse;
import com.oxtore.backend.stock.DTO.ResellerPaymentResponse;
import com.oxtore.backend.stock.DTO.SupplierBalance;
import com.oxtore.backend.stock.service.ResellerActivityService;
import com.oxtore.backend.stock.service.ResellerSettlementService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Source-store view of what its resellers received, sold, still hold and owe.
 * Lives in the stock package so StockExceptionHandler turns AccessDeniedException into a 403.
 */
@RestController
@RequiredArgsConstructor
public class ResellerActivityController {

    private final ResellerActivityService resellerActivityService;
    private final ResellerSettlementService resellerSettlementService;

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

    /** The source store records cash received from one of its resellers; it settles the oldest unpaid sales. */
    @PostMapping("/api/boutiques/{sourceStoreId}/reseller-payments")
    @RequireStoreRole(value = {StaffRole.SUPERVISOR, StaffRole.MANAGER}, pathParam = "sourceStoreId")
    public ResponseEntity<ResellerPaymentResponse> recordPayment(
            @PathVariable("sourceStoreId") Long sourceStoreId,
            @RequestBody RecordResellerPaymentRequest body,
            HttpServletRequest http) {
        return ResponseEntity.status(201)
                .body(resellerSettlementService.record(sourceStoreId, StoreAccess.currentUserId(http), body));
    }

    @DeleteMapping("/api/boutiques/{sourceStoreId}/reseller-payments/{paymentId}")
    @RequireStoreRole(value = {StaffRole.SUPERVISOR, StaffRole.MANAGER}, pathParam = "sourceStoreId")
    public ResponseEntity<Void> deletePayment(@PathVariable("sourceStoreId") Long sourceStoreId,
                                              @PathVariable("paymentId") Long paymentId) {
        resellerSettlementService.delete(sourceStoreId, paymentId);
        return ResponseEntity.noContent().build();
    }

    /** Reseller side: what it owes each source store whose products it sells, and what it already paid. */
    @GetMapping("/api/boutiques/{resellerStoreId}/supplier-balances")
    @RequireStoreRole(value = {StaffRole.SUPERVISOR, StaffRole.MANAGER}, pathParam = "resellerStoreId")
    public ResponseEntity<Map<String, List<SupplierBalance>>> supplierBalances(
            @PathVariable("resellerStoreId") Long resellerStoreId) {
        return ResponseEntity.ok(Map.of("data", resellerSettlementService.supplierBalances(resellerStoreId)));
    }
}
