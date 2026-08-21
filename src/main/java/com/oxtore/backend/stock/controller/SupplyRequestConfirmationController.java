package com.oxtore.backend.stock.controller;

import com.oxtore.backend.common.security.RequireStoreRole;
import com.oxtore.backend.staff.enums.StaffRole;
import com.oxtore.backend.stock.DTO.StockResponse;
import com.oxtore.backend.stock.DTO.SupplyRequestResponse;
import com.oxtore.backend.stock.entity.Stock;
import com.oxtore.backend.stock.mapper.StockMapper;
import com.oxtore.backend.stock.service.SupplyRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stores/{resellerStoreId}/supply-requests")
@RequiredArgsConstructor
public class SupplyRequestConfirmationController {

    private final SupplyRequestService supplyRequestService;
    private final StockMapper stockMapper;

    @RequireStoreRole(value = StaffRole.SUPERVISOR, pathParam = "resellerStoreId")
    @PatchMapping("/{requestId}/confirm")
    public ResponseEntity<StockResponse> confirmArrival(
            @PathVariable Long resellerStoreId,
            @PathVariable Long requestId) {

        Stock resellerStock = supplyRequestService.confirmArrival(requestId, resellerStoreId);
        return ResponseEntity.ok(stockMapper.toResponse(resellerStock));
    }

   /* @GetMapping("/outgoing")
    public ResponseEntity<List<SupplyRequestResponse>> getMyOutgoingRequests(
            @PathVariable Long resellerStoreId) {
        // exposes SupplyRequestRepository.findByResellerStore_IdOrderByRequestedAtDesc,
        // which we defined but never wired to a controller
        List<SupplyRequestResponse> requests = supplyRequestService
                .getOutgoingRequests(resellerStoreId)
                .stream()
                .map(supplyRequestMapper::toResponse)
                .toList();
        return ResponseEntity.ok(requests);
    }*/

}
