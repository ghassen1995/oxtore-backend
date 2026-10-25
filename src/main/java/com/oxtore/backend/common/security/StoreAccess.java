package com.oxtore.backend.common.security;

import com.oxtore.backend.staff.entity.StoreStaff;
import com.oxtore.backend.staff.enums.StaffRole;
import com.oxtore.backend.staff.repository.StoreStaffRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Per-boutique permission checks for controllers whose boutique id isn't a simple path variable or body field
 * (derived from an entity, a list of ids, either side of a relation), where @RequireStoreRole can't express it.
 * Failures throw AccessDeniedException, answered as 403.
 */
@Component
public class StoreAccess {

    /** Boutique management: products, HR, network, stock requests. Sellers only sell. */
    public static final Set<StaffRole> MANAGERS = EnumSet.of(StaffRole.SUPERVISOR, StaffRole.MANAGER);

    private final StoreStaffRepository storeStaffRepository;

    public StoreAccess(StoreStaffRepository storeStaffRepository) {
        this.storeStaffRepository = storeStaffRepository;
    }

    public static Long currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) throw new AccessDeniedException("No authenticated user found on request");
        return Long.valueOf(userId.toString());
    }

    /** The user must hold one of {@code roles} in {@code storeId}. */
    public void require(Long userId, Long storeId, Set<StaffRole> roles) {
        StaffRole role = roleIn(userId, storeId);
        if (role == null || !roles.contains(role)) {
            throw new AccessDeniedException("You don't have permission to do this in this boutique");
        }
    }

    /** The user must hold one of {@code roles} in at least one of the given boutiques (e.g. either side of a relation). */
    public void requireAny(Long userId, Collection<Long> storeIds, Set<StaffRole> roles) {
        boolean allowed = storeIds.stream().anyMatch(id -> {
            StaffRole role = roleIn(userId, id);
            return role != null && roles.contains(role);
        });
        if (!allowed) throw new AccessDeniedException("You don't have permission to do this in this boutique");
    }

    /** Read access: the user must work (any role) at every listed boutique. */
    public void requireMemberOfAll(Long userId, Collection<Long> storeIds) {
        for (Long id : storeIds) {
            if (roleIn(userId, id) == null) {
                throw new AccessDeniedException("You don't have access to this boutique");
            }
        }
    }

    /** Boutiques the user works at, any role. */
    public List<Long> memberStoreIds(Long userId) {
        return storeStaffRepository.findAllByUserId(userId).stream().map(s -> s.getStore().getId()).toList();
    }

    private StaffRole roleIn(Long userId, Long storeId) {
        return storeStaffRepository.findByStore_IdAndUserId(storeId, userId).map(StoreStaff::getRole).orElse(null);
    }
}
