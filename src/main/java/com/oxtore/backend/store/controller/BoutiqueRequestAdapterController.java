package com.oxtore.backend.store.controller;

import com.oxtore.backend.common.security.StoreAccess;
import com.oxtore.backend.store.DTO.BoutiqueRequestResponse;
import com.oxtore.backend.store.entity.ResellerRelation;
import com.oxtore.backend.store.enums.ResellerRelationStatus;
import com.oxtore.backend.store.mapper.ResellerRelationMapper;
import com.oxtore.backend.store.repository.ResellerRelationRepository;
import com.oxtore.backend.store.service.ResellerRelationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/boutique-requests")
@RequiredArgsConstructor
public class BoutiqueRequestAdapterController {

    private final ResellerRelationService service;
    private final ResellerRelationRepository repository;
    private final ResellerRelationMapper mapper;
    private final StoreAccess storeAccess;

    /** Sent by the requesting (reseller) boutique: its supervisor or a manager. */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, HttpServletRequest http) {
        Long userId = StoreAccess.currentUserId(http);
        Long from = Long.valueOf(body.get("fromBoutiqueId").toString());
        Long to = Long.valueOf(body.get("toBoutiqueId").toString());
        storeAccess.require(userId, from, StoreAccess.MANAGERS);
        ResellerRelation r = service.createRequest(to, from, userId); // source=to, reseller=from
        return ResponseEntity.status(201).body(mapper.toResponse(r));
    }

    @PatchMapping("/{id}/accept")
    public ResponseEntity<?> accept(@PathVariable("id") Long id, HttpServletRequest http) {
        Long userId = StoreAccess.currentUserId(http);
        ResellerRelation existing = repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Supply request not found: " + id));
        // Answered by the receiving (source) boutique: its supervisor or a manager.
        storeAccess.require(userId, existing.getSourceStore().getId(), StoreAccess.MANAGERS);
        ResellerRelation r = service.decide(id, existing.getSourceStore().getId(), true, userId);
        return ResponseEntity.ok(mapper.toResponse(r));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable("id") Long id, HttpServletRequest http) {
        Long userId = Long.valueOf(http.getAttribute("userId").toString());
        ResellerRelation existing = repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Supply request not found: " + id));
        storeAccess.require(userId, existing.getSourceStore().getId(), StoreAccess.MANAGERS);
        ResellerRelation r = service.decide(id, existing.getSourceStore().getId(), false, userId);
        return ResponseEntity.ok(mapper.toResponse(r));
    }

    /** Requests of the given boutiques (the caller must work at each), or of all the caller's boutiques. */
    @GetMapping
    public ResponseEntity<Map<String, Object>> findAll(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String boutiqueId,
            @RequestParam(required = false) String boutiqueIds,
            @RequestParam(required = false) String status,
            HttpServletRequest http) {

        Long userId = StoreAccess.currentUserId(http);
        List<Long> requested = resolveIds(boutiqueId, boutiqueIds);
        storeAccess.requireMemberOfAll(userId, requested);
        List<Long> ids = requested.isEmpty() ? storeAccess.memberStoreIds(userId) : requested;
        List<ResellerRelation> relations = ids.isEmpty()
                ? List.<ResellerRelation>of()
                : repository.findBySourceStore_IdInOrResellerStore_IdIn(ids, ids);

        List<BoutiqueRequestResponse> data = relations.stream()
                .filter(r -> type == null
                        || ("sent".equals(type) && ids.contains(r.getResellerStore().getId()))
                        || ("received".equals(type) && ids.contains(r.getSourceStore().getId())))
                .filter(r -> status == null || mapRequestStatus(r.getStatus()).equalsIgnoreCase(status))
                .map(this::toRequestResponse)
                .toList();

        return ResponseEntity.ok(Map.of("data", data, "meta", Map.of()));
    }

    private List<Long> resolveIds(String boutiqueId, String boutiqueIds) {
        if (boutiqueIds != null && !boutiqueIds.isBlank()) {
            return Arrays.stream(boutiqueIds.split(",")).map(Long::valueOf).toList();
        }
        if (boutiqueId != null) return List.of(Long.valueOf(boutiqueId));
        return List.of();
    }

    private BoutiqueRequestResponse toRequestResponse(ResellerRelation r) {
        return new BoutiqueRequestResponse(
                r.getId(),
                r.getResellerStore().getId(), r.getResellerStore().getStoreName(),
                r.getSourceStore().getId(), r.getSourceStore().getStoreName(),
                "RESELLER", mapRequestStatus(r.getStatus()), null,
                r.getRequestedAt(), r.getDecidedAt(), r.getDecidedBy(), null
        );
    }

    private String mapRequestStatus(ResellerRelationStatus status) {
        return switch (status) {
            case PENDING -> "PENDING";
            case ACCEPTED -> "ACCEPTED";
            case DECLINED -> "REJECTED";
            case REVOKED -> "CANCELLED";
        };
    }
}
