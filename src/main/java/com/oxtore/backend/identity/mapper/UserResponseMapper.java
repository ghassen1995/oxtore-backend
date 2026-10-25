package com.oxtore.backend.identity.mapper;

import com.oxtore.backend.identity.DTO.UserResponse;
import com.oxtore.backend.identity.entity.User;
import com.oxtore.backend.identity.service.ActiveStores;
import com.oxtore.backend.staff.entity.StoreStaff;
import com.oxtore.backend.staff.enums.StaffRole;
import com.oxtore.backend.staff.repository.StoreStaffRepository;
import com.oxtore.backend.upload.ImageUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserResponseMapper {

    private final StoreStaffRepository storeStaffRepository;
    private final ImageUrlResolver imageUrlResolver;

    public UserResponse toResponse(User user) {
        List<StoreStaff> staffRoles = storeStaffRepository.findAllByUserId(user.getId());

        List<Long> ownedIds = staffRoles.stream()
                .filter(s -> s.getRole() == StaffRole.SUPERVISOR)
                .map(s -> s.getStore().getId())
                .toList();

        // Staff members who own nothing land in their workplace instead of having no boutique at all.
        Long activeStoreId = ActiveStores.resolve(user, staffRoles);

        // The role the app works with is the one held in the active boutique (switching boutique switches role):
        // someone who owns a boutique but sells in another one is a SELLER while that other boutique is active.
        String role = staffRoles.stream()
                .filter(s -> s.getStore().getId().equals(activeStoreId))
                .findFirst()
                .map(s -> switch (s.getRole()) {
                    case SUPERVISOR -> "ADMIN";
                    case MANAGER -> "MANAGER";
                    case SELLER -> "SELLER";
                })
                .orElse("USER");

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
                (user.getCreatedAt() != null ? user.getCreatedAt() : Instant.now()).toString()
        );
    }
}
