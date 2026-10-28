package com.oxtore.backend.notification.service;

import com.oxtore.backend.staff.entity.StoreStaff;
import com.oxtore.backend.staff.enums.StaffRole;
import com.oxtore.backend.staff.repository.StoreStaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Resolves who should be told about a store-level event. */
@Component
@RequiredArgsConstructor
public class NotificationRecipients {

    private final StoreStaffRepository storeStaffRepository;

    public List<Long> supervisorsOf(Long storeId) {
        return storeStaffRepository.findAllByStoreIdAndRole(storeId, StaffRole.SUPERVISOR).stream()
                .map(StoreStaff::getUserId)
                .toList();
    }

    /** Supervisors and managers: the people who handle the store's marketplace orders. */
    public List<Long> managersOf(Long storeId) {
        return storeStaffRepository.findAllByStoreId(storeId).stream()
                .filter(s -> s.getRole() == StaffRole.SUPERVISOR || s.getRole() == StaffRole.MANAGER)
                .map(StoreStaff::getUserId)
                .toList();
    }
}
