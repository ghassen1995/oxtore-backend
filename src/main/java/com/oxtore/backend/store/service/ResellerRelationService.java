package com.oxtore.backend.store.service;

import com.oxtore.backend.store.entity.ResellerRelation;

import java.util.List;

public interface ResellerRelationService {

    default ResellerRelation createRequest(Long sourceStoreId, Long resellerStoreId, Long requestedBy) {
        return createRequest(sourceStoreId, resellerStoreId, requestedBy, null);
    }

    /** 422 SAME_SHOP, 409 REQUEST_ALREADY_PENDING / ALREADY_PARTNERS, 404 BOUTIQUE_NOT_FOUND. */
    ResellerRelation createRequest(Long sourceStoreId, Long resellerStoreId, Long requestedBy, String message);

    default ResellerRelation decide(Long relationId, Long sourceStoreId, boolean accept, Long decidedBy) {
        return decide(relationId, sourceStoreId, accept, decidedBy, null);
    }

    /** 409 REQUEST_NOT_PENDING when already answered. */
    ResellerRelation decide(Long relationId, Long sourceStoreId, boolean accept, Long decidedBy, String rejectionReason);

    ResellerRelation revoke(Long relationId, Long sourceStoreId, Long decidedBy);

    boolean isAcceptedReseller(Long sourceStoreId, Long resellerStoreId);

    public List<ResellerRelation> getMyResellers(Long sourceStoreId);

    List<ResellerRelation> getPendingRequestsForSource(Long sourceStoreId);
}