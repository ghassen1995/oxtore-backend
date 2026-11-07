package com.oxtore.backend.store.controller;

import com.oxtore.backend.common.config.CurrencyCatalog;
import com.oxtore.backend.common.error.ApiException;
import com.oxtore.backend.common.security.StoreAccess;
import com.oxtore.backend.common.validation.InputRules;
import com.oxtore.backend.identity.enums.UserRole;
import com.oxtore.backend.identity.repository.IdentityRepository;
import com.oxtore.backend.product.DTOs.CreateProductRequest;
import com.oxtore.backend.product.DTOs.WholesalePriceTierRequest;
import com.oxtore.backend.product.entity.Product;
import com.oxtore.backend.product.enums.*;
import com.oxtore.backend.product.mapper.AngularProductShapeMapper;
import com.oxtore.backend.product.service.ProductInputRules;
import com.oxtore.backend.product.service.ProductService;
import com.oxtore.backend.staff.entity.StoreStaff;
import com.oxtore.backend.staff.enums.StaffRole;
import com.oxtore.backend.staff.repository.StoreStaffRepository;
import com.oxtore.backend.stock.entity.Stock;
import com.oxtore.backend.stock.repository.StockRepository;
import com.oxtore.backend.store.DTO.BoutiqueResponse;
import com.oxtore.backend.store.DTO.CreateBoutiqueRequest;
import com.oxtore.backend.store.entity.ResellerRelation;
import com.oxtore.backend.store.entity.Store;
import com.oxtore.backend.store.enums.ResellerRelationStatus;
import com.oxtore.backend.store.repository.ResellerRelationRepository;
import com.oxtore.backend.store.repository.StoreFollowRepository;
import com.oxtore.backend.store.repository.StoreRepository;
import com.oxtore.backend.store.service.StoreCategories;
import com.oxtore.backend.store.service.StoreService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.oxtore.backend.upload.ImageReferenceInput;
import com.oxtore.backend.upload.ImageUrlResolver;
import com.oxtore.backend.upload.ProductImageInput;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/boutiques")
@RequiredArgsConstructor
public class BoutiqueAdapterController {

    private static final Set<String> LANGUAGES = Set.of("fr", "en", "ar");

    private final StoreService storeService;
    private final StoreStaffRepository storeStaffRepository;
    private final ResellerRelationRepository resellerRelationRepository;
    private final ProductService productService;
    private final AngularProductShapeMapper productShapeMapper;
    private final ProductImageInput productImageInput;
    private final ImageUrlResolver imageUrlResolver;
    private final ImageReferenceInput imageReferenceInput;
    private final StockRepository stockRepository;
    private final StoreRepository storeRepository;
    private final StoreAccess storeAccess;
    private final StoreCategories storeCategories;
    private final StoreFollowRepository followRepository;
    private final IdentityRepository identityRepository;
    private final CurrencyCatalog currencyCatalog;
    private final com.oxtore.backend.stock.repository.StockActivityRepository stockActivityRepository;

    // ─── Boutiques ─────────────────────────────────────────────────────

    /** The creator becomes the boutique's MANAGER ("Gestionnaire"). */
    @PostMapping
    public ResponseEntity<BoutiqueResponse> create(@RequestBody CreateBoutiqueRequest req, HttpServletRequest http) {
        Long userId = StoreAccess.currentUserId(http);
        if (req == null) throw ApiException.validation("name", "Shop name is required");
        List<String> categories = categories(req.categories());
        Store store = Store.builder()
                .storeName(InputRules.shopName(req.name(), "name", true))
                .location(InputRules.text(req.address(), "address", "Address", 150, true))
                .phoneNumber(InputRules.phone(req.phone(), "phone", true))
                .slogan(InputRules.text(req.description(), "description", "Description", 150, false))
                .categories(new ArrayList<>(categories))
                .businessSector(StoreCategories.sectorFor(categories))
                .currency(currency(req.currency()))
                .language(language(req.language()))
                .showPhone(req.showPhone() == null || req.showPhone())
                // URL returned by POST /api/uploads/images?folder=boutiques, stored as its storage key
                .logo(imageReferenceInput.optionalKey(req.logo(), "boutiques", "logo"))
                .build();
        Store saved = storeService.createStore(store, userId);
        return ResponseEntity.status(201).body(toFullResponse(saved, viewer(userId)));
    }

