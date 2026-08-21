package com.oxtore.backend.stock.service;

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


@Service
@AllArgsConstructor
public class SupplyRequestServiceImpl implements SupplyRequestService {

    private final SupplyRequestRepository supplyRequestRepository;
    private final StockRepository stockRepository;
    private final StockService stockService;
    private final StoreRepository storeRepository;
    private final ResellerRelationService resellerRelationService;

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

        return supplyRequestRepository.save(request);
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
        return supplyRequestRepository.save(request);
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
                        SaleType.RETAIL,
                        TransactionMode.DIRECT_SALE,
                        sourceStock.getRetailPrice(),
                        null,
                        null,
                        null
                ));

        request.setStatus(SupplyRequestStatus.CONFIRMED);

        return resellerStock;
    }
}
