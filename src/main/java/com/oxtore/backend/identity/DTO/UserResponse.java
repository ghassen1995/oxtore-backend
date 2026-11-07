package com.oxtore.backend.identity.DTO;

import java.util.List;

/**
 * publicId: the shareable 8-character id a boutique owner uses to add this user to their team.
 * role: the platform ADMIN, else the role held in the active boutique (MANAGER / SUPERVISOR / SELLER), else USER.
 * ownedBoutiqueIds: boutiques the user manages. memberships: the role in each boutique the user works at.
 */
public record UserResponse(
        Long id, String publicId, String fullName, String email, String phone, String avatar, String role,
        List<Long> ownedBoutiqueIds, Long activeBoutiqueId, List<String> permissions,
        boolean isVerified, String createdAt, List<Membership> memberships
) {
    public record Membership(Long boutiqueId, String role, String status) {}
}
