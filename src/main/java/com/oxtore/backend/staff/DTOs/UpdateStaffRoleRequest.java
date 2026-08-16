package com.oxtore.backend.staff.DTOs;

import com.oxtore.backend.staff.enums.StaffRole;
import jakarta.validation.constraints.NotNull;

public record UpdateStaffRoleRequest(
        @NotNull StaffRole newRole
) {}