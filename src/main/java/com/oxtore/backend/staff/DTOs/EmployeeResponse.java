package com.oxtore.backend.staff.DTOs;

import com.oxtore.backend.staff.enums.StaffRole;

import java.time.LocalDateTime;

public record EmployeeResponse(Long id, String fullName, String email, String phone, String avatar,
                               StaffRole role, Long boutiqueId, String boutiqueName,
                               String status, LocalDateTime createdAt) {}