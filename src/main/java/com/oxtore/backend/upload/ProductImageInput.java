package com.oxtore.backend.upload;

import com.oxtore.backend.product.DTOs.ProductImageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates the "images" field of product create/update payloads: an ordered list of URLs previously returned by
 * POST /api/uploads/images (strings, or objects with a "url"). Produces storage keys with displayOrder = position.
 */
@Component
@RequiredArgsConstructor
public class ProductImageInput {

    public static final int MAX_IMAGES = 5;

    private final ImageStorageService storage;

    public List<ProductImageRequest> parse(Object raw) {
        if (raw == null) return List.of();
        if (!(raw instanceof List<?> items)) {
            throw new InvalidImageReferenceException("images must be a list of uploaded image URLs");
        }
        if (items.size() > MAX_IMAGES) {
            throw new InvalidImageReferenceException("A product can have at most " + MAX_IMAGES + " images");
        }
        List<ProductImageRequest> result = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            Object url = item instanceof Map<?, ?> map ? map.get("url") : item;
            String key = url instanceof String s ? storage.keyFromUrl(s).orElse(null) : null;
            if (key == null || !storage.exists(key)) {
                throw new InvalidImageReferenceException(
                        "Image " + (i + 1) + " was not uploaded through /api/uploads/images");
            }
            result.add(new ProductImageRequest(null, key, i));
        }
        return result;
    }
}
