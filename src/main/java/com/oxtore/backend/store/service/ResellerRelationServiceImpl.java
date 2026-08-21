package com.oxtore.backend.store.service;

import com.oxtore.backend.store.entity.ResellerRelation;
import com.oxtore.backend.store.entity.Store;
import com.oxtore.backend.store.enums.ResellerRelationStatus;
import com.oxtore.backend.store.repository.ResellerRelationRepository;
import com.oxtore.backend.store.repository.StoreRepository;
import com.sun.jdi.request.InvalidRequestStateException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ResellerRelationServiceImpl implements ResellerRelationService {

    private final ResellerRelationRepository resellerRelationRepository;
    private final StoreRepository storeRepository;

    @Override
    @Transactional
    public ResellerRelation createRequest(Long sourceStoreId, Long resellerStoreId, Long requestedBy) {
        if (sourceStoreId.equals(resellerStoreId)) {
            throw new IllegalArgumentException("A store cannot be its own reseller");
        }

        Store sourceStore = storeRepository.findById(sourceStoreId)
                .orElseThrow(() -> new EntityNotFoundException("Source store not found: " + sourceStoreId));

        Store resellerStore = storeRepository.findById(resellerStoreId)
                .orElseThrow(() -> new EntityNotFoundException("Reseller store not found: " + resellerStoreId));

        resellerRelationRepository.findBySourceStore_IdAndResellerStore_Id(sourceStoreId, resellerStoreId)
                .ifPresent(existing -> {
                    if (existing.getStatus() == ResellerRelationStatus.ACCEPTED
                            || existing.getStatus() == ResellerRelationStatus.PENDING) {
                        throw new IllegalStateException(
                                "A relation already exists between these stores with status: " + existing.getStatus());
                    }
                });

        ResellerRelation relation = ResellerRelation.builder()
                .sourceStore(sourceStore)
                .resellerStore(resellerStore)
                .status(ResellerRelationStatus.PENDING)
                .requestedBy(requestedBy)
                .build();

        return resellerRelationRepository.save(relation);
    }

    @Override
    @Transactional
    public ResellerRelation decide(Long relationId, Long sourceStoreId, boolean accept, Long decidedBy) {

        ResellerRelation relation = resellerRelationRepository.findByIdAndSourceStore_Id(relationId, sourceStoreId)
                .orElseThrow(() -> new EntityNotFoundException("Reseller relation not found"));

        if (relation.getStatus() != ResellerRelationStatus.PENDING) {
            throw new IllegalStateException(relation.getStatus().toString());
        }

        relation.setStatus(accept ? ResellerRelationStatus.ACCEPTED : ResellerRelationStatus.DECLINED);
        relation.setDecidedBy(decidedBy);

        return resellerRelationRepository.save(relation);
    }

    @Override
    @Transactional
    public ResellerRelation revoke(Long relationId, Long sourceStoreId, Long decidedBy) {

        ResellerRelation relation = resellerRelationRepository.findByIdAndSourceStore_Id(relationId, sourceStoreId)
                .orElseThrow(() -> new EntityNotFoundException("Reseller relation not found"));

        if (relation.getStatus() != ResellerRelationStatus.ACCEPTED) {
            throw new IllegalStateException(relation.getStatus().toString());
        }

        relation.setStatus(ResellerRelationStatus.REVOKED);
        relation.setDecidedBy(decidedBy);
        return resellerRelationRepository.save(relation);

    }

    @Override
    public boolean isAcceptedReseller(Long sourceStoreId, Long resellerStoreId) {
        return resellerRelationRepository
                .findBySourceStore_IdAndResellerStore_IdAndStatus(
                        sourceStoreId, resellerStoreId, ResellerRelationStatus.ACCEPTED)
                .isPresent();
    }

    @Override
    public List<ResellerRelation> getMyResellers(Long sourceStoreId) {
        return resellerRelationRepository.findBySourceStore_IdAndStatus(sourceStoreId, ResellerRelationStatus.ACCEPTED);
    }

    @Override
    public List<ResellerRelation> getPendingRequestsForSource(Long sourceStoreId) {
        return resellerRelationRepository.findBySourceStore_IdAndStatus(sourceStoreId, ResellerRelationStatus.PENDING);
    }
}
