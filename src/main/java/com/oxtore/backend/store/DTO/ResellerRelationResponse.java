package com.oxtore.backend.store.DTO;

import com.oxtore.backend.store.enums.ResellerRelationStatus;

import java.time.LocalDateTime;

public record ResellerRelationResponse(
        Long id,
        Long sourceStoreId,
        String sourceStoreName,
        Long resellerStoreId,
        String resellerStoreName,
        ResellerRelationStatus status,
        Long requestedBy,
        Long decidedBy,
        LocalDateTime requestedAt,
        LocalDateTime decidedAt
) {}