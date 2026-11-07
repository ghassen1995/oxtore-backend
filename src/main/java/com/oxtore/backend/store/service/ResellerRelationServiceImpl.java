package com.oxtore.backend.store.service;

import com.oxtore.backend.common.error.ApiException;
import com.oxtore.backend.notification.enums.NotificationType;
import com.oxtore.backend.notification.service.NotificationRecipients;
import com.oxtore.backend.notification.service.NotificationService;
import com.oxtore.backend.store.entity.ResellerRelation;
import com.oxtore.backend.store.entity.Store;
import com.oxtore.backend.store.enums.ResellerRelationStatus;
import com.oxtore.backend.store.repository.ResellerRelationRepository;
import com.oxtore.backend.store.repository.StoreRepository;
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
    private final NotificationService notificationService;
    private final NotificationRecipients notificationRecipients;

    /** The Angular app deep-links to the "Received" tab on this exact title — do not translate. */
    private static final String NEW_NETWORK_REQUEST_TITLE = "New Network Request";

    @Override
    @Transactional
    public ResellerRelation createRequest(Long sourceStoreId, Long resellerStoreId, Long requestedBy, String message) {
        if (sourceStoreId.equals(resellerStoreId)) {
            throw ApiException.unprocessable("SAME_SHOP", "A shop can't send a partnership request to itself");
        }

        Store sourceStore = storeRepository.findById(sourceStoreId)
                .orElseThrow(() -> ApiException.notFound("BOUTIQUE_NOT_FOUND", "Boutique not found").at("toBoutiqueId"));

        Store resellerStore = storeRepository.findById(resellerStoreId)
                .orElseThrow(() -> ApiException.notFound("BOUTIQUE_NOT_FOUND", "Boutique not found").at("fromBoutiqueId"));

        // Several rows may exist for a pair (declined / revoked ones are kept); only a live one blocks a new request.
        resellerRelationRepository.findAllBySourceStore_IdAndResellerStore_Id(sourceStoreId, resellerStoreId)
                .forEach(existing -> {
                    if (existing.getStatus() == ResellerRelationStatus.PENDING) {
                        throw ApiException.conflict("REQUEST_ALREADY_PENDING", "A request to this shop is already pending");
                    }
                    if (existing.getStatus() == ResellerRelationStatus.ACCEPTED) {
                        throw ApiException.conflict("ALREADY_PARTNERS", "You are already a partner of this shop");
                    }
                });

        ResellerRelation relation = ResellerRelation.builder()
                .sourceStore(sourceStore)
                .resellerStore(resellerStore)
                .status(ResellerRelationStatus.PENDING)
                .requestedBy(requestedBy)
                .message(message)
                .build();

        ResellerRelation saved = resellerRelationRepository.save(relation);
        notificationService.notify(
                notificationRecipients.supervisorsOf(sourceStoreId),
                requestedBy,
                NotificationType.NETWORK,
                NEW_NETWORK_REQUEST_TITLE,
                "%s souhaite devenir revendeur de votre boutique".formatted(resellerStore.getStoreName()),
                "business-outline",
                java.util.Map.of("requestId", String.valueOf(saved.getId()),
                        "fromBoutiqueId", String.valueOf(resellerStoreId)));
        return saved;
    }

    @Override
    @Transactional
    public ResellerRelation decide(Long relationId, Long sourceStoreId, boolean accept, Long decidedBy,
                                   String rejectionReason) {

        ResellerRelation relation = resellerRelationRepository.findByIdAndSourceStore_Id(relationId, sourceStoreId)
                .orElseThrow(() -> ApiException.notFound("REQUEST_NOT_FOUND", "Partnership request not found"));

        if (relation.getStatus() != ResellerRelationStatus.PENDING) {
            throw ApiException.conflict("REQUEST_NOT_PENDING", "This request was already answered");
        }

        relation.setStatus(accept ? ResellerRelationStatus.ACCEPTED : ResellerRelationStatus.DECLINED);
        relation.setDecidedBy(decidedBy);
        if (!accept) relation.setRejectionReason(rejectionReason);

        ResellerRelation saved = resellerRelationRepository.save(relation);
        notificationService.notify(
                java.util.List.of(saved.getRequestedBy()),
                decidedBy,
                NotificationType.NETWORK,
                accept ? "Partenariat accepté" : "Partenariat refusé",
                "%s a %s votre demande de partenariat".formatted(
                        saved.getSourceStore().getStoreName(), accept ? "accepté" : "refusé"),
                accept ? "checkmark-circle-outline" : "close-circle-outline",
                java.util.Map.of("requestId", String.valueOf(saved.getId()),
                        "toBoutiqueId", String.valueOf(saved.getSourceStore().getId())));
        return saved;
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
