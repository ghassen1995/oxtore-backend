package com.oxtore.backend.identity.service;

import com.oxtore.backend.identity.entity.User;
import com.oxtore.backend.staff.entity.StoreStaff;
import com.oxtore.backend.staff.enums.StaffRole;

import java.util.List;

/** Which boutique the app works in for a user (shared by the user response and the feed). */
public final class ActiveStores {

    private ActiveStores() {}

    /**
     * The stored choice wins while the user is still staff there; otherwise (never chosen, or removed from that
     * boutique) an owned boutique, else the first one they work at, else null. The stored value only changes when
     * the user picks a boutique (POST /api/users/me/active-boutique/{id}).
     */
    public static Long resolve(User user, List<StoreStaff> memberships) {
        List<Long> memberOf = memberships.stream().map(s -> s.getStore().getId()).sorted().toList();
        Long chosen = user.getActiveStoreId();
        if (chosen != null && memberOf.contains(chosen)) return chosen;
        Long owned = memberships.stream()
                .filter(s -> s.getRole() == StaffRole.SUPERVISOR)
                .map(s -> s.getStore().getId())
                .sorted()
                .findFirst()
                .orElse(null);
        if (owned != null) return owned;
        return memberOf.isEmpty() ? null : memberOf.get(0);
    }

    /** The user's role in {@code storeId}, or null when they don't work there. */
    public static StaffRole roleIn(List<StoreStaff> memberships, Long storeId) {
        if (storeId == null) return null;
        return memberships.stream()
                .filter(s -> s.getStore().getId().equals(storeId))
                .map(StoreStaff::getRole)
                .findFirst()
                .orElse(null);
    }
}
