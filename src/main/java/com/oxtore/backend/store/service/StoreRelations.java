package com.oxtore.backend.store.service;

import com.oxtore.backend.identity.entity.User;
import com.oxtore.backend.identity.repository.IdentityRepository;
import com.oxtore.backend.identity.service.ActiveStores;
import com.oxtore.backend.product.enums.TransactionMode;
import com.oxtore.backend.staff.entity.StoreStaff;
import com.oxtore.backend.staff.enums.StaffRole;
import com.oxtore.backend.staff.repository.StoreStaffRepository;
import com.oxtore.backend.stock.entity.Stock;
import com.oxtore.backend.store.entity.ResellerRelation;
import com.oxtore.backend.store.repository.ResellerRelationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * How other boutiques relate to the user's active boutique, and what that allows. Shared by the feed, the
 * marketplace ({@code relation}, {@code canOrder}) and checkout, which enforces the consignment rule: consignment
 * stock may only be ordered by an approved reseller of the selling boutique (accepted relation where the buyer's
 * active boutique is the reseller and the seller is the source).
 */
@Component
@RequiredArgsConstructor
public class StoreRelations {

    /** Relation of a boutique to the active boutique, strongest first. */
    public static final String OWN = "own", SOURCE = "source", PARTNER = "partner", REQUESTED = "requested", NONE = "none";
    private static final List<String> STRENGTH = List.of(OWN, SOURCE, PARTNER, REQUESTED, NONE);

    private final IdentityRepository userRepository;
    private final StoreStaffRepository storeStaffRepository;
    private final ResellerRelationRepository relationRepository;

    /** The user's point of view: active boutique, role there, relations of other boutiques to it. */
    public record View(Long activeStoreId, StaffRole activeRole, Map<Long, String> relationByStore) {

        public String relationTo(Long storeId) {
            if (storeId != null && storeId.equals(activeStoreId)) return OWN;
            return relationByStore.getOrDefault(storeId, NONE);
        }

        /** Boutiques that supply the active boutique (it is their accepted reseller). */
        public Set<Long> sourceIds() {
            return relationByStore.entrySet().stream()
                    .filter(e -> SOURCE.equals(e.getValue())).map(Map.Entry::getKey).collect(Collectors.toSet());
        }

        /** Direct-sale offers: anyone. Consignment offers: approved resellers of the offering boutique only. */
        public boolean canOrder(Stock stock) {
            if (stock.getTransactionMode() != TransactionMode.CONSIGNMENT) return true;
            return SOURCE.equals(relationTo(stock.getStore().getId()));
        }
    }

    @Transactional(readOnly = true)
    public View forUser(Long userId) {
        User user = userId == null ? null : userRepository.findById(userId).orElse(null);
        if (user == null) return new View(null, null, Map.of());
        List<StoreStaff> memberships = storeStaffRepository.findAllByUserId(userId).stream()
                .filter(StoreStaff::isActive).toList();
        Long activeStoreId = ActiveStores.resolve(user, memberships);
        return new View(activeStoreId, ActiveStores.roleIn(memberships, activeStoreId), relationsOf(activeStoreId));
    }

    /** Other boutique id → relation with {@code storeId} (accepted or pending relations only). */
    private Map<Long, String> relationsOf(Long storeId) {
        Map<Long, String> byStore = new HashMap<>();
        if (storeId == null) return byStore;
        for (ResellerRelation r : relationRepository.findBySourceStore_IdInOrResellerStore_IdIn(List.of(storeId), List.of(storeId))) {
            boolean iAmReseller = r.getResellerStore().getId().equals(storeId);
            Long other = iAmReseller ? r.getSourceStore().getId() : r.getResellerStore().getId();
            String relation = switch (r.getStatus()) {
                case ACCEPTED -> iAmReseller ? SOURCE : PARTNER;
                case PENDING -> REQUESTED;
                default -> null; // declined / revoked: a new request is allowed
            };
            if (relation != null) {
                byStore.merge(other, relation, (a, b) -> STRENGTH.indexOf(a) <= STRENGTH.indexOf(b) ? a : b);
            }
        }
        return byStore;
    }
}
