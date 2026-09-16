package com.oxtore.backend.staff.DTOs;

import com.oxtore.backend.staff.enums.StaffRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddStaffRequest(
        @NotBlank String userPublicId,
        @NotNull StaffRole role
) {}