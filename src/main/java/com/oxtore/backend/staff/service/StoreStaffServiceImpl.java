package com.oxtore.backend.staff.service;

import com.oxtore.backend.common.error.ApiException;
import com.oxtore.backend.notification.enums.NotificationType;
import com.oxtore.backend.notification.service.NotificationService;
import com.oxtore.backend.identity.entity.User;
import com.oxtore.backend.identity.repository.IdentityRepository;
import com.oxtore.backend.staff.entity.StoreStaff;
import com.oxtore.backend.staff.enums.StaffRole;
import com.oxtore.backend.staff.exceptions.StaffAlreadyExistsException;
import com.oxtore.backend.staff.exceptions.StaffNotFoundException;
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

/**
 * Boutique teams. Any role can be given or changed (MANAGER, SUPERVISOR, SELLER), as long as the boutique keeps at
 * least one active MANAGER; the creator starts as its MANAGER.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StoreStaffServiceImpl implements StoreStaffService {

    private final StoreStaffRepository storeStaffRepository;
    private final StoreRepository storeRepository;
    private final IdentityRepository identityRepository;
    private final NotificationService notificationService;

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
        if (storeStaffRepository.existsByStoreIdAndUserId(storeId, userId)) {
            throw new StaffAlreadyExistsException(
                    "User " + userId + " is already a staff member of store " + storeId);
        }

        Store store = storeRepository.findById(storeId).orElseThrow(() -> new EntityNotFoundException("Store was not found : " + storeId));

        StoreStaff staff = StoreStaff.builder()
                .store(store)
                .userId(userId)
                .role(role)
                .addedBy(addedBy)
                .build();

        StoreStaff saved = storeStaffRepository.save(staff);
        // Store creation adds the owner with addedBy == userId; the actor exclusion skips that case.
        notificationService.notify(
                java.util.List.of(userId),
                addedBy,
                NotificationType.HR,
                "Ajout à une équipe",
                "Vous avez été ajouté(e) à %s en tant que %s".formatted(store.getStoreName(), roleLabelFr(role)),
                "people-outline",
                java.util.Map.of("storeId", String.valueOf(storeId)));
        return saved;
    }

    /** Same labels as the app (fr.json auth.roles). */
    private static String roleLabelFr(StaffRole role) {
        return switch (role) {
            case MANAGER -> "gestionnaire";
            case SUPERVISOR -> "responsable des ventes";
            case SELLER -> "vendeur";
        };
    }

    @Transactional
    @Override
    public StoreStaff updateRole(Long storeId, Long userId, StaffRole newRole, Long updatedBy) {
        StoreStaff staff = storeStaffRepository.findByStoreIdAndUserId(storeId, userId)
                .orElseThrow(() -> new StaffNotFoundException(
                        "Staff member not found for storeId=" + storeId + " userId=" + userId));
        return updateMember(staff.getId(), newRole, null, updatedBy);
    }

    @Transactional
    @Override
    public void removeStaff(Long storeId, Long userId, Long removedBy) {
        StoreStaff staff = storeStaffRepository.findByStoreIdAndUserId(storeId, userId)
                .orElseThrow(() -> new StaffNotFoundException(
                        "Staff member not found for storeId=" + storeId + " userId=" + userId));
        removeMember(staff.getId(), removedBy);
    }

    @Transactional
    @Override
    public StoreStaff updateMember(Long staffId, StaffRole newRole, Boolean active, Long updatedBy) {
        StoreStaff staff = storeStaffRepository.findById(staffId)
                .orElseThrow(() -> ApiException.notFound("EMPLOYEE_NOT_FOUND", "Team member not found"));

        boolean losesManager = staff.getRole() == StaffRole.MANAGER && staff.isActive()
                && ((newRole != null && newRole != StaffRole.MANAGER) || Boolean.FALSE.equals(active));
        if (losesManager) requireAnotherManager(staff);

        StaffRole previous = staff.getRole();
        if (newRole != null) staff.setRole(newRole);
        if (active != null) staff.setActive(active);
        StoreStaff saved = storeStaffRepository.save(staff);

        if (newRole != null && newRole != previous) {
            notificationService.notify(
                    List.of(saved.getUserId()),
                    updatedBy,
                    NotificationType.HR,
                    "Nouveau rôle",
                    "Vous êtes maintenant %s de %s".formatted(roleLabelFr(newRole), saved.getStore().getStoreName()),
                    "people-outline",
                    java.util.Map.of("storeId", String.valueOf(saved.getStore().getId())));
        }
        return saved;
    }

    @Transactional
    @Override
    public void removeMember(Long staffId, Long removedBy) {
        StoreStaff staff = storeStaffRepository.findById(staffId)
                .orElseThrow(() -> ApiException.notFound("EMPLOYEE_NOT_FOUND", "Team member not found"));
        if (staff.getRole() == StaffRole.MANAGER && staff.isActive()) requireAnotherManager(staff);

        storeStaffRepository.delete(staff);
        log.info("Staff member userId={} removed from storeId={} by userId={}",
                staff.getUserId(), staff.getStore().getId(), removedBy);
    }

    /** 422 LAST_MANAGER when {@code staff} is the boutique's only active manager. */
    private void requireAnotherManager(StoreStaff staff) {
        long managers = storeStaffRepository.countByStore_IdAndRoleAndActiveTrue(staff.getStore().getId(), StaffRole.MANAGER);
        if (managers <= 1) {
            throw ApiException.unprocessable("LAST_MANAGER", "A shop needs at least one manager");
        }
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
