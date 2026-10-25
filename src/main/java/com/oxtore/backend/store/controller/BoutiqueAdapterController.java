package com.oxtore.backend.store.controller;

import com.oxtore.backend.common.security.StoreAccess;
import com.oxtore.backend.product.DTOs.CreateProductRequest;
import com.oxtore.backend.product.DTOs.WholesalePriceTierRequest;
import com.oxtore.backend.product.entity.Product;
import com.oxtore.backend.product.enums.*;
import com.oxtore.backend.product.mapper.AngularProductShapeMapper;
import com.oxtore.backend.product.service.ProductService;
import com.oxtore.backend.staff.entity.StoreStaff;
import com.oxtore.backend.staff.enums.StaffRole;
import com.oxtore.backend.staff.repository.StoreStaffRepository;
import com.oxtore.backend.stock.entity.Stock;
import com.oxtore.backend.stock.repository.StockRepository;
import com.oxtore.backend.stock.service.StockService;
import com.oxtore.backend.store.DTO.BoutiqueResponse;
import com.oxtore.backend.store.DTO.CreateBoutiqueRequest;
import com.oxtore.backend.store.entity.ResellerRelation;
import com.oxtore.backend.store.entity.Store;
import com.oxtore.backend.store.enums.BusinessSector;
import com.oxtore.backend.store.enums.ResellerRelationStatus;
import com.oxtore.backend.store.mapper.BoutiqueMapper;
import com.oxtore.backend.store.repository.ResellerRelationRepository;
import com.oxtore.backend.store.repository.StoreRepository;
import com.oxtore.backend.store.service.StoreCategories;
import com.oxtore.backend.store.service.StoreService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.oxtore.backend.upload.ImageReferenceInput;
import com.oxtore.backend.upload.ImageUrlResolver;
import com.oxtore.backend.upload.ProductImageInput;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/boutiques")
@RequiredArgsConstructor
public class BoutiqueAdapterController {

    private final StoreService storeService;
    private final StoreStaffRepository storeStaffRepository;
    private final BoutiqueMapper mapper;
    private final ResellerRelationRepository resellerRelationRepository;
    private final ProductService productService;
    private final AngularProductShapeMapper productShapeMapper;
    private final ProductImageInput productImageInput;
    private final ImageUrlResolver imageUrlResolver;
    private final ImageReferenceInput imageReferenceInput;
    private final StockService stockService;
    private final StockRepository stockRepository;
    private final StoreRepository storeRepository;
    private final StoreAccess storeAccess;
    private final StoreCategories storeCategories;
    private final com.oxtore.backend.stock.repository.StockActivityRepository stockActivityRepository;

    @PostMapping
    public ResponseEntity<BoutiqueResponse> create(@RequestBody CreateBoutiqueRequest req, HttpServletRequest http) {
        Long userId = Long.valueOf(http.getAttribute("userId").toString());
        List<String> categories = storeCategories.validate(req.categories());
        Store store = Store.builder()
                .storeName(req.name()).location(req.address()).phoneNumber(req.phone())
                .slogan(req.description())
                .categories(new java.util.ArrayList<>(categories))
                .businessSector(StoreCategories.sectorFor(categories))
                // URL returned by POST /api/uploads/images?folder=boutiques, stored as its storage key
                .logo(imageReferenceInput.optionalKey(req.logo(), "boutiques", "logo"))
                .build();
        Store saved = storeService.createStore(store, userId);
        return ResponseEntity.status(201).body(toFullResponse(saved));
    }

