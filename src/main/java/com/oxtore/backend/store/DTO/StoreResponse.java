package com.oxtore.backend.store.DTO;


import com.oxtore.backend.store.enums.BusinessSector;

import java.time.LocalDateTime;

public record StoreResponse(
        Long id,
        Long ownerId,
        String storeName,
        String slogan,
        BusinessSector businessSector,
        String location,
        String phoneNumber,
        String email,
        Boolean authenticationEnabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}