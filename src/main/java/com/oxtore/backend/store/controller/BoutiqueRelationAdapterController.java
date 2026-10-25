package com.oxtore.backend.store.controller;

import com.oxtore.backend.common.security.StoreAccess;
import com.oxtore.backend.store.DTO.BoutiqueRelationResponse;
import com.oxtore.backend.store.entity.ResellerRelation;
import com.oxtore.backend.store.enums.ResellerRelationStatus;
import com.oxtore.backend.store.repository.ResellerRelationRepository;
import com.oxtore.backend.store.service.ResellerRelationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/boutique-relations")
@RequiredArgsConstructor
public class BoutiqueRelationAdapterController {

    private final ResellerRelationRepository repository;
    private final ResellerRelationService service;
    private final StoreAccess storeAccess;

    /** Partnerships of a boutique the caller works at (any role), or of all the caller's boutiques. */
    @GetMapping
    public ResponseEntity<Map<String, Object>> findAll(@RequestParam(required = false) String boutiqueId,
                                                       HttpServletRequest http) {
        Long userId = StoreAccess.currentUserId(http);
        List<Long> ids;
        if (boutiqueId != null) {
            ids = List.of(Long.valueOf(boutiqueId));
            storeAccess.requireMemberOfAll(userId, ids);
        } else {
            ids = storeAccess.memberStoreIds(userId);
        }
        List<ResellerRelation> relations = ids.isEmpty()
                ? List.of()
                : repository.findBySourceStore_IdInOrResellerStore_IdIn(ids, ids);

        List<BoutiqueRelationResponse> data = relations.stream()
                .filter(r -> r.getStatus() == ResellerRelationStatus.ACCEPTED)
                .map(this::toRelationResponse)
                .toList();

        return ResponseEntity.ok(Map.of("data", data, "meta", Map.of()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateStatus(@PathVariable("id") Long id,
                                          @RequestBody Map<String, Object> body, HttpServletRequest http) {
        Long userId = StoreAccess.currentUserId(http);
        String newStatus = (String) body.get("status");
        ResellerRelation existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Relation not found: " + id));
        // Either partner may end the partnership, through its supervisor or a manager.
        storeAccess.requireAny(userId,
                List.of(existing.getSourceStore().getId(), existing.getResellerStore().getId()),
                StoreAccess.MANAGERS);

        if ("TERMINATED".equalsIgnoreCase(newStatus) || "SUSPENDED".equalsIgnoreCase(newStatus)) {
            ResellerRelation updated = service.revoke(id, existing.getSourceStore().getId(), userId);
            return ResponseEntity.ok(toRelationResponse(updated));
        }
        throw new IllegalArgumentException("Unsupported relation status: " + newStatus);
    }

    private BoutiqueRelationResponse toRelationResponse(ResellerRelation r) {
        return new BoutiqueRelationResponse(
                r.getId(),
                r.getResellerStore().getId(), r.getResellerStore().getStoreName(),
                r.getSourceStore().getId(), r.getSourceStore().getStoreName(),
                "RESELLER", mapRelationStatus(r.getStatus()), null,
                r.getDecidedAt(), r.getDecidedBy(), r.getRequestedAt()
        );
    }

    private String mapRelationStatus(ResellerRelationStatus status) {
        return switch (status) {
            case ACCEPTED -> "ACTIVE";
            case REVOKED -> "TERMINATED";
            default -> status.name();
        };
    }
}