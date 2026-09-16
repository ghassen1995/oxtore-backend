package com.oxtore.backend.staff.service;

import com.oxtore.backend.identity.entity.User;
import com.oxtore.backend.identity.repository.IdentityRepository;
import com.oxtore.backend.staff.entity.StoreStaff;
import com.oxtore.backend.staff.enums.StaffRole;
import com.oxtore.backend.staff.exceptions.InvalidStaffOperationException;
import com.oxtore.backend.staff.exceptions.StaffAlreadyExistsException;
import com.oxtore.backend.staff.exceptions.StaffNotFoundException;
import com.oxtore.backend.staff.exceptions.SupervisorAlreadyExistsException;
import com.oxtore.backend.staff.repository.StoreStaffRepository;
import com.oxtore.backend.store.entity.Store;
import com.oxtore.backend.store.repository.StoreRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreStaffServiceImpl implements StoreStaffService {

    private final StoreStaffRepository storeStaffRepository;
    private final StoreRepository storeRepository;
    private final IdentityRepository identityRepository;

    @Override
    @Transactional
    public StoreStaff addStaff(Long storeId, String userPublicId, StaffRole role, Long addedBy) {
        User user = identityRepository.findByPublicId(userPublicId)
                .orElseThrow(() -> new EntityNotFoundException("No user found with ID: " + userPublicId));

        return addStaff(storeId, user.getId(), role, addedBy);
    }

    @Transactional
    @Override
    public StoreStaff addStaff(Long storeId, Long userId, StaffRole role, Long addedBy) {

        //ToDo: updatedBy must be the store owner

        if (storeStaffRepository.existsByStoreIdAndUserId(storeId, userId)) {
            throw new StaffAlreadyExistsException(
                    "User " + userId + " is already a staff member of store " + storeId);
        }

        if (role == StaffRole.SUPERVISOR && storeStaffRepository.existsByStoreIdAndRole(storeId, StaffRole.SUPERVISOR)) {
            throw new SupervisorAlreadyExistsException(
                    "Store " + storeId + " already has a supervisor");
        }

        Store store = storeRepository.findById(storeId).orElseThrow(() -> new EntityNotFoundException("Store was not found : " + storeId));

        StoreStaff staff = StoreStaff.builder()
                .store(store)
                .userId(userId)
                .role(role)
                .addedBy(addedBy)
                .build();

        return storeStaffRepository.save(staff);
    }

    @Transactional
    @Override
    public StoreStaff updateRole(Long storeId, Long userId, StaffRole newRole, Long updatedBy) {

        //ToDo: updatedBy must be the store owner

        StoreStaff staff = storeStaffRepository.findByStoreIdAndUserId(storeId, userId)
                .orElseThrow(() -> new StaffNotFoundException(
                        "Staff member not found for storeId=" + storeId + " userId=" + userId));

        if (staff.getRole() == StaffRole.SUPERVISOR) {
            throw new InvalidStaffOperationException("Supervisor role cannot be changed");
        }

        if (newRole == StaffRole.SUPERVISOR) {
            throw new InvalidStaffOperationException("Cannot promote to supervisor via role update");
        }

        staff.setRole(newRole);
        return storeStaffRepository.save(staff);
    }

    @Transactional
    @Override
    public void removeStaff(Long storeId, Long userId, Long removedBy) {

        //ToDo: updatedBy must be the store owner

        StoreStaff staff = storeStaffRepository.findByStoreIdAndUserId(storeId, userId)
                .orElseThrow(() -> new StaffNotFoundException(
                        "Staff member not found for storeId=" + storeId + " userId=" + userId));

        if (staff.getRole() == StaffRole.SUPERVISOR) {
            throw new InvalidStaffOperationException("Supervisor cannot be removed from a store");
        }

        storeStaffRepository.delete(staff);
        log.info("Staff member userId={} removed from storeId={} by userId={}", userId, storeId, removedBy);
    }

    @Transactional(readOnly = true)
    @Override
    public List<StoreStaff> getStaffByStore(Long storeId) {
        return storeStaffRepository.findAllByStoreId(storeId);
    }

    @Transactional(readOnly = true)
    @Override
    public List<StoreStaff> getStaffByStoreAndRole(Long storeId, StaffRole role) {
        return storeStaffRepository.findAllByStoreIdAndRole(storeId, role);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<StoreStaff> getStaffMember(Long storeId, Long userId) {
        return storeStaffRepository.findByStoreIdAndUserId(storeId, userId);
    }
}