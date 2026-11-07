package com.oxtore.backend.identity.mapper;

import com.oxtore.backend.identity.DTO.UserResponse;
import com.oxtore.backend.identity.entity.User;
import com.oxtore.backend.identity.enums.UserRole;
import com.oxtore.backend.identity.service.ActiveStores;
import com.oxtore.backend.staff.entity.StoreStaff;
import com.oxtore.backend.staff.enums.StaffRole;
import com.oxtore.backend.staff.repository.StoreStaffRepository;
import com.oxtore.backend.upload.ImageUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserResponseMapper {

    private final StoreStaffRepository storeStaffRepository;
    private final ImageUrlResolver imageUrlResolver;

    public UserResponse toResponse(User user) {
        List<StoreStaff> all = storeStaffRepository.findAllByUserId(user.getId());
        // Deactivated memberships give no access: the app shouldn't open those boutiques.
        List<StoreStaff> staffRoles = all.stream().filter(StoreStaff::isActive).toList();

        List<Long> ownedIds = staffRoles.stream()
                .filter(s -> s.getRole() == StaffRole.MANAGER)
                .map(s -> s.getStore().getId())
                .toList();

        // Staff members who own nothing land in their workplace instead of having no boutique at all.
        Long activeStoreId = ActiveStores.resolve(user, staffRoles);

        // The role the app works with is the one held in the active boutique (switching boutique switches role):
        // someone who manages a boutique but sells in another one is a SELLER while that other boutique is active.
        // The platform administrator stays ADMIN everywhere.
        StaffRole activeRole = ActiveStores.roleIn(staffRoles, activeStoreId);
        String role = user.getRole() == UserRole.ADMIN ? "ADMIN"
                : activeRole != null ? activeRole.name()
                : "USER";

        List<UserResponse.Membership> memberships = all.stream()
                .sorted(Comparator.comparing(s -> s.getStore().getId()))
                .map(s -> new UserResponse.Membership(s.getStore().getId(), s.getRole().name(),
                        s.isActive() ? "active" : "inactive"))
                .toList();

        String fullName = (user.getFirstName() + " " + (user.getLastName() == null ? "" : user.getLastName())).trim();

        return new UserResponse(
                user.getId(),
                user.getPublicId(),
                fullName,
                user.getEmail(),
                user.getPhoneNumber(),
                imageUrlResolver.toPublicUrl(user.getAvatar()),
                role,
                ownedIds,
                activeStoreId,
                List.of(), // no permissions concept yet
                user.isEmailVerified(),
                (user.getCreatedAt() != null ? user.getCreatedAt() : Instant.now()).toString(),
                memberships
        );
    }
}
