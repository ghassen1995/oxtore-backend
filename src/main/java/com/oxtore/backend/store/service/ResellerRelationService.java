package com.oxtore.backend.store.service;

import com.oxtore.backend.store.entity.ResellerRelation;

import java.util.List;

public interface ResellerRelationService {

    ResellerRelation createRequest(Long sourceStoreId, Long resellerStoreId, Long requestedBy);

    ResellerRelation decide(Long relationId, Long sourceStoreId, boolean accept, Long decidedBy);

    ResellerRelation revoke(Long relationId, Long sourceStoreId, Long decidedBy);

    boolean isAcceptedReseller(Long sourceStoreId, Long resellerStoreId);

    public List<ResellerRelation> getMyResellers(Long sourceStoreId);

    List<ResellerRelation> getPendingRequestsForSource(Long sourceStoreId);
}