    /**
     * Partial update by a MANAGER of the boutique (403 otherwise — never 401). Fields: name, address, phone,
     * description, logo (uploaded URL; null or "" removes it), categories, language, currency, showPhone.
     */
    @Transactional
    @PatchMapping("/{id}")
    public ResponseEntity<BoutiqueResponse> update(@PathVariable("id") Long id,
                                                   @RequestBody(required = false) Map<String, Object> body,
                                                   HttpServletRequest http) {
        Long userId = StoreAccess.currentUserId(http);
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("BOUTIQUE_NOT_FOUND", "Boutique not found"));
        storeAccess.require(userId, id, StoreAccess.MANAGERS);
        Map<String, Object> b = body == null ? Map.of() : body;

        if (b.containsKey("name")) store.setStoreName(InputRules.shopName(str(b.get("name")), "name", true));
        if (b.containsKey("address")) store.setLocation(InputRules.text(str(b.get("address")), "address", "Address", 150, true));
        if (b.containsKey("phone")) store.setPhoneNumber(InputRules.phone(str(b.get("phone")), "phone", true));
        if (b.containsKey("description")) {
            store.setSlogan(InputRules.text(str(b.get("description")), "description", "Description", 150, false));
        }
        if (b.containsKey("logo")) store.setLogo(imageReferenceInput.optionalKey(b.get("logo"), "boutiques", "logo"));
        if (b.containsKey("categories")) {
            if (!(b.get("categories") instanceof List<?> raw)) {
                throw ApiException.validation("categories", "categories must be a list");
            }
            List<String> categories = categories(raw.stream().map(String::valueOf).toList());
            store.setCategories(new ArrayList<>(categories));
            store.setBusinessSector(StoreCategories.sectorFor(categories));
        }
        if (b.containsKey("language")) store.setLanguage(language(str(b.get("language"))));
        if (b.containsKey("currency")) store.setCurrency(currency(str(b.get("currency"))));
        if (b.containsKey("showPhone")) {
            if (!(b.get("showPhone") instanceof Boolean show)) {
                throw ApiException.validation("showPhone", "showPhone must be true or false");
            }
            store.setShowPhone(show);
        }
        Store saved = storeRepository.save(store);
        return ResponseEntity.ok(toFullResponse(saved, viewer(userId)));
    }

    /** The caller's boutiques (any role). */
    @Transactional(readOnly = true)
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMine(HttpServletRequest http) {
        Long userId = StoreAccess.currentUserId(http);
        Viewer viewer = viewer(userId);
        List<BoutiqueResponse> data = storeStaffRepository.findAllByUserId(userId).stream()
                .filter(StoreStaff::isActive)
                .map(s -> toFullResponse(s.getStore(), viewer)).toList();
        return ResponseEntity.ok(Map.of("data", data, "meta", Map.of()));
    }

    @Transactional(readOnly = true)
    @GetMapping("/{id}")
    public ResponseEntity<BoutiqueResponse> getById(@PathVariable("id") Long id, HttpServletRequest http) {
        Viewer viewer = viewer(StoreAccess.currentUserId(http));
        return storeService.getStoreById(id)
                .map(s -> toFullResponse(s, viewer))
                .map(ResponseEntity::ok)
                .orElseThrow(() -> ApiException.notFound("BOUTIQUE_NOT_FOUND", "Boutique not found"));
    }

    @Transactional(readOnly = true)
    @GetMapping("/discoverable")
    public ResponseEntity<Map<String, Object>> getDiscoverable(HttpServletRequest http) {
        Long userId = StoreAccess.currentUserId(http);
        Viewer viewer = viewer(userId);
        List<Long> myStoreIds = storeStaffRepository.findAllByUserId(userId).stream()
                .map(s -> s.getStore().getId())
                .toList();

        List<BoutiqueResponse> data = storeRepository.findAll().stream()
                .filter(s -> !myStoreIds.contains(s.getId()))
                .map(s -> toFullResponse(s, viewer))
                .toList();

        return ResponseEntity.ok(Map.of("data", data, "meta", Map.of()));
    }

    // ─── Products ──────────────────────────────────────────────────────

    /**
     * Article creation by a MANAGER. {@code transactionMode} (direct | consignment) is mandatory; {@code saleType}
     * (retail | wholesale | both, default retail) is stored as sent. Wholesale / both need at least one tier with a
     * unit price above 0; their trade price is {@code wholesalePrice}, else the lowest tier price. Retail articles
     * keep no trade price or tiers.
     */
    @PostMapping("/{boutiqueId}/products")
    public ResponseEntity<?> createProduct(@PathVariable("boutiqueId") Long boutiqueId,
                                           @RequestBody Map<String, Object> body, HttpServletRequest http) {
        storeAccess.require(StoreAccess.currentUserId(http), boutiqueId, StoreAccess.MANAGERS);

        String name = InputRules.text(str(body.get("name")), "name", "Name", 100, true);
        SaleType saleType = ProductInputRules.parseSaleType(body.get("saleType"));
        TransactionMode transactionMode = ProductInputRules.parseTransactionMode(body.get("transactionMode"));
        BigDecimal price = money(body.get("price"), "price", true);
        int initialStock = body.get("stock") == null ? 0 : ProductInputRules.wholeNumber(body.get("stock"), "stock", 0);

        List<WholesalePriceTierRequest> tiers = List.of();
        BigDecimal tradePrice = null;
        if (saleType.isWholesale()) {
            tiers = parseWholesaleTiers(body.get("wholesaleTiers"));
            if (tiers.stream().noneMatch(t -> t.unitPrice().signum() > 0)) {
                throw ApiException.validation("wholesaleTiers",
                        "Wholesale articles need at least one price tier with a unit price above 0");
            }
            BigDecimal sent = money(body.get("wholesalePrice"), "wholesalePrice", false);
            tradePrice = sent != null && sent.signum() > 0 ? sent
                    : tiers.stream().map(WholesalePriceTierRequest::unitPrice).filter(p -> p.signum() > 0)
                    .min(Comparator.naturalOrder()).orElse(null);
        }

        CreateProductRequest req = new CreateProductRequest(
                mapCondition(body.get("condition")),
                name,
                UnitOfSale.PIECE,
                ProductStatus.PUBLISHED,
                boutiqueId,
                // Ordered URLs from POST /api/uploads/images, stored as storage keys (max 5, ours only).
                productImageInput.parse(body.get("images")),
                saleType,
                transactionMode,
                price,
                tradePrice,
                initialStock,
                null,
                tiers
        );

        Product saved = productService.createProduct(req);
        // Same shape as the products list, so the Angular cache gets usable image URLs right away.
        return ResponseEntity.status(201).body(productShapeMapper.toShape(saved));
    }

    @GetMapping("/{boutiqueId}/products")
    public ResponseEntity<Map<String, Object>> getProducts(@PathVariable("boutiqueId") Long boutiqueId) {
        List<Product> products = productService.findAllProductsByStoreId(boutiqueId);
        List<Map<String, Object>> data = products.stream()
                .filter(p -> p.getStatus() != ProductStatus.ARCHIVED)
                .map(productShapeMapper::toShape)
                .toList();
        return ResponseEntity.ok(Map.of("data", data, "meta", Map.of()));
    }

    @GetMapping("/{id}/network-products")
    public ResponseEntity<Map<String, Object>> getNetworkProducts(@PathVariable("id") Long id) {
        List<ResellerRelation> accepted = resellerRelationRepository
                .findByResellerStore_IdAndStatus(id, ResellerRelationStatus.ACCEPTED);

        List<Long> sourceStoreIds = accepted.stream()
                .map(r -> r.getSourceStore().getId())
                .toList();

        List<Stock> stocks = sourceStoreIds.stream()
                .flatMap(sid -> stockRepository
                        .findByStore_IdAndSaleTypeInAndTransactionMode(sid, SaleType.SOLD_WHOLESALE, TransactionMode.CONSIGNMENT)
                        .stream())
                .filter(s -> s.getProduct().getStatus() != ProductStatus.ARCHIVED)
                .toList();

        List<Map<String, Object>> data = stocks.stream().map(this::toNetworkProductShape).toList();
        return ResponseEntity.ok(Map.of("data", data, "meta", Map.of()));
    }

    private Map<String, Object> toNetworkProductShape(Stock stock) {
        Product p = stock.getProduct();
        Store store = stock.getStore();
        List<String> images = imageUrlResolver.urls(p);

        Map<String, Object> ownerBoutique = new LinkedHashMap<>();
        ownerBoutique.put("id", store.getId());
        ownerBoutique.put("name", store.getStoreName());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("name", p.getProductName());
        map.put("description", "");
        map.put("category", "all");
        map.put("images", images);
        map.put("ownerBoutiqueId", store.getId());
        map.put("cost", stock.getRetailPrice());
        map.put("price", stock.getRetailPrice());
        map.put("currency", CurrencyCatalog.orDefault(store.getCurrency()));
        map.put("wholesalePrice", stock.getMarketPrice() != null ? stock.getMarketPrice() : BigDecimal.ZERO);
        map.put("wholesaleTiers", AngularProductShapeMapper.tiers(stock));
        map.put("isActive", true);
        map.put("saleType", stock.getSaleType().apiValue());
        map.put("transactionMode", "consignment");
        map.put("condition", p.getCondition().name().toLowerCase());
        map.put("createdAt", p.getCreatedAt());
        map.put("ownerBoutique", ownerBoutique);
        map.put("stockItems", List.of(Map.of("quantity", stock.getQuantity(), "boutiqueId", store.getId())));
        return map;
    }

    // ─── Helpers ───────────────────────────────────────────────────────

    /** Who is reading: decides whether a hidden phone number is shown and whether the boutique is followed. */
    private record Viewer(Long userId, boolean platformAdmin, Set<Long> memberOf, Set<Long> following) {}

    private Viewer viewer(Long userId) {
        boolean admin = identityRepository.findById(userId).map(u -> u.getRole() == UserRole.ADMIN).orElse(false);
        Set<Long> memberOf = new HashSet<>(storeAccess.memberStoreIds(userId));
        Set<Long> following = new HashSet<>(followRepository.followedStoreIds(userId));
        return new Viewer(userId, admin, memberOf, following);
    }

    private BoutiqueResponse toFullResponse(Store store, Viewer viewer) {
        List<String> ownerIds = storeStaffRepository.findAllByStoreId(store.getId()).stream()
                .filter(s -> s.getRole() == StaffRole.MANAGER && s.isActive())
                .map(s -> s.getUserId().toString())
                .toList();

        // Card stats: items on offer, network members (boutiques linked by an accepted partnership, the app's
        // "network members" label, sent in employeeCount), turnover (total of recorded sales).
        int productCount = (int) stockRepository.countActiveByStoreId(store.getId());
        int networkMembers = (int) resellerRelationRepository.countNetworkMembers(store.getId());
        BigDecimal turnover = stockActivityRepository.sumSalesByStoreId(store.getId());

        // A hidden number is left out of the response itself, not only hidden by the app.
        boolean seesPhone = store.isShowPhone() || viewer.platformAdmin() || viewer.memberOf().contains(store.getId());

        return new BoutiqueResponse(
                store.getId(), store.getStoreName(), imageUrlResolver.toPublicUrl(store.getLogo()),
                store.getLocation(), seesPhone ? store.getPhoneNumber() : null,
                store.getSlogan(), ownerIds, null, null,
                "active", null, store.getLanguage() != null ? store.getLanguage() : "fr",
                CurrencyCatalog.orDefault(store.getCurrency()),
                StoreCategories.of(store), networkMembers, productCount,
                turnover, store.getCreatedAt(), store.isShowPhone(), viewer.following().contains(store.getId())
        );
    }

    private List<String> categories(List<String> requested) {
        try {
            return storeCategories.validate(requested);
        } catch (IllegalArgumentException e) {
            throw ApiException.validation("categories", String.valueOf(e.getMessage()));
        }
    }

    private String currency(String raw) {
        String v = InputRules.trimToNull(raw);
        if (v == null) return null;
        return currencyCatalog.find(v).map(CurrencyCatalog.Currency::code)
                .orElseThrow(() -> ApiException.validation("currency", "Unsupported currency: " + v));
    }

    private static String language(String raw) {
        String v = InputRules.trimToNull(raw);
        if (v == null) return null;
        String lang = v.toLowerCase(Locale.ROOT);
        if (!LANGUAGES.contains(lang)) throw ApiException.validation("language", "Language must be fr, en or ar");
        return lang;
    }

    private static String str(Object raw) {
        return raw == null ? null : String.valueOf(raw);
    }

    @SuppressWarnings("unchecked")
    private static List<WholesalePriceTierRequest> parseWholesaleTiers(Object raw) {
        if (raw == null) return List.of();
        if (!(raw instanceof List<?> list)) throw ApiException.validation("wholesaleTiers", "wholesaleTiers must be a list");
        List<WholesalePriceTierRequest> tiers = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (!(list.get(i) instanceof Map<?, ?> t)) {
                throw ApiException.validation("wholesaleTiers[" + i + "]", "Invalid price tier");
            }
            int minQty = ProductInputRules.wholeNumber(t.get("minQty"), "wholesaleTiers[" + i + "].minQty", 1);
            BigDecimal unitPrice = money(t.get("unitPrice"), "wholesaleTiers[" + i + "].unitPrice", true);
            tiers.add(new WholesalePriceTierRequest(minQty, unitPrice));
        }
        return tiers;
    }

    private static ProductCondition mapCondition(Object raw) {
        if (raw == null) return ProductCondition.NEW;
        return switch (String.valueOf(raw).trim().toLowerCase(Locale.ROOT)) {
            case "new" -> ProductCondition.NEW;
            case "used" -> ProductCondition.USED;
            default -> throw ApiException.validation("condition", "condition must be new or used");
        };
    }

    private static BigDecimal money(Object raw, String field, boolean required) {
        return ProductInputRules.money(raw, field, required);
    }
}
