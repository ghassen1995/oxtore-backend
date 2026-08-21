package com.oxtore.backend.store.controller;

import com.oxtore.backend.common.security.RequireStoreRole;
import com.oxtore.backend.staff.enums.StaffRole;
import com.oxtore.backend.store.DTO.CreateResellerRequest;
import com.oxtore.backend.store.DTO.ResellerDecisionRequest;
import com.oxtore.backend.store.DTO.ResellerRelationResponse;
import com.oxtore.backend.store.entity.ResellerRelation;
import com.oxtore.backend.store.mapper.ResellerRelationMapper;
import com.oxtore.backend.store.service.ResellerRelationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stores/{sourceStoreId}/resellers")
@RequiredArgsConstructor
public class ResellerRelationController {

    private final ResellerRelationService resellerRelationService;
    private final ResellerRelationMapper resellerRelationMapper;

    @RequireStoreRole(value = StaffRole.SUPERVISOR, storeIdSource = "body")
    @PostMapping
    public ResponseEntity<ResellerRelationResponse> requestReseller(
            @PathVariable Long sourceStoreId,
            @Valid @RequestBody CreateResellerRequest request,
            HttpServletRequest httpRequest) {

        Long requestedBy = Long.valueOf(httpRequest.getAttribute("userId").toString());
        ResellerRelation relation = resellerRelationService.createRequest(
                sourceStoreId, request.resellerStoreId(), requestedBy);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resellerRelationMapper.toResponse(relation));
    }

    @RequireStoreRole(value = StaffRole.SUPERVISOR, pathParam = "sourceStoreId")
    @PatchMapping("/{relationId}/decision")
    public ResponseEntity<ResellerRelationResponse> decide(
            @PathVariable Long sourceStoreId,
            @PathVariable Long relationId,
            @Valid @RequestBody ResellerDecisionRequest request,
            HttpServletRequest httpRequest) {

        Long decidedBy = Long.valueOf(httpRequest.getAttribute("userId").toString());
        ResellerRelation relation = resellerRelationService.decide(
                relationId, sourceStoreId, request.accept(), decidedBy);

        return ResponseEntity.ok(resellerRelationMapper.toResponse(relation));
    }

    @PatchMapping("/{relationId}/revoke")
    public ResponseEntity<ResellerRelationResponse> revoke(
            @PathVariable Long sourceStoreId,
            @PathVariable Long relationId,
            HttpServletRequest httpRequest) {

        Long decidedBy = Long.valueOf(httpRequest.getAttribute("userId").toString());
        ResellerRelation relation = resellerRelationService.revoke(relationId, sourceStoreId, decidedBy);

        return ResponseEntity.ok(resellerRelationMapper.toResponse(relation));
    }

    @GetMapping
    public ResponseEntity<List<ResellerRelationResponse>> getMyResellers(
            @PathVariable Long sourceStoreId) {

        List<ResellerRelationResponse> resellers = resellerRelationService.getMyResellers(sourceStoreId)
                .stream()
                .map(resellerRelationMapper::toResponse)
                .toList();

        return ResponseEntity.ok(resellers);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ResellerRelationResponse>> getPendingRequests(
            @PathVariable Long sourceStoreId) {

        List<ResellerRelationResponse> pending = resellerRelationService.getPendingRequestsForSource(sourceStoreId)
                .stream()
                .map(resellerRelationMapper::toResponse)
                .toList();

        return ResponseEntity.ok(pending);
    }
}