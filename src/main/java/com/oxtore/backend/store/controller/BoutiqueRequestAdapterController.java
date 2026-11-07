package com.oxtore.backend.store.controller;

import com.oxtore.backend.common.error.ApiException;
import com.oxtore.backend.common.security.StoreAccess;
import com.oxtore.backend.common.validation.InputRules;
import com.oxtore.backend.store.DTO.BoutiqueRequestResponse;
import com.oxtore.backend.store.entity.ResellerRelation;
import com.oxtore.backend.store.enums.ResellerRelationStatus;
import com.oxtore.backend.store.repository.ResellerRelationRepository;
import com.oxtore.backend.store.service.ResellerRelationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Partnership requests. A request goes from the reseller boutique ({@code fromBoutiqueId}, the shop the user works
 * in) to the source boutique ({@code toBoutiqueId}); {@code fromBoutiqueId} is used exactly as sent, once the caller
 * is checked to be a manager there.
 */
@RestController
@RequestMapping("/api/boutique-requests")
@RequiredArgsConstructor
public class BoutiqueRequestAdapterController {

    private final ResellerRelationService service;
    private final ResellerRelationRepository repository;
    private final StoreAccess storeAccess;

    /**
     * Body {fromBoutiqueId, toBoutiqueId, type: "RESELLER", message?} → 201 with the request.
     * Errors: 403 NOT_SHOP_MEMBER / FORBIDDEN, 409 REQUEST_ALREADY_PENDING / ALREADY_PARTNERS, 422 SAME_SHOP.
     */
    @PostMapping
    public ResponseEntity<BoutiqueRequestResponse> create(@RequestBody(required = false) Map<String, Object> body,
                                                          HttpServletRequest http) {
        Long userId = StoreAccess.currentUserId(http);
        Map<String, Object> b = body == null ? Map.of() : body;
        Long from = id(b.get("fromBoutiqueId"), "fromBoutiqueId");
        Long to = id(b.get("toBoutiqueId"), "toBoutiqueId");
        if (b.get("type") != null && !"RESELLER".equalsIgnoreCase(String.valueOf(b.get("type")))) {
            throw ApiException.validation("type", "type must be RESELLER");
        }
        String message = InputRules.text(b.get("message") == null ? null : String.valueOf(b.get("message")),
                "message", "Message", 500, false);

        if (!storeAccess.isMember(userId, from)) {
            throw ApiException.forbidden("NOT_SHOP_MEMBER", "You don't work at the shop sending this request")
                    .at("fromBoutiqueId");
        }
        storeAccess.require(userId, from, StoreAccess.MANAGERS);

        ResellerRelation r = service.createRequest(to, from, userId, message); // source = to, reseller = from
        return ResponseEntity.status(201).body(toRequestResponse(r));
    }

    @PatchMapping("/{id}/accept")
    public ResponseEntity<BoutiqueRequestResponse> accept(@PathVariable("id") Long id, HttpServletRequest http) {
        Long userId = StoreAccess.currentUserId(http);
        ResellerRelation existing = load(id);
        // Answered by the receiving (source) boutique's managers.
        storeAccess.require(userId, existing.getSourceStore().getId(), StoreAccess.MANAGERS);
        ResellerRelation r = service.decide(id, existing.getSourceStore().getId(), true, userId, null);
        return ResponseEntity.ok(toRequestResponse(r));
    }

    /** Body {rejectionReason?}. */
    @PatchMapping("/{id}/reject")
    public ResponseEntity<BoutiqueRequestResponse> reject(@PathVariable("id") Long id,
                                                          @RequestBody(required = false) Map<String, Object> body,
                                                          HttpServletRequest http) {
        Long userId = StoreAccess.currentUserId(http);
        ResellerRelation existing = load(id);
        storeAccess.require(userId, existing.getSourceStore().getId(), StoreAccess.MANAGERS);
        Object reason = body == null ? null : body.get("rejectionReason");
        ResellerRelation r = service.decide(id, existing.getSourceStore().getId(), false, userId,
                InputRules.text(reason == null ? null : String.valueOf(reason), "rejectionReason", "Reason", 255, false));
        return ResponseEntity.ok(toRequestResponse(r));
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
                .map(BoutiqueRequestAdapterController::toRequestResponse)
                .toList();

        return ResponseEntity.ok(Map.of("data", data, "meta", Map.of()));
    }

    private ResellerRelation load(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("REQUEST_NOT_FOUND", "Partnership request not found"));
    }

    private static Long id(Object raw, String field) {
        if (raw == null || String.valueOf(raw).isBlank()) throw ApiException.validation(field, field + " is required");
        try {
            return Long.valueOf(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            throw ApiException.validation(field, field + " is not a valid id");
        }
    }

    private List<Long> resolveIds(String boutiqueId, String boutiqueIds) {
        if (boutiqueIds != null && !boutiqueIds.isBlank()) {
            return Arrays.stream(boutiqueIds.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(Long::valueOf).toList();
        }
        if (boutiqueId != null) return List.of(Long.valueOf(boutiqueId));
        return List.of();
    }

    private static BoutiqueRequestResponse toRequestResponse(ResellerRelation r) {
        return new BoutiqueRequestResponse(
                r.getId(),
                r.getResellerStore().getId(), r.getResellerStore().getStoreName(),
                r.getSourceStore().getId(), r.getSourceStore().getStoreName(),
                "RESELLER", mapRequestStatus(r.getStatus()), r.getMessage(),
                r.getRequestedAt(), r.getDecidedAt(), r.getDecidedBy(), r.getRejectionReason()
        );
    }

    private static String mapRequestStatus(ResellerRelationStatus status) {
        return switch (status) {
            case PENDING -> "PENDING";
            case ACCEPTED -> "ACCEPTED";
            case DECLINED -> "REJECTED";
            case REVOKED -> "CANCELLED";
        };
    }
}
