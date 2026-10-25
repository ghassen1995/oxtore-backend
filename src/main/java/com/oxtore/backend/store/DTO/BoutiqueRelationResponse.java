package com.oxtore.backend.store.DTO;

import java.time.LocalDateTime;

public record BoutiqueRelationResponse(
        Long id, Long fromBoutiqueId, String fromBoutiqueName,
        Long toBoutiqueId, String toBoutiqueName,
        String type, String status, String description,
        LocalDateTime approvedAt, Long approvedBy, LocalDateTime createdAt
) {}