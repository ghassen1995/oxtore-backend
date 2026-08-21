package com.oxtore.backend.stock.DTO;

import com.oxtore.backend.stock.enums.SupplyRequestStatus;

import java.time.LocalDateTime;

public record SupplyRequestResponse(
        Long id,
        Long productId,
        String productName,
        Long sourceStoreId,
        String sourceStoreName,
        Long resellerStoreId,
        String resellerStoreName,
        Integer requestedQuantity,
        SupplyRequestStatus status,
        Long requestedBy,
        Long decidedBy,
        LocalDateTime requestedAt,
        LocalDateTime decidedAt,
        LocalDateTime confirmedAt
) {}