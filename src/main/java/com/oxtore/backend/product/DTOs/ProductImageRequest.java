package com.oxtore.backend.product.DTOs;

public record ProductImageRequest(
        Long id,
        String url,
        Integer displayOrder
) {
}