    @Transactional(readOnly = true)
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMine(HttpServletRequest http) {
        Long userId = Long.valueOf(http.getAttribute("userId").toString());
        List<StoreStaff> mine = storeStaffRepository.findAllByUserId(userId);
        List<BoutiqueResponse> data = mine.stream()
                .map(s -> toFullResponse(s.getStore())).toList();
        return ResponseEntity.ok(Map.of("data", data, "meta", Map.of()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoutiqueResponse> getById(@PathVariable("id") Long id) {
        return storeService.getStoreById(id)
                .map(this::toFullResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{boutiqueId}/products")
    public ResponseEntity<?> createProduct(@PathVariable("boutiqueId") Long boutiqueId,
                                           @RequestBody Map<String, Object> body, HttpServletRequest http) {
        storeAccess.require(StoreAccess.currentUserId(http), boutiqueId, StoreAccess.MANAGERS);

        CreateProductRequest req = new CreateProductRequest(
                mapCondition((String) body.get("condition")),
                (String) body.get("name"),
                UnitOfSale.PIECE,
                ProductStatus.PUBLISHED,
                boutiqueId,
                // Ordered URLs from POST /api/uploads/images, stored as storage keys (max 5, ours only).
                productImageInput.parse(body.get("images")),
                mapSaleType((String) body.get("saleType")),
                mapTransactionMode((String) body.get("transactionMode")),
                new BigDecimal(body.get("price").toString()),
                body.get("wholesalePrice") != null ? new BigDecimal(body.get("wholesalePrice").toString()) : null,
                (Integer) body.get("stock"),
                null,
                mapWholesaleTiers(body.get("wholesaleTiers"))
        );

        Product saved = productService.createProduct(req);
        // Same shape as the products list, so the Angular cache gets usable image URLs right away.
        return ResponseEntity.status(201).body(productShapeMapper.toShape(saved));
    }

    @SuppressWarnings("unchecked")
    private List<WholesalePriceTierRequest> mapWholesaleTiers(Object raw) {
        if (raw == null) return List.of();
        List<Map<String, Object>> tiers = (List<Map<String, Object>>) raw;
        return tiers.stream()
                .map(t -> new WholesalePriceTierRequest(
                        (Integer) t.get("minQty"),
                        new BigDecimal(t.get("unitPrice").toString())
                ))
                .toList();
    }

    private SaleType mapSaleType(String raw) {
        if (raw == null) return SaleType.RETAIL;
        return switch (raw.toLowerCase()) {
            case "wholesale" -> SaleType.WHOLESALE;
            case "retail" -> SaleType.RETAIL;
            case "both" -> SaleType.WHOLESALE; // no BOTH in your enum — flagged below
            default -> throw new IllegalArgumentException("Unknown saleType: " + raw);
        };
    }

    private TransactionMode mapTransactionMode(String raw) {
        if (raw == null) return TransactionMode.DIRECT_SALE;
        return switch (raw.toLowerCase()) {
            case "direct" -> TransactionMode.DIRECT_SALE;
            case "consignment" -> TransactionMode.CONSIGNMENT;
            case "commission" -> TransactionMode.CONSIGNMENT; // no COMMISSION mode in your enum — flagged below
            default -> throw new IllegalArgumentException("Unknown transactionMode: " + raw);
        };
    }

    private ProductCondition mapCondition(String raw) {
        if (raw == null) return ProductCondition.NEW;
        return ProductCondition.valueOf(raw.toUpperCase()); // "new"/"used" map cleanly, no mismatch here
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
                        .findByStore_IdAndSaleTypeAndTransactionMode(sid, SaleType.WHOLESALE, TransactionMode.CONSIGNMENT)
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
        map.put("wholesalePrice", stock.getMarketPrice() != null ? stock.getMarketPrice() : BigDecimal.ZERO);
        map.put("isActive", true);
        map.put("saleType", "wholesale");
        map.put("transactionMode", "consignment");
        map.put("condition", p.getCondition().name().toLowerCase());
        map.put("createdAt", p.getCreatedAt());
        map.put("ownerBoutique", ownerBoutique);
        map.put("stockItems", List.of(Map.of("quantity", stock.getQuantity(), "boutiqueId", store.getId())));
        return map;
    }

    @GetMapping("/discoverable")
    public ResponseEntity<Map<String, Object>> getDiscoverable(HttpServletRequest http) {
        Long userId = Long.valueOf(http.getAttribute("userId").toString());

        List<Long> myStoreIds = storeStaffRepository.findAllByUserId(userId).stream()
                .map(s -> s.getStore().getId())
                .toList();

        List<Store> discoverable = storeRepository.findAll().stream()
                .filter(s -> !myStoreIds.contains(s.getId()))
                .toList();

        List<BoutiqueResponse> data = discoverable.stream()
                .map(this::toFullResponse)
                .toList();

        return ResponseEntity.ok(Map.of("data", data, "meta", Map.of()));
    }

    private BoutiqueResponse toFullResponse(Store store) {
        BoutiqueResponse base = mapper.toResponse(store);
        List<String> ownerIds = storeStaffRepository.findAllByStoreId(store.getId()).stream()
                .filter(s -> s.getRole() == StaffRole.SUPERVISOR)
                .map(s -> s.getUserId().toString())
                .toList();

        // Card stats: items on offer, network members (boutiques linked by an accepted partnership, the app's
        // "network members" label, sent in employeeCount), turnover (total of recorded sales).
        int productCount = (int) stockRepository.countActiveByStoreId(store.getId());
        int networkMembers = (int) resellerRelationRepository.countNetworkMembers(store.getId());
        BigDecimal turnover = stockActivityRepository.sumSalesByStoreId(store.getId());

        return new BoutiqueResponse(
                base.id(), base.name(), imageUrlResolver.toPublicUrl(store.getLogo()), base.address(), base.phone(),
                base.description(), ownerIds, base.managerId(), base.managerName(),
                base.status(), base.rejectionReason(), base.language(), base.currency(),
                StoreCategories.of(store), networkMembers, productCount,
                turnover, base.createdAt()
        );
    }
}