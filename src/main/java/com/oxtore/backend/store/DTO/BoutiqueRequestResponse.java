package com.oxtore.backend.store.DTO;

import java.time.LocalDateTime;

public record BoutiqueRequestResponse(
        Long id, Long fromBoutiqueId, String fromBoutiqueName,
        Long toBoutiqueId, String toBoutiqueName, String type, String status,
        String message, LocalDateTime createdAt, LocalDateTime respondedAt,
        Long respondedBy, String rejectionReason
) {}