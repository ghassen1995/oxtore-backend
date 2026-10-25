package com.oxtore.backend.feed;

import com.oxtore.backend.common.security.StoreAccess;
import com.oxtore.backend.identity.entity.User;
import com.oxtore.backend.identity.repository.IdentityRepository;
import com.oxtore.backend.identity.service.ActiveStores;
import com.oxtore.backend.product.entity.Product;
import com.oxtore.backend.product.enums.ProductStatus;
import com.oxtore.backend.product.enums.SaleType;
import com.oxtore.backend.product.enums.TransactionMode;
import com.oxtore.backend.product.repository.ProductRepository;
import com.oxtore.backend.staff.entity.StoreStaff;
import com.oxtore.backend.staff.enums.StaffRole;
import com.oxtore.backend.staff.repository.StoreStaffRepository;
import com.oxtore.backend.stock.entity.Stock;
import com.oxtore.backend.stock.repository.StockRepository;
import com.oxtore.backend.store.entity.ResellerRelation;
import com.oxtore.backend.store.entity.Store;
import com.oxtore.backend.store.repository.ResellerRelationRepository;
import com.oxtore.backend.upload.ImageUrlResolver;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The feed ("threads") seen by the current user, from the point of view of their active boutique:
 * <ul>
 *   <li>every public (retail) offer, from any boutique;</li>
 *   <li>every offer of the boutiques that supply the active boutique (its "sources": accepted relation where the
 *       active boutique is the reseller), including their wholesale / consignment offers, which are flagged
 *       {@code special} so the app can frame them.</li>
 * </ul>
 * Non-partner boutiques therefore only show their public offers; each item says how its boutique relates to the
 * active boutique and whether the user may send it a partnership request (supervisor or manager only).
 * One item per offer (stock row): the same product sold by two boutiques is two threads; likes are per product.
 */
@Service
@RequiredArgsConstructor
public class FeedService {

    /** Relation of an item's boutique to the active boutique, strongest first. */
    static final String OWN = "own", SOURCE = "source", PARTNER = "partner", REQUESTED = "requested", NONE = "none";
    private static final List<String> STRENGTH = List.of(OWN, SOURCE, PARTNER, REQUESTED, NONE);

    private final IdentityRepository userRepository;
    private final StoreStaffRepository storeStaffRepository;
    private final ResellerRelationRepository relationRepository;
    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final ProductLikeRepository likeRepository;
    private final ImageUrlResolver imageUrlResolver;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> feed(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        List<StoreStaff> memberships = storeStaffRepository.findAllByUserId(userId);
        Long myStoreId = ActiveStores.resolve(user, memberships);
        StaffRole myRole = ActiveStores.roleIn(memberships, myStoreId);
        boolean mayRequest = myRole != null && StoreAccess.MANAGERS.contains(myRole);

        Map<Long, String> relationByStore = relationsOf(myStoreId);
        Set<Long> sourceIds = relationByStore.entrySet().stream()
                .filter(e -> SOURCE.equals(e.getValue())).map(Map.Entry::getKey).collect(Collectors.toSet());

        // Public offers + everything the sources offer; a source's retail stock appears in both lists, hence the map.
        Map<Long, Stock> offers = new LinkedHashMap<>();
        stockRepository.findBySaleType(SaleType.RETAIL).forEach(s -> offers.put(s.getId(), s));
        sourceIds.forEach(id -> stockRepository.findByStore_Id(id).forEach(s -> offers.putIfAbsent(s.getId(), s)));

        List<Stock> visible = offers.values().stream()
                .filter(s -> s.getProduct().getStatus() != ProductStatus.ARCHIVED)
                .sorted(Comparator.comparing(Stock::getCreatedAt, Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())))
                .toList();

        Set<Long> productIds = visible.stream().map(s -> s.getProduct().getId()).collect(Collectors.toSet());
        Map<Long, Long> likeCounts = new HashMap<>();
        Set<Long> liked = new HashSet<>();
        if (!productIds.isEmpty()) {
            likeRepository.countByProductIds(productIds).forEach(row -> likeCounts.put((Long) row[0], (Long) row[1]));
            liked.addAll(likeRepository.likedAmong(userId, productIds));
        }

        return visible.stream().map(stock -> {
            Long storeId = stock.getStore().getId();
            String relation = storeId.equals(myStoreId) ? OWN : relationByStore.getOrDefault(storeId, NONE);
            boolean special = SOURCE.equals(relation) && isTradeOffer(stock);
            return toItem(stock, relation, special, mayRequest && NONE.equals(relation),
                    likeCounts.getOrDefault(stock.getProduct().getId(), 0L), liked.contains(stock.getProduct().getId()));
        }).toList();
    }

    /** Likes or unlikes a product; empty when the product doesn't exist or is archived. */
    @Transactional
    public Optional<Map<String, Object>> toggleLike(Long productId, Long userId) {
        Optional<Product> product = productRepository.findById(productId)
                .filter(p -> p.getStatus() != ProductStatus.ARCHIVED);
        if (product.isEmpty()) return Optional.empty();

        boolean liked;
        if (likeRepository.existsByProductIdAndUserId(productId, userId)) {
            likeRepository.deleteLike(productId, userId);
            liked = false;
        } else {
            likeRepository.save(new ProductLike(productId, userId));
            liked = true;
        }
        // int: JacksonConfig serializes every long as a JSON string, the app expects a number.
        return Optional.of(Map.of("liked", liked, "likes", (int) likeRepository.countByProductId(productId)));
    }

    /** Wholesale or consignment: an offer for resellers rather than for the public. */
    private static boolean isTradeOffer(Stock stock) {
        return stock.getSaleType() == SaleType.WHOLESALE || stock.getTransactionMode() == TransactionMode.CONSIGNMENT;
    }

    /** Other boutique id → relation with the active boutique (accepted or pending relations only). */
    private Map<Long, String> relationsOf(Long myStoreId) {
        Map<Long, String> byStore = new HashMap<>();
        if (myStoreId == null) return byStore;
        for (ResellerRelation r : relationRepository.findBySourceStore_IdInOrResellerStore_IdIn(List.of(myStoreId), List.of(myStoreId))) {
            boolean iAmReseller = r.getResellerStore().getId().equals(myStoreId);
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

    private Map<String, Object> toItem(Stock stock, String relation, boolean special, boolean canRequestPartnership,
                                       long likes, boolean liked) {
        Product p = stock.getProduct();
        Store store = stock.getStore();

        Map<String, Object> owner = new LinkedHashMap<>();
        owner.put("id", store.getId());
        owner.put("name", store.getStoreName());
        owner.put("logo", imageUrlResolver.toPublicUrl(store.getLogo()));

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", p.getId());
        item.put("stockId", stock.getId());
        item.put("name", p.getProductName());
        item.put("price", stock.getRetailPrice());
        item.put("wholesalePrice", special ? stock.getMarketPrice() : null);
        item.put("images", imageUrlResolver.urls(p));
        item.put("category", "all");
        item.put("condition", p.getCondition().name());
        item.put("saleType", stock.getSaleType() == SaleType.WHOLESALE ? "wholesale" : "retail");
        item.put("transactionMode", stock.getTransactionMode() == TransactionMode.CONSIGNMENT ? "consignment" : "direct");
        item.put("special", special);
        item.put("relation", relation);
        item.put("canRequestPartnership", canRequestPartnership);
        item.put("ownerBoutiqueId", store.getId());
        item.put("ownerBoutique", owner);
        item.put("likes", (int) likes); // int, not long: longs are serialized as strings
        item.put("liked", liked);
        return item;
    }
}
