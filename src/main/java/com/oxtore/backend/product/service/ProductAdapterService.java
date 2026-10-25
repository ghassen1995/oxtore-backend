package com.oxtore.backend.product.service;

import com.oxtore.backend.product.DTOs.ProductImageRequest;
import com.oxtore.backend.product.entity.Product;
import com.oxtore.backend.product.entity.ProductImage;
import com.oxtore.backend.product.enums.ProductStatus;
import com.oxtore.backend.product.repository.ProductRepository;
import com.oxtore.backend.staff.entity.StoreStaff;
import com.oxtore.backend.staff.enums.StaffRole;
import com.oxtore.backend.staff.repository.StoreStaffRepository;
import com.oxtore.backend.stock.entity.Stock;
import com.oxtore.backend.stock.repository.StockRepository;
import com.oxtore.backend.upload.ImageStorageService;
import com.oxtore.backend.upload.ProductImageInput;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Update / soft delete for /api/products/{id}, restricted to the SUPERVISOR or MANAGER of the product's store. */
@Service
@RequiredArgsConstructor
public class ProductAdapterService {

    private static final Set<StaffRole> EDITOR_ROLES = Set.of(StaffRole.SUPERVISOR, StaffRole.MANAGER);

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final StoreStaffRepository storeStaffRepository;
    private final ProductImageInput productImageInput;
    private final ImageStorageService imageStorage;

    /**
     * Partial update. Supported fields: name, isActive, price (own-store retail price) and images (replaces the
     * whole gallery). Files of images dropped from the gallery are deleted only after the transaction commits.
     */
    @Transactional
    public Product update(Long productId, Map<String, Object> body, Long userId) {
        Product product = loadEditable(productId, userId);

        if (body.get("name") instanceof String name) {
            if (name.isBlank() || name.length() > 100) {
                throw new IllegalArgumentException("name must be 1 to 100 characters");
            }
            product.setProductName(name.trim());
        }
        if (body.get("isActive") instanceof Boolean active) {
            product.setStatus(active ? ProductStatus.PUBLISHED : ProductStatus.DRAFT);
        }
        if (body.containsKey("price") && body.get("price") != null) {
            BigDecimal price = new BigDecimal(body.get("price").toString());
            if (price.signum() < 0) {
                throw new IllegalArgumentException("price must not be negative");
            }
            Stock ownStock = stockRepository.findByProduct_IdAndStore_Id(product.getId(), product.getStoreId())
                    .orElseThrow(() -> new EntityNotFoundException("Stock not found for product " + productId));
            ownStock.setRetailPrice(price);
            stockRepository.save(ownStock);
        }
        if (body.containsKey("images")) {
            replaceImages(product, productImageInput.parse(body.get("images")));
        }
        return productRepository.save(product);
    }

    /** Soft delete: the product is archived and hidden from listings; its image files are kept. */
    @Transactional
    public void archive(Long productId, Long userId) {
        Product product = loadEditable(productId, userId);
        product.setStatus(ProductStatus.ARCHIVED);
        productRepository.save(product);
    }

    private void replaceImages(Product product, List<ProductImageRequest> requested) {
        Set<String> keep = new HashSet<>();
        requested.forEach(r -> keep.add(r.url()));
        List<String> removed = product.getImages().stream()
                .map(ProductImage::getUrl)
                .filter(url -> !keep.contains(url))
                .toList();

        product.getImages().clear();
        requested.forEach(r -> product.getImages().add(ProductImage.builder()
                .product(product)
                .url(r.url())
                .displayOrder(r.displayOrder())
                .build()));

        if (!removed.isEmpty()) {
            deleteFilesAfterCommit(removed);
        }
    }

    /** Never delete a file the database might still reference: wait for the commit, then best effort. */
    private void deleteFilesAfterCommit(List<String> storedUrls) {
        Runnable cleanup = () -> storedUrls.forEach(stored ->
                imageStorage.keyFromUrl(stored).ifPresent(imageStorage::delete));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cleanup.run();
                }
            });
        } else {
            cleanup.run();
        }
    }

    private Product loadEditable(Long productId, Long userId) {
        Product product = productRepository.findById(productId)
                .filter(p -> p.getStatus() != ProductStatus.ARCHIVED)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
        StoreStaff staff = storeStaffRepository.findByStore_IdAndUserId(product.getStoreId(), userId)
                .orElseThrow(() -> new AccessDeniedException(
                        "User %d is not staff of store %d".formatted(userId, product.getStoreId())));
        if (!EDITOR_ROLES.contains(staff.getRole())) {
            throw new AccessDeniedException("Only the supervisor or manager can modify this product");
        }
        return product;
    }
}
