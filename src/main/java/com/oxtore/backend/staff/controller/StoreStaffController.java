package com.oxtore.backend.staff.controller;

import com.oxtore.backend.common.security.RequireStoreRole;
import com.oxtore.backend.staff.DTOs.AddStaffRequest;
import com.oxtore.backend.staff.DTOs.StoreStaffResponse;
import com.oxtore.backend.staff.DTOs.UpdateStaffRoleRequest;
import com.oxtore.backend.staff.entity.StoreStaff;
import com.oxtore.backend.staff.enums.StaffRole;
import com.oxtore.backend.staff.mappers.StoreStaffMapper;
import com.oxtore.backend.staff.service.StoreStaffService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stores/{storeId}/staff")
@RequiredArgsConstructor
public class StoreStaffController {

    private final StoreStaffService storeStaffService;
    private final StoreStaffMapper storeStaffMapper;

    @RequireStoreRole(value = StaffRole.SUPERVISOR, pathParam = "storeId")
    @PostMapping
    public ResponseEntity<StoreStaffResponse> addStaff(
            @PathVariable Long storeId,
            @Valid @RequestBody AddStaffRequest request,
            HttpServletRequest httpRequest) {

        Long addedBy = Long.valueOf(httpRequest.getAttribute("userId").toString());
        StoreStaff saved = storeStaffService.addStaff(
                storeId, request.userPublicId(), request.role(), addedBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(storeStaffMapper.toResponse(saved));
    }
    @RequireStoreRole(value = StaffRole.SUPERVISOR, pathParam = "storeId")
    @PatchMapping("/{userId}/role")
    public ResponseEntity<StoreStaffResponse> updateRole(
            @PathVariable Long storeId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateStaffRoleRequest request,
            HttpServletRequest httpRequest) {
        Long updatedBy = Long.valueOf(httpRequest.getAttribute("userId").toString());
        StoreStaff updated = storeStaffService.updateRole(
                storeId, userId, request.newRole(), updatedBy);
        return ResponseEntity.ok(storeStaffMapper.toResponse(updated));
    }

    @RequireStoreRole(value = StaffRole.SUPERVISOR, pathParam = "storeId")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeStaff(
            @PathVariable Long storeId,
            @PathVariable Long userId,
            HttpServletRequest httpRequest) {

        Long removedBy = Long.valueOf(httpRequest.getAttribute("userId").toString());
        storeStaffService.removeStaff(storeId, userId, removedBy);
        return ResponseEntity.noContent().build();
    }

    @RequireStoreRole(value = { StaffRole.SUPERVISOR, StaffRole.MANAGER }, pathParam = "storeId")
    @GetMapping
    public ResponseEntity<List<StoreStaffResponse>> getStaffByStore(
            @PathVariable Long storeId) {
        List<StoreStaffResponse> staff = storeStaffService.getStaffByStore(storeId)
                .stream()
                .map(storeStaffMapper::toResponse)
                .toList();
        return ResponseEntity.ok(staff);
    }

    @RequireStoreRole(value = { StaffRole.SUPERVISOR, StaffRole.MANAGER }, pathParam = "storeId")
    @GetMapping("/role/{role}")
    public ResponseEntity<List<StoreStaffResponse>> getStaffByRole(
            @PathVariable Long storeId,
            @PathVariable StaffRole role) {
        List<StoreStaffResponse> staff = storeStaffService.getStaffByStoreAndRole(storeId, role)
                .stream()
                .map(storeStaffMapper::toResponse)
                .toList();
        return ResponseEntity.ok(staff);
    }

    @RequireStoreRole(value = { StaffRole.SUPERVISOR, StaffRole.MANAGER }, pathParam = "storeId")
    @GetMapping("/{userId}")
    public ResponseEntity<StoreStaffResponse> getStaffMember(
            @PathVariable Long storeId,
            @PathVariable Long userId) {
        return storeStaffService.getStaffMember(storeId, userId)
                .map(storeStaffMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}