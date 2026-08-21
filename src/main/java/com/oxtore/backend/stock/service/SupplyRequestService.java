package com.oxtore.backend.stock.service;

import com.oxtore.backend.stock.entity.Stock;
import com.oxtore.backend.stock.entity.SupplyRequest;

public interface SupplyRequestService {
    public SupplyRequest createRequest(Long productId, Long sourceStoreId, Long resellerStoreId,
                                       Integer requestedQuantity, Long requestedBy);
    public SupplyRequest decide(Long requestId, Long sourceStoreId, boolean approve, Long decidedBy) ;
    public Stock confirmArrival(Long requestId, Long resellerStoreId) ;

    }
