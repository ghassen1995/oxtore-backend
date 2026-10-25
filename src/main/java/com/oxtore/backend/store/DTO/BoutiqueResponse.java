package com.oxtore.backend.store.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BoutiqueResponse(Long id, String name, String logo, String address, String phone,
                               String description, List<String> ownerIds, Long managerId, String managerName,
                               String status, String rejectionReason, String language, String currency,
                               List<String> categories, Integer employeeCount, Integer productCount,
                               BigDecimal revenue, LocalDateTime createdAt) {}