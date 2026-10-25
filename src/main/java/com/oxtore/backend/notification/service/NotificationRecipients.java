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
}
