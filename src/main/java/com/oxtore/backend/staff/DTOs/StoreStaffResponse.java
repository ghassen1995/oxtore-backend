package com.oxtore.backend.staff.DTOs;

import java.time.LocalDateTime;
import com.oxtore.backend.staff.enums.StaffRole;


public record StoreStaffResponse(
        Long id,
        Long storeId,
        Long userId,
        StaffRole role,
        Long addedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}