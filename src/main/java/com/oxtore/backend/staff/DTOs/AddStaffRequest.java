package com.oxtore.backend.staff.DTOs;

import com.oxtore.backend.staff.enums.StaffRole;
import jakarta.validation.constraints.NotNull;

public record AddStaffRequest(
        @NotNull Long userId,
        @NotNull StaffRole role
) {}