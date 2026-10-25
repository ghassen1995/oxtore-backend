package com.oxtore.backend.identity.DTO;

import java.util.List;

/** publicId: the shareable 8-character id a boutique owner uses to add this user to their team. */
public record UserResponse(
        Long id, String publicId, String fullName, String email, String phone, String avatar, String role,
        List<Long> ownedBoutiqueIds, Long activeBoutiqueId, List<String> permissions,
        boolean isVerified, String createdAt
) {}