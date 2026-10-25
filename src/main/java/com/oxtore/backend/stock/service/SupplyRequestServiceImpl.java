package com.oxtore.backend.stock.service;

import com.oxtore.backend.notification.enums.NotificationType;
import com.oxtore.backend.notification.service.NotificationRecipients;
import com.oxtore.backend.notification.service.NotificationService;
import com.oxtore.backend.product.enums.SaleType;
import com.oxtore.backend.product.enums.TransactionMode;
import com.oxtore.backend.stock.entity.Stock;
import com.oxtore.backend.stock.entity.SupplyRequest;
import com.oxtore.backend.stock.enums.SupplyRequestStatus;
import com.oxtore.backend.stock.exceptions.InsufficientStockException;
import com.oxtore.backend.stock.repository.StockRepository;
import com.oxtore.backend.stock.repository.SupplyRequestRepository;
import com.oxtore.backend.store.entity.Store;
import com.oxtore.backend.store.repository.StoreRepository;
import com.oxtore.backend.store.service.ResellerRelationService;
import com.sun.jdi.request.InvalidRequestStateException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
@AllArgsConstructor
public class SupplyRequestServiceImpl implements SupplyRequestService {

    private final SupplyRequestRepository supplyRequestRepository;
    private final StockRepository stockRepository;
    private final StockService stockService;
    private final StoreRepository storeRepository;
    private final ResellerRelationService resellerRelationService;
    private final NotificationService notificationService;
    private final NotificationRecipients notificationRecipients;

    @Transactional
    @Override
    public SupplyRequest createRequest(Long productId, Long sourceStoreId, Long resellerStoreId,
                                       Integer requestedQuantity, Long requestedBy) {

        if (!resellerRelationService.isAcceptedReseller(sourceStoreId, resellerStoreId)) {
            throw new IllegalStateException(
                    "Store %d is not an accepted reseller of store %d".formatted(resellerStoreId, sourceStoreId));
        }

        if (sourceStoreId.equals(resellerStoreId)) {
            throw new IllegalArgumentException("Source and reseller store cannot be the same");
        }

        Stock sourceStock = stockRepository.findByProduct_IdAndStore_Id(productId, sourceStoreId)
                .orElseThrow(() -> new EntityNotFoundException("Source stock not found"));

        Store resellerStore = storeRepository.findById(resellerStoreId)
                .orElseThrow(() -> new EntityNotFoundException("Reseller store not found: " + resellerStoreId));

        if (sourceStock.getQuantity() < requestedQuantity) {
            throw new InsufficientStockException(sourceStock.getId(), sourceStock.getQuantity(), requestedQuantity);
        }

        SupplyRequest request = SupplyRequest.builder()
                .product(sourceStock.getProduct())
                .sourceStore(sourceStock.getStore())
                .resellerStore(resellerStore)
                .requestedQuantity(requestedQuantity)
                .status(SupplyRequestStatus.PENDING)
                .requestedBy(requestedBy)
                .build();

        SupplyRequest saved = supplyRequestRepository.save(request);
        notificationService.notify(
                notificationRecipients.supervisorsOf(saved.getSourceStore().getId()),
                requestedBy,
                NotificationType.STOCK_REQUEST,
                "Nouvelle demande de stock",
                "%s demande %d × %s".formatted(
                        resellerStore.getStoreName(), requestedQuantity, saved.getProduct().getProductName()),
                "cube-outline",
                Map.of("requestId", String.valueOf(saved.getId()),
                        "fromBoutiqueId", String.valueOf(resellerStoreId)));
        return saved;
    }

    @Transactional
    @Override
    public SupplyRequest decide(Long requestId, Long sourceStoreId, boolean approve, Long decidedBy) {
        SupplyRequest request = supplyRequestRepository.findByIdAndSourceStore_Id(requestId, sourceStoreId)
                .orElseThrow(() -> new EntityNotFoundException("Supply request not found"));

        if (request.getStatus() != SupplyRequestStatus.PENDING) {
            throw new IllegalStateException("Invalid request state: " + request.getStatus());
        }

        request.setStatus(approve ? SupplyRequestStatus.APPROVED : SupplyRequestStatus.DECLINED);
        request.setDecidedBy(decidedBy);
        SupplyRequest saved = supplyRequestRepository.save(request);

        List<Long> recipients = new ArrayList<>();
        recipients.add(saved.getRequestedBy());
        recipients.addAll(notificationRecipients.supervisorsOf(saved.getResellerStore().getId()));
        notificationService.notify(
                recipients,
                decidedBy,
                NotificationType.STOCK_REQUEST,
                approve ? "Demande de stock approuvée" : "Demande de stock refusée",
                "%s a %s votre demande de %d × %s".formatted(
                        saved.getSourceStore().getStoreName(), approve ? "approuvé" : "refusé",
                        saved.getRequestedQuantity(), saved.getProduct().getProductName()),
                approve ? "checkmark-circle-outline" : "close-circle-outline",
                Map.of("requestId", String.valueOf(saved.getId())));
        return saved;
    }

    @Transactional
    @Override
    public Stock confirmArrival(Long requestId, Long resellerStoreId) {
        SupplyRequest request = supplyRequestRepository.findByIdAndResellerStore_Id(requestId, resellerStoreId)
                .orElseThrow(() -> new EntityNotFoundException("Supply request not found"));

        if (request.getStatus() != SupplyRequestStatus.APPROVED) {
            throw new IllegalStateException("Invalid request state: " + request.getStatus());
        }

        Stock sourceStock = stockRepository
                .findByProduct_IdAndStore_Id(request.getProduct().getId(), request.getSourceStore().getId())
                .orElseThrow(() -> new EntityNotFoundException("Source stock not found"));

        if (sourceStock.getQuantity() < request.getRequestedQuantity()) {
            throw new InsufficientStockException(
                    sourceStock.getId(), sourceStock.getQuantity(), request.getRequestedQuantity());
        }
        sourceStock.setQuantity(sourceStock.getQuantity() - request.getRequestedQuantity());
        stockRepository.save(sourceStock);

        Stock resellerStock = stockRepository
                .findByProduct_IdAndStore_Id(request.getProduct().getId(), resellerStoreId)
                .map(existing -> {
                    existing.setQuantity(existing.getQuantity() + request.getRequestedQuantity());
                    return stockRepository.save(existing);
                })
                .orElseGet(() -> stockService.createStock(
                        resellerStoreId,
                        request.getProduct().getId(),
                        request.getRequestedQuantity(),
                        // The reseller always sells the received goods retail, as a direct sale,
                        // whatever terms the source stock was created with (consignment / wholesale).
                        SaleType.RETAIL,
                        TransactionMode.DIRECT_SALE,
                        sourceStock.getRetailPrice(),
                        null,
                        null,
                        null,
                        request.getId()
                ));

        request.setStatus(SupplyRequestStatus.CONFIRMED);

        // No actor id reaches this method, so nobody is excluded; the recipients are on the source side anyway.
        notificationService.notify(
                notificationRecipients.supervisorsOf(request.getSourceStore().getId()),
                null,
                NotificationType.STOCK_REQUEST,
                "Réception confirmée",
                "%s a confirmé la réception de %d × %s".formatted(
                        request.getResellerStore().getStoreName(), request.getRequestedQuantity(),
                        request.getProduct().getProductName()),
                "cube-outline",
                Map.of("requestId", String.valueOf(request.getId())));

        return resellerStock;
    }
}
