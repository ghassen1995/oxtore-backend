package com.oxtore.backend.staff.service;

import com.oxtore.backend.staff.entity.StoreStaff;
import com.oxtore.backend.staff.enums.StaffRole;

import java.util.List;
import java.util.Optional;

public interface StoreStaffService {
    StoreStaff addStaff(Long storeId, String userPublicId, StaffRole role, Long addedBy);
    StoreStaff addStaff(Long storeId, Long userId, StaffRole role, Long addedBy);
    StoreStaff updateRole(Long storeId, Long userId, StaffRole newRole, Long updatedBy);
    void removeStaff(Long storeId, Long userId, Long removedBy);
    List<StoreStaff> getStaffByStore(Long storeId);
    List<StoreStaff> getStaffByStoreAndRole(Long storeId, StaffRole role);
    Optional<StoreStaff> getStaffMember(Long storeId, Long userId);
}