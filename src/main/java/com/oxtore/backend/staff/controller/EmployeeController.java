package com.oxtore.backend.staff.controller;

import com.oxtore.backend.common.error.ApiException;
import com.oxtore.backend.common.security.StoreAccess;
import com.oxtore.backend.identity.repository.IdentityRepository;
import com.oxtore.backend.staff.DTOs.EmployeeResponse;
import com.oxtore.backend.staff.entity.StoreStaff;
import com.oxtore.backend.staff.enums.StaffRole;
import com.oxtore.backend.staff.repository.StoreStaffRepository;
import com.oxtore.backend.staff.service.StoreStaffService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;

/**
 * One team member, by employee id (the store_staff row id the team list returns). Only a MANAGER of that member's
 * boutique may change or remove them. Name, email and phone belong to the user's own profile and are not editable here.
 */
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final StoreStaffService storeStaffService;
    private final StoreStaffRepository storeStaffRepository;
    private final IdentityRepository identityRepository;
    private final StoreAccess storeAccess;
    private final EmployeeAdapterController employees;

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> get(@PathVariable("id") Long id, HttpServletRequest http) {
        StoreStaff staff = load(id);
        storeAccess.require(StoreAccess.currentUserId(http), staff.getStore().getId(), StoreAccess.MANAGERS);
        return ResponseEntity.ok(toResponse(staff));
    }

    /** Body: {role?: MANAGER|SUPERVISOR|SELLER, status?: active|inactive}. Answers the saved member. */
    @PatchMapping("/{id}")
    public ResponseEntity<EmployeeResponse> update(@PathVariable("id") Long id,
                                                   @RequestBody(required = false) Map<String, Object> body,
                                                   HttpServletRequest http) {
        Long userId = StoreAccess.currentUserId(http);
        StoreStaff staff = load(id);
        storeAccess.require(userId, staff.getStore().getId(), StoreAccess.MANAGERS);

        Map<String, Object> changes = body == null ? Map.of() : body;
        StaffRole role = changes.get("role") == null ? null : parseRole(changes.get("role"));
        Boolean active = changes.get("status") == null ? null : parseStatus(changes.get("status"));

        StoreStaff saved = storeStaffService.updateMember(id, role, active, userId);
        return ResponseEntity.ok(toResponse(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id, HttpServletRequest http) {
        Long userId = StoreAccess.currentUserId(http);
        StoreStaff staff = load(id);
        storeAccess.require(userId, staff.getStore().getId(), StoreAccess.MANAGERS);
        storeStaffService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }

    /** MANAGER, SUPERVISOR or SELLER; 400 VALIDATION_ERROR on "role" otherwise. */
    public static StaffRole parseRole(Object raw) {
        String value = String.valueOf(raw).trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "MANAGER" -> StaffRole.MANAGER;
            case "SUPERVISOR" -> StaffRole.SUPERVISOR;
            case "SELLER" -> StaffRole.SELLER;
            default -> throw ApiException.validation("role", "Role must be MANAGER, SUPERVISOR or SELLER");
        };
    }

    private static Boolean parseStatus(Object raw) {
        return switch (String.valueOf(raw).trim().toLowerCase(Locale.ROOT)) {
            case "active" -> true;
            case "inactive" -> false;
            default -> throw ApiException.validation("status", "Status must be active or inactive");
        };
    }

    private StoreStaff load(Long id) {
        return storeStaffRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("EMPLOYEE_NOT_FOUND", "Team member not found"));
    }

    private EmployeeResponse toResponse(StoreStaff staff) {
        return employees.toResponse(staff, identityRepository.findById(staff.getUserId()).orElse(null));
    }
}
