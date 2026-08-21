package com.oxtore.backend.stock.controller;

import com.oxtore.backend.common.security.RequireStoreRole;
import com.oxtore.backend.staff.enums.StaffRole;
import com.oxtore.backend.stock.DTO.CreateSupplyRequestRequest;
import com.oxtore.backend.stock.DTO.SupplyDecisionRequest;
import com.oxtore.backend.stock.DTO.SupplyRequestResponse;
import com.oxtore.backend.stock.entity.SupplyRequest;
import com.oxtore.backend.stock.mapper.SupplyRequestMapper;
import com.oxtore.backend.stock.service.SupplyRequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stores/{sourceStoreId}/supply-requests")
@RequiredArgsConstructor
public class SupplyRequestController {
    private final SupplyRequestService supplyRequestService;
    private final SupplyRequestMapper supplyRequestMapper;


    @RequireStoreRole(value = StaffRole.SUPERVISOR, storeIdSource = "body")
    @PostMapping
    public ResponseEntity<SupplyRequestResponse> createRequest(
            @PathVariable Long sourceStoreId,
            @Valid @RequestBody CreateSupplyRequestRequest request,
            HttpServletRequest httpRequest) {

        Long requestedBy = Long.valueOf(httpRequest.getAttribute("userId").toString());
        SupplyRequest supplyRequest = supplyRequestService.createRequest(
                request.productId(), sourceStoreId, request.resellerStoreId(),
                request.requestedQuantity(), requestedBy);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(supplyRequestMapper.toResponse(supplyRequest));
    }

    @RequireStoreRole(value = StaffRole.SUPERVISOR, pathParam = "sourceStoreId")
    @PatchMapping("/{requestId}/decision")
    public ResponseEntity<SupplyRequestResponse> decide(
            @PathVariable Long sourceStoreId,
            @PathVariable Long requestId,
            @Valid @RequestBody SupplyDecisionRequest request,
            HttpServletRequest httpRequest) {

        Long decidedBy = Long.valueOf(httpRequest.getAttribute("userId").toString());
        SupplyRequest supplyRequest = supplyRequestService.decide(
                requestId, sourceStoreId, request.accept(), decidedBy);

        return ResponseEntity.ok(supplyRequestMapper.toResponse(supplyRequest));
    }

   /* @GetMapping
    public ResponseEntity<List<SupplyRequestResponse>> getPendingForSource(
            @PathVariable Long sourceStoreId) {

        List<SupplyRequestResponse> requests = supplyRequestService
                .getPendingRequestsForSource(sourceStoreId)
                .stream()
                .map(supplyRequestMapper::toResponse)
                .toList();

        return ResponseEntity.ok(requests);
    }*/
}
