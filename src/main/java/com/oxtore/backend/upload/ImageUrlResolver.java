package com.oxtore.backend.upload;

import com.oxtore.backend.product.entity.Product;
import com.oxtore.backend.product.entity.ProductImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Turns stored image references (storage keys) into absolute URLs for API responses. */
@Component
@RequiredArgsConstructor
public class ImageUrlResolver {

    private final ImageStorageService storage;

    /** Storage keys become public URLs; anything else (legacy absolute URLs) passes through unchanged. */
    public String toPublicUrl(String stored) {
        if (stored == null || stored.isBlank()) return stored;
        return storage.keyFromUrl(stored)
                .filter(stored::equals)
                .map(storage::publicUrl)
                .orElse(stored);
    }

    /** Gallery URLs in display order (Product.images is @OrderBy displayOrder). */
    public List<String> urls(Product product) {
        return product.getImages().stream().map(ProductImage::getUrl).map(this::toPublicUrl).toList();
    }

    /** Main image URL, or null when the product has none. */
    public String firstUrl(Product product) {
        return product.getImages().isEmpty() ? null : toPublicUrl(product.getImages().get(0).getUrl());
    }
}
