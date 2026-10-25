package com.oxtore.backend.product.mapper;

import com.oxtore.backend.product.DTOs.ProductImageResponse;
import com.oxtore.backend.product.entity.ProductImage;
import com.oxtore.backend.upload.ImageUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Used by ProductMapper (MapStruct "uses") so legacy ProductResponse images carry public URLs, not storage keys. */
@Component
@RequiredArgsConstructor
public class ProductImageUrlMapping {

    private final ImageUrlResolver imageUrlResolver;

    public ProductImageResponse toResponse(ProductImage image) {
        if (image == null) return null;
        return new ProductImageResponse(image.getId(), imageUrlResolver.toPublicUrl(image.getUrl()), image.getDisplayOrder());
    }
}